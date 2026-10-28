package com.anupshrestha.sleepkeep.data

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Plain rules for session times, kept free of Android so they can be unit-tested. */
object SessionRules {
    const val MIN_DURATION_MILLIS = 5 * 60_000L
    const val MAX_DURATION_MILLIS = 24 * 60 * 60_000L

    /** Ending a session longer than this asks whether the user forgot to end it. */
    const val LONG_SESSION_MILLIS = 14 * 60 * 60_000L

    enum class TimeError { END_BEFORE_START, IN_FUTURE, TOO_SHORT, TOO_LONG, OVERLAPS }

    /** Checks a completed session's times. Overlap needs the database, so it's checked separately. */
    fun validate(start: Long, end: Long, now: Long): TimeError? = when {
        end <= start -> TimeError.END_BEFORE_START
        end > now -> TimeError.IN_FUTURE
        end - start < MIN_DURATION_MILLIS -> TimeError.TOO_SHORT
        end - start > MAX_DURATION_MILLIS -> TimeError.TOO_LONG
        else -> null
    }

    fun isSuspiciouslyLong(start: Long, end: Long): Boolean = end - start > LONG_SESSION_MILLIS

    /** The first time after [start] that the clock shows [wakeTime], in [zone]. */
    fun nextOccurrenceAfter(start: Long, wakeTime: LocalTime, zone: ZoneId): Long {
        val startLocal = Instant.ofEpochMilli(start).atZone(zone)
        var candidate = startLocal.with(wakeTime)
        if (!candidate.isAfter(startLocal)) candidate = candidate.plusDays(1)
        return candidate.toInstant().toEpochMilli()
    }
}
