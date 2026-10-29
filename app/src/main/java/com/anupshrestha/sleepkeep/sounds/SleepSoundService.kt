package com.anupshrestha.sleepkeep.sounds

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RawRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.anupshrestha.sleepkeep.MainActivity
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.SleepApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Plays a generated [SleepSound] with the screen off, as a foreground service of type
 * "mediaPlayback". Stops by itself when the timer's fade-out ends, or when another app takes
 * the audio (a call, music).
 */
class SleepSoundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playJob: Job? = null
    private var focusRequest: AudioFocusRequest? = null

    private val controller get() = (application as SleepApp).container.sounds
    private val audioManager get() = getSystemService(AudioManager::class.java)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val sound = intent.getStringExtra(EXTRA_SOUND)?.let { runCatching { SleepSound.valueOf(it) }.getOrNull() }
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 0).takeIf { it > 0 }
                if (sound == null) stopSelf() else play(sound, minutes)
            }
            ACTION_STOP -> scope.launch { finish("stop requested") }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun play(sound: SleepSound, minutes: Int?) {
        val clock = (application as SleepApp).container.clock
        val start = clock.millis()
        val duration = minutes?.let { it * 60_000L }
        val endsAt = duration?.let { start + it }
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification(sound, endsAt),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
        } catch (e: RuntimeException) {
            stopSelf()
            return
        }
        if (!requestFocus()) {
            scope.launch { finish("no audio focus") }
            return
        }
        controller.mutableStatus.value = SoundStatus.Playing(sound, endsAt)

        // Switching sounds: stop the old stream, then start the new one.
        val previous = playJob
        playJob = scope.launch {
            previous?.cancelAndJoin()
            stream(sound, start, duration)
            // The timer ran out (not a stop request, which cancels this job first). Finish from a
            // separate coroutine: finish() cancels and waits for this job, which can't wait for itself.
            if (currentCoroutineContext().isActive) scope.launch { finish("stream ended") }
        }
    }

    /**
     * Streams until the timer ends or the job is cancelled. If the audio output fails (e.g. the
     * audio server restarts or the route changes), it's rebuilt instead of stopping the sound;
     * only repeated failures end playback.
     */
    private suspend fun stream(sound: SleepSound, start: Long, duration: Long?) {
        val clock = (application as SleepApp).container.clock
        val gain = { NoiseGenerator.gainAt(clock.millis() - start, duration) }
        val keepGoing = { duration == null || clock.millis() - start < duration }

        // Rain, storm and ocean are real recordings. If one can't be played on some phone, the
        // generated version plays instead of nothing.
        recordingFor(sound)?.let { resId ->
            try {
                RecordingPlayer(this, resId).play(gain, keepGoing)
                return
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't play the $sound recording, playing the generated version", e)
            }
        }

        val generator = NoiseGenerator(sound, SAMPLE_RATE, seed = start)
        val chunk = ShortArray(SAMPLE_RATE / 10)
        var failures = 0
        while (currentCoroutineContext().isActive && failures < MAX_TRACK_FAILURES) {
            val track = buildTrack(SAMPLE_RATE, chunk.size)
            try {
                track.play()
                while (currentCoroutineContext().isActive) {
                    if (!keepGoing()) return
                    generator.fill(chunk, gain())
                    // Blocks until the buffer has room, which paces the loop to real time.
                    val written = track.write(chunk, 0, chunk.size)
                    if (written < 0) {
                        failures++
                        Log.w(TAG, "AudioTrack write failed ($written), rebuilding (failure $failures)")
                        break
                    }
                }
            } finally {
                track.stop()
                track.release()
            }
        }
    }

    @RawRes
    private fun recordingFor(sound: SleepSound): Int? = when (sound) {
        SleepSound.RAIN -> R.raw.sound_rain
        SleepSound.STORM -> R.raw.sound_storm
        SleepSound.OCEAN -> R.raw.sound_ocean
        else -> null
    }

    private fun buildTrack(rate: Int, chunkSamples: Int): AudioTrack {
        val minBytes = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBytes, chunkSamples * 2 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun requestFocus(): Boolean {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            // A call or other audio takes over: stop rather than play over it.
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    scope.launch { finish("audio focus lost: $change") }
                }
            }
            .build()
        focusRequest = request
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private suspend fun finish(reason: String) {
        Log.i(TAG, "Stopping sleep sound: $reason")
        playJob?.cancelAndJoin()
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        controller.mutableStatus.value = SoundStatus.Off
        withContext(Dispatchers.Main) {
            ServiceCompat.stopForeground(this@SleepSoundService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun notification(sound: SleepSound, endsAt: Long?): Notification {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.sounds_channel))
                .setShowBadge(false)
                .build()
        )
        val openApp = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(this, 3, stopIntent(this), PendingIntent.FLAG_IMMUTABLE)
        val text = endsAt?.let {
            getString(R.string.sounds_stops_at, clockFormat.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())))
        } ?: getString(R.string.sounds_no_timer)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.sounds_playing, getString(labelOf(sound))))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.sounds_stop), stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        controller.mutableStatus.value = SoundStatus.Off
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.anupshrestha.sleepkeep.action.PLAY_SOUND"
        const val ACTION_STOP = "com.anupshrestha.sleepkeep.action.STOP_SOUND"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_MINUTES = "minutes"
        private const val TAG = "SleepSound"
        private const val MAX_TRACK_FAILURES = 5
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "sleep_sounds"
        private const val SAMPLE_RATE = 22_050
        private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")

        fun stopIntent(context: Context): Intent =
            Intent(context, SleepSoundService::class.java).setAction(ACTION_STOP)

        fun labelOf(sound: SleepSound): Int = when (sound) {
            SleepSound.RAIN -> R.string.sound_rain
            SleepSound.STORM -> R.string.sound_storm
            SleepSound.OCEAN -> R.string.sound_ocean
            SleepSound.WHITE -> R.string.sound_white
            SleepSound.PINK -> R.string.sound_pink
            SleepSound.BROWN -> R.string.sound_brown
        }
    }
}
