package com.anupshrestha.sleepkeep.monitor

import com.anupshrestha.sleepkeep.data.EndReason
import com.anupshrestha.sleepkeep.data.MonitoringSegment
import com.anupshrestha.sleepkeep.monitor.Coverage.Gap
import com.anupshrestha.sleepkeep.monitor.Coverage.GapReason
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverageTest {

    private val minute = 60_000L
    private val hour = 60 * minute

    private fun segment(start: Long, end: Long?, reason: String?, heartbeat: Long = end ?: start) =
        MonitoringSegment(sessionId = 1, startTime = start, lastHeartbeat = heartbeat, endTime = end, endReason = reason)

    @Test
    fun `fully monitored night has no gaps`() {
        val result = Coverage.of(0, 8 * hour, listOf(segment(0, 8 * hour, EndReason.SESSION_ENDED)))
        assertEquals(8 * hour, result.monitoredMillis)
        assertEquals(emptyList<Gap>(), result.gaps)
    }

    @Test
    fun `killed app leaves a gap until monitoring resumed`() {
        val result = Coverage.of(
            0, 8 * hour,
            listOf(
                segment(0, 3 * hour, EndReason.STOPPED_UNEXPECTEDLY),
                segment(4 * hour, 8 * hour, EndReason.SESSION_ENDED),
            ),
        )
        assertEquals(7 * hour, result.monitoredMillis)
        assertEquals(listOf(Gap(3 * hour, 4 * hour, GapReason.STOPPED_UNEXPECTEDLY)), result.gaps)
    }

    @Test
    fun `never resumed after a kill leaves a gap to the end`() {
        // Still open: the stale-segment cleanup hasn't run yet; it ends at its heartbeat.
        val result = Coverage.of(0, 8 * hour, listOf(segment(0, null, null, heartbeat = 2 * hour)))
        assertEquals(2 * hour, result.monitoredMillis)
        assertEquals(listOf(Gap(2 * hour, 8 * hour, GapReason.STOPPED_UNEXPECTEDLY)), result.gaps)
    }

    @Test
    fun `silenced microphone and late start are labelled`() {
        val result = Coverage.of(
            0, 8 * hour,
            listOf(
                segment(30 * minute, 2 * hour, EndReason.MIC_SILENCED),
                segment(2 * hour + 20 * minute, 8 * hour, EndReason.SESSION_ENDED),
            ),
        )
        assertEquals(
            listOf(
                Gap(0, 30 * minute, GapReason.NOT_RUNNING),
                Gap(2 * hour, 2 * hour + 20 * minute, GapReason.MIC_SILENCED),
            ),
            result.gaps,
        )
    }

    @Test
    fun `gaps under two minutes are ignored`() {
        val result = Coverage.of(0, 8 * hour, listOf(segment(30_000, 8 * hour, EndReason.SESSION_ENDED)))
        assertEquals(emptyList<Gap>(), result.gaps)
    }

    @Test
    fun `segments outside edited session times are clipped`() {
        // The user moved bedtime later and wake-up earlier after the night was recorded.
        val result = Coverage.of(hour, 7 * hour, listOf(segment(0, 8 * hour, EndReason.SESSION_ENDED)))
        assertEquals(6 * hour, result.monitoredMillis)
        assertEquals(emptyList<Gap>(), result.gaps)
    }

    @Test
    fun `no segments at all is one gap`() {
        val result = Coverage.of(0, 8 * hour, emptyList())
        assertEquals(0L, result.monitoredMillis)
        assertEquals(listOf(Gap(0, 8 * hour, GapReason.NOT_RUNNING)), result.gaps)
    }
}
