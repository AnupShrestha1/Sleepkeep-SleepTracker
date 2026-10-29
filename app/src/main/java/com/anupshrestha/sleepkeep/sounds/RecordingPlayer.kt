package com.anupshrestha.sleepkeep.sounds

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.annotation.RawRes
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Loops a bundled recording (res/raw) without an audible seam. Android's MediaPlayer decodes it
 * in real time; near the end a second player starts from the beginning and the two crossfade,
 * so the loop point is never a sudden jump.
 */
class RecordingPlayer(private val context: Context, @RawRes private val resId: Int) {

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun newPlayer(): MediaPlayer = MediaPlayer().apply {
        setAudioAttributes(attributes)
        context.resources.openRawResourceFd(resId).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
        prepare()
        setVolume(0f, 0f)
    }

    /**
     * Plays until cancelled or [keepGoing] turns false. [gain] (0–1) is read every 50 ms, which
     * is how the timer's fade-in and fade-out reach the recording.
     */
    suspend fun play(gain: () -> Float, keepGoing: () -> Boolean) {
        var current = newPlayer()
        var next: MediaPlayer? = null
        try {
            current.start()
            while (currentCoroutineContext().isActive && keepGoing()) {
                val g = gain()
                val remaining = (current.duration - current.currentPosition).coerceAtLeast(0)
                if (next == null && remaining <= CROSSFADE_MILLIS) {
                    next = newPlayer().also { it.start() }
                }
                val incoming = next
                if (incoming == null) {
                    current.setVolume(g, g)
                } else {
                    // Fade the ending pass out while the new pass fades in.
                    val t = (1f - remaining.toFloat() / CROSSFADE_MILLIS).coerceIn(0f, 1f)
                    current.setVolume(g * (1 - t), g * (1 - t))
                    incoming.setVolume(g * t, g * t)
                    if (remaining == 0 || !current.isPlaying) {
                        current.release()
                        current = incoming
                        next = null
                    }
                }
                delay(TICK_MILLIS)
            }
        } finally {
            current.release()
            next?.release()
        }
    }

    private companion object {
        const val CROSSFADE_MILLIS = 3_000
        const val TICK_MILLIS = 50L
    }
}
