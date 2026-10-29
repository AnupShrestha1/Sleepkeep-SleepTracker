package com.anupshrestha.sleepkeep.monitor

/**
 * Whether the monitoring service is running, kept in memory only. After the process is killed it
 * starts again as [NotRunning], which is how the app notices that monitoring stopped.
 */
sealed interface MonitorStatus {
    data object NotRunning : MonitorStatus

    data class Running(
        val sessionId: Long,
        /** Set while the microphone delivers only silence (another app has it, or it's turned off). */
        val silencedSince: Long? = null,
        /** Listening is paused while sleep sounds play. */
        val pausedForSounds: Boolean = false,
    ) : MonitorStatus
}
