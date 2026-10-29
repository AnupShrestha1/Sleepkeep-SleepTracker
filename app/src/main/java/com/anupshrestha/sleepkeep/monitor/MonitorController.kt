package com.anupshrestha.sleepkeep.monitor

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.anupshrestha.sleepkeep.data.EndReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Starts and stops [SoundMonitorService]; the UI and the service share its [status]. */
class MonitorController(private val context: Context) {

    internal val mutableStatus = MutableStateFlow<MonitorStatus>(MonitorStatus.NotRunning)
    val status: StateFlow<MonitorStatus> = mutableStatus.asStateFlow()

    /** The microphone's latest level in dBFS, for the on-screen meter. Kept in memory only. */
    internal val mutableLevel = MutableStateFlow(LoudnessDetector.MIN_DB)
    val level: StateFlow<Float> = mutableLevel.asStateFlow()

    /**
     * Call only while the app is on screen: Android 14+ refuses to start a microphone service from
     * the background. Returns false if Android refused to start it.
     */
    fun start(sessionId: Long): Boolean = try {
        ContextCompat.startForegroundService(
            context,
            Intent(context, SoundMonitorService::class.java)
                .setAction(SoundMonitorService.ACTION_START)
                .putExtra(SoundMonitorService.EXTRA_SESSION_ID, sessionId),
        )
        true
    } catch (e: IllegalStateException) {
        // ForegroundServiceStartNotAllowedException (Android 12+) is an IllegalStateException.
        false
    }

    fun stop(reason: String = EndReason.USER_STOPPED) {
        if (status.value !is MonitorStatus.Running) return
        try {
            context.startService(SoundMonitorService.stopIntent(context, reason))
        } catch (e: IllegalStateException) {
            // Not allowed from the background; the service is gone with its process anyway.
        }
    }
}
