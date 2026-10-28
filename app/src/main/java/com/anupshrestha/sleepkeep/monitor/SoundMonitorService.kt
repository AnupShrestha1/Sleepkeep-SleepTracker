package com.anupshrestha.sleepkeep.monitor

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.data.EndReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Listens to the microphone during a sleep session. Each stretch of real monitoring is saved as a
 * segment with a heartbeat every minute, so gaps can be shown honestly afterwards.
 *
 * START_NOT_STICKY: Android 14+ won't let a microphone service restart itself in the background,
 * so on every phone the user resumes monitoring from the app instead.
 */
class SoundMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitoringJob: Job? = null

    @Volatile
    private var stopReason = EndReason.USER_STOPPED

    @Volatile
    private var stopRequested = false

    private val container get() = (application as SleepApp).container

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent.getLongExtra(EXTRA_SESSION_ID, -1))
            ACTION_STOP -> {
                stopReason = intent.getStringExtra(EXTRA_REASON) ?: EndReason.USER_STOPPED
                stopRequested = true
                scope.launch {
                    monitoringJob?.cancelAndJoin()
                    finish()
                }
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start(sessionId: Long) {
        if (monitoringJob?.isActive == true || sessionId < 0) return
        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            stopSelf()
            return
        }
        try {
            ServiceCompat.startForeground(
                this,
                MonitorNotification.ID,
                MonitorNotification.build(this, container.clock.millis()),
                if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0,
            )
        } catch (e: RuntimeException) {
            // SecurityException, or ForegroundServiceStartNotAllowedException on Android 12+.
            stopSelf()
            return
        }
        container.monitor.mutableStatus.value = MonitorStatus.Running(sessionId)
        monitoringJob = scope.launch {
            try {
                monitor(sessionId)
            } finally {
                container.monitor.mutableStatus.value = MonitorStatus.NotRunning
            }
            // Ended by itself (microphone error); a stop request finishes the service itself.
            if (!stopRequested) finish()
        }
    }

    private suspend fun monitor(sessionId: Long) {
        val repository = container.repository
        val clock = container.clock
        val status = container.monitor.mutableStatus

        val input = AudioInput.open() ?: return
        var segmentId: Long? = null
        try {
            segmentId = repository.startSegment(sessionId)
            var lastHeartbeat = clock.millis()
            var silentMillis = 0L
            val buffer = ShortArray(input.chunkSize)
            input.record.startRecording()

            while (currentCoroutineContext().isActive) {
                val read = input.record.read(buffer, 0, buffer.size)
                val now = clock.millis()
                if (read < 0) {
                    segmentId?.let { repository.closeSegment(it, now, EndReason.MIC_ERROR) }
                    segmentId = null
                    return
                }

                // Exact digital silence means the microphone is being withheld: another app is
                // recording (e.g. a call) or the user turned the microphone off (Android 12+).
                // A real room always has some noise.
                if ((0 until read).all { buffer[it] == 0.toShort() }) {
                    silentMillis += read * 1000L / input.sampleRate
                    val current = segmentId
                    if (current != null && silentMillis >= SILENCE_LIMIT_MILLIS) {
                        val since = now - silentMillis
                        repository.closeSegment(current, since, EndReason.MIC_SILENCED)
                        segmentId = null
                        status.value = MonitorStatus.Running(sessionId, silencedSince = since)
                    }
                } else {
                    silentMillis = 0
                    if (segmentId == null) {
                        segmentId = repository.startSegment(sessionId)
                        lastHeartbeat = now
                        status.value = MonitorStatus.Running(sessionId)
                    }
                    // Step 5 feeds the level of this chunk to the loud-sound detector here.
                }

                val current = segmentId
                if (current != null && now - lastHeartbeat >= HEARTBEAT_MILLIS) {
                    repository.heartbeat(current)
                    lastHeartbeat = now
                }
            }
        } catch (e: Exception) {
            // E.g. the session was deleted mid-night: stop quietly instead of crashing the app.
            if (e is kotlinx.coroutines.CancellationException) throw e
        } finally {
            withContext(NonCancellable) {
                // A no-op if ending the session already closed it.
                segmentId?.let { repository.closeSegment(it, clock.millis(), stopReason) }
            }
            input.record.stop()
            input.record.release()
        }
    }

    private suspend fun finish() = withContext(Dispatchers.Main) {
        ServiceCompat.stopForeground(this@SoundMonitorService, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        container.monitor.mutableStatus.value = MonitorStatus.NotRunning
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.anupshrestha.sleepkeep.action.START_MONITORING"
        const val ACTION_STOP = "com.anupshrestha.sleepkeep.action.STOP_MONITORING"
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_REASON = "reason"

        private const val HEARTBEAT_MILLIS = 60_000L
        private const val SILENCE_LIMIT_MILLIS = 10_000L

        fun stopIntent(context: Context, reason: String = EndReason.USER_STOPPED): Intent =
            Intent(context, SoundMonitorService::class.java)
                .setAction(ACTION_STOP)
                .putExtra(EXTRA_REASON, reason)
    }
}
