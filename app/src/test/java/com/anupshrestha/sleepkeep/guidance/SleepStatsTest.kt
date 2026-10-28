package com.anupshrestha.sleepkeep.guidance

import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.guidance.SleepStats.Weekly
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SleepStatsTest {

    private val zone = ZoneId.of("Asia/Kathmandu")
    private val hour = 60 * 60_000L
    private val today = LocalDate.of(2026, 10, 28)

    private fun at(day: Int, hour: Int, minute: Int = 0, z: ZoneId = zone) =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, z).toInstant().toEpochMilli()

    private fun session(
        id: Long, start: Long, end: Long?,
        fallAsleep: Int? = null, awake: Int? = null, z: ZoneId = zone,
    ) = SleepSession(id = id, startTime = start, endTime = end, zoneId = z.id,
        fallAsleepMinutes = fallAsleep, awakeMinutes = awake)

    /** A night from [bed] on day-1 to [wake] on [day]; hours may be fractional via minutes. */
    private fun night(id: Long, day: Int, bedH: Int, bedM: Int, wakeH: Int, wakeM: Int, fallAsleep: Int? = 0) =
        session(
            id,
            if (bedH >= 12) at(day - 1, bedH, bedM) else at(day, bedH, bedM),
            at(day, wakeH, wakeM),
            fallAsleep = fallAsleep,
        )

    // Night dates and naps

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

    // Weekly figures

    @Test
    fun `fewer than 3 nights is not enough data`() {
        val result = SleepStats.weekly(listOf(night(1, 27, 23, 0, 7, 0), night(2, 28, 23, 0, 7, 0)), today)
        assertEquals(Weekly.NotEnoughData(2), result)
    }

    @Test
    fun `average meets the guideline and nights are counted`() {
        val result = SleepStats.weekly(
            listOf(
                night(1, 26, 23, 0, 7, 0), // 8 h
                night(2, 27, 23, 0, 6, 0), // 7 h
                night(3, 28, 23, 0, 5, 0), // 6 h
            ),
            today,
        ) as Weekly.Summary
        assertEquals(3, result.nights)
        assertEquals(7 * hour, result.averageSleepMillis)
        assertTrue(result.meetsGuideline)
        assertEquals(2, result.nightsMeetingGuideline)
    }

    @Test
    fun `check-in answers lower the average and skipped ones are counted`() {
        val result = SleepStats.weekly(
            listOf(
                night(1, 26, 23, 0, 7, 0, fallAsleep = 60), // 7 h asleep
                night(2, 27, 23, 0, 7, 0, fallAsleep = 90), // 6.5 h
                night(3, 28, 23, 0, 6, 30, fallAsleep = null), // 7.5 h in bed, no check-in
            ),
            today,
        ) as Weekly.Summary
        assertEquals(7 * hour, result.averageSleepMillis)
        assertEquals(1, result.timeInBedOnlyNights)
    }

    @Test
    fun `below the guideline`() {
        val result = SleepStats.weekly(
            listOf(night(1, 26, 0, 0, 6, 0), night(2, 27, 0, 30, 6, 0), night(3, 28, 1, 0, 6, 0)),
            today,
        ) as Weekly.Summary
        assertFalse(result.meetsGuideline)
        assertEquals(0, result.nightsMeetingGuideline)
    }

    @Test
    fun `naps and nights older than 7 days are left out`() {
        val result = SleepStats.weekly(
            listOf(
                night(1, 20, 23, 0, 7, 0), // 8 days ago
                night(2, 26, 23, 0, 7, 0),
                night(3, 27, 23, 0, 7, 0),
                night(4, 28, 23, 0, 7, 0),
                session(5, at(28, 14), at(28, 15), fallAsleep = 0), // nap
            ),
            today,
        ) as Weekly.Summary
        assertEquals(3, result.nights)
        assertEquals(8 * hour, result.averageSleepMillis)
    }

    @Test
    fun `daylight saving night counts its real length`() {
        // London clocks go back at 02:00 on 25 Oct 2026: 23:00 to 07:00 is 9 hours.
        val london = ZoneId.of("Europe/London")
        val sessions = listOf(
            session(1, at(24, 23, z = london), at(25, 7, z = london), fallAsleep = 0, z = london),
            session(2, at(25, 23, z = london), at(26, 7, z = london), fallAsleep = 0, z = london),
            session(3, at(26, 23, z = london), at(27, 7, z = london), fallAsleep = 0, z = london),
        )
        val result = SleepStats.weekly(sessions, LocalDate.of(2026, 10, 27)) as Weekly.Summary
        assertEquals((9 + 8 + 8) * hour / 3, result.averageSleepMillis)
    }

    // Consistency

    @Test
    fun `bedtimes either side of midnight are close together`() {
        // 23:30, 00:30, 00:00 → usual 00:00; 30, 30 and 0 minutes from it, so 20 on average.
        val c = SleepStats.consistency(listOf(23 * 60 + 30, 30, 0))
        assertEquals(0, c.usualMinuteOfDay)
        assertEquals(20, c.averageDifferenceMinutes)
    }

    @Test
    fun `identical times vary by zero`() {
        val c = SleepStats.consistency(listOf(7 * 60, 7 * 60, 7 * 60))
        assertEquals(7 * 60, c.usualMinuteOfDay)
        assertEquals(0, c.averageDifferenceMinutes)
    }

    @Test
    fun `minutes apart wraps around midnight`() {
        assertEquals(60, SleepStats.minutesApart(23 * 60 + 30, 30))
        assertEquals(60, SleepStats.minutesApart(30, 23 * 60 + 30))
        assertEquals(720, SleepStats.minutesApart(0, 12 * 60))
    }

    @Test
    fun `weekly summary reports bedtime and wake time separately`() {
        val result = SleepStats.weekly(
            listOf(night(1, 26, 23, 0, 7, 0), night(2, 27, 23, 30, 7, 0), night(3, 28, 0, 30, 7, 0)),
            today,
        ) as Weekly.Summary
        assertEquals(23 * 60 + 40, result.bedtime.usualMinuteOfDay)
        // 23:00, 23:30, 00:30 are 40, 10 and 50 minutes from 23:40.
        assertEquals(33, result.bedtime.averageDifferenceMinutes)
        assertEquals(7 * 60, result.wakeTime.usualMinuteOfDay)
        assertEquals(0, result.wakeTime.averageDifferenceMinutes)
    }
}
