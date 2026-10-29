package com.anupshrestha.sleepkeep.monitor

import com.anupshrestha.sleepkeep.data.EndReason
import com.anupshrestha.sleepkeep.data.MonitoringSegment

/** How much of a session was actually monitored, and why the rest wasn't. Plain Kotlin. */
object Coverage {

    enum class GapReason { NOT_RUNNING, USER_STOPPED, MIC_SILENCED, MIC_ERROR, STOPPED_UNEXPECTEDLY, SOUNDS_PLAYING }

    data class Gap(val start: Long, val end: Long, val reason: GapReason)

    data class Result(val monitoredMillis: Long, val gaps: List<Gap>)

    /** Gaps shorter than this are start-up delays, not worth showing. */
    const val MIN_GAP_MILLIS = 2 * 60_000L

    fun of(sessionStart: Long, sessionEnd: Long, segments: List<MonitoringSegment>): Result {
        var monitored = 0L
        val gaps = mutableListOf<Gap>()
        var cursor = sessionStart
        var reason = GapReason.NOT_RUNNING

        for (segment in segments.sortedBy { it.startTime }) {
            // An open segment of a killed service ends at its last heartbeat.
            val start = segment.startTime.coerceIn(sessionStart, sessionEnd)
            val end = (segment.endTime ?: segment.lastHeartbeat).coerceIn(sessionStart, sessionEnd)
            if (start - cursor >= MIN_GAP_MILLIS) gaps += Gap(cursor, start, reason)
            if (end > maxOf(start, cursor)) {
                monitored += end - maxOf(start, cursor)
                cursor = end
            }
            reason = gapReasonAfter(segment.endReason)
        }
        if (sessionEnd - cursor >= MIN_GAP_MILLIS) gaps += Gap(cursor, sessionEnd, reason)
        return Result(monitored, gaps)
    }

    private fun gapReasonAfter(endReason: String?): GapReason = when (endReason) {
        EndReason.USER_STOPPED -> GapReason.USER_STOPPED
        EndReason.MIC_SILENCED -> GapReason.MIC_SILENCED
        EndReason.MIC_ERROR -> GapReason.MIC_ERROR
        EndReason.SOUNDS_PLAYING -> GapReason.SOUNDS_PLAYING
        EndReason.STOPPED_UNEXPECTEDLY, null -> GapReason.STOPPED_UNEXPECTEDLY
        // SESSION_ENDED: a gap after it only appears if the end time was edited later.
        else -> GapReason.NOT_RUNNING
    }
}
