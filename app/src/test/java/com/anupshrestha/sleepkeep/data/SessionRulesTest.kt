package com.anupshrestha.sleepkeep.data

import com.anupshrestha.sleepkeep.data.SessionRules.TimeError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SessionRulesTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private val now = 1_000 * hour

    @Test
    fun `valid night passes`() {
        assertNull(SessionRules.validate(now - 8 * hour, now - hour, now))
    }

    @Test
    fun `end before or equal to start is rejected`() {
        assertEquals(TimeError.END_BEFORE_START, SessionRules.validate(now - hour, now - 2 * hour, now))
        assertEquals(TimeError.END_BEFORE_START, SessionRules.validate(now - hour, now - hour, now))
    }

    @Test
    fun `end in the future is rejected`() {
        assertEquals(TimeError.IN_FUTURE, SessionRules.validate(now - hour, now + minute, now))
    }

    @Test
    fun `too short and too long are rejected`() {
        assertEquals(TimeError.TOO_SHORT, SessionRules.validate(now - 4 * minute, now, now))
        assertEquals(TimeError.TOO_LONG, SessionRules.validate(now - 25 * hour, now, now))
        assertNull(SessionRules.validate(now - 24 * hour, now, now))
    }

    @Test
    fun `sessions over 14 hours are flagged as suspicious`() {
        assertFalse(SessionRules.isSuspiciouslyLong(0, 14 * hour))
        assertTrue(SessionRules.isSuspiciouslyLong(0, 14 * hour + minute))
    }

    @Test
    fun `wake time after midnight lands on the next day`() {
        val zone = ZoneId.of("Asia/Kathmandu")
        val start = ZonedDateTime.of(2026, 10, 27, 23, 5, 0, 0, zone).toInstant().toEpochMilli()
        val end = SessionRules.nextOccurrenceAfter(start, LocalTime.of(6, 45), zone)
        assertEquals(ZonedDateTime.of(2026, 10, 28, 6, 45, 0, 0, zone).toInstant().toEpochMilli(), end)
    }

    @Test
    fun `wake time later the same day stays on that day`() {
        val zone = ZoneId.of("Asia/Kathmandu")
        val start = ZonedDateTime.of(2026, 10, 28, 1, 30, 0, 0, zone).toInstant().toEpochMilli()
        val end = SessionRules.nextOccurrenceAfter(start, LocalTime.of(7, 0), zone)
        assertEquals(ZonedDateTime.of(2026, 10, 28, 7, 0, 0, 0, zone).toInstant().toEpochMilli(), end)
    }

    @Test
    fun `daylight saving night is measured in real time`() {
        // Clocks go back one hour in London at 02:00 on 25 Oct 2026, so 23:00 to 07:00 is 9 hours.
        val zone = ZoneId.of("Europe/London")
        val start = ZonedDateTime.of(2026, 10, 24, 23, 0, 0, 0, zone).toInstant().toEpochMilli()
        val end = SessionRules.nextOccurrenceAfter(start, LocalTime.of(7, 0), zone)
        assertEquals(9 * hour, end - start)
    }

    @Test
    fun `sleep estimate subtracts check-in answers`() {
        val session = SleepSession(startTime = 0, endTime = 8 * hour, zoneId = "UTC", fallAsleepMinutes = 30, awakeMinutes = 15)
        assertEquals(8 * hour - 45 * minute, session.sleepEstimateMillis())
        assertTrue(session.hasCheckIn)
    }

    @Test
    fun `skipped check-in falls back to time in bed`() {
        val session = SleepSession(startTime = 0, endTime = 8 * hour, zoneId = "UTC")
        assertEquals(8 * hour, session.sleepEstimateMillis())
        assertFalse(session.hasCheckIn)
    }
}
