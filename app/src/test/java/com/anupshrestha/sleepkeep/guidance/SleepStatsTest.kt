package com.anupshrestha.sleepkeep.guidance

import com.anupshrestha.sleepkeep.data.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SleepStatsTest {

    private val zone = ZoneId.of("Asia/Kathmandu")

    private fun at(day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun session(id: Long, start: Long, end: Long?) =
        SleepSession(id = id, startTime = start, endTime = end, zoneId = zone.id)

    @Test
    fun `night belongs to the date it ended`() {
        assertEquals(LocalDate.of(2026, 10, 28), SleepStats.nightDate(session(1, at(27, 23), at(28, 7))))
    }

    @Test
    fun `shorter session ending the same day is a nap`() {
        val night = session(1, at(27, 23), at(28, 7))
        val nap = session(2, at(28, 14), at(28, 15))
        assertEquals(setOf(2L), SleepStats.napIds(listOf(night, nap)))
    }

    @Test
    fun `sessions on different days are all main sleeps`() {
        val first = session(1, at(26, 23), at(27, 7))
        val second = session(2, at(27, 23), at(28, 6))
        assertEquals(emptySet<Long>(), SleepStats.napIds(listOf(first, second)))
    }

    @Test
    fun `active session is ignored`() {
        val night = session(1, at(27, 23), at(28, 7))
        val active = session(2, at(28, 13), null)
        assertEquals(emptySet<Long>(), SleepStats.napIds(listOf(night, active)))
    }
}
