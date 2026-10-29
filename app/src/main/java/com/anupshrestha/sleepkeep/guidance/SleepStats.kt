package com.anupshrestha.sleepkeep.guidance

import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.hasCheckIn
import com.anupshrestha.sleepkeep.data.sleepEstimateMillis
import com.anupshrestha.sleepkeep.data.timeInBedMillis
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Sleep calculations, kept free of Android so they can be unit-tested. */
object SleepStats {

    private const val MINUTES_PER_DAY = 24 * 60

    /** A session belongs to the local date it ended on. */
    fun nightDate(session: SleepSession): LocalDate? = session.endTime?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.of(session.zoneId)).toLocalDate()
    }

    /** Completed sessions that aren't the longest one ending on their date. */
    fun napIds(sessions: List<SleepSession>): Set<Long> =
        sessions
            .filter { it.endTime != null }
            .groupBy { nightDate(it) }
            .values
            .flatMap { sameDate ->
                val main = sameDate.maxBy { it.timeInBedMillis() ?: 0 }
                sameDate.filter { it.id != main.id }.map { it.id }
            }
            .toSet()

    /** One day in a 7-day chart; [sleepMillis] is `null` when no night ended that day. */
    data class NightBar(
        val date: LocalDate,
        val sleepMillis: Long?,
        val timeInBedOnly: Boolean,
        /** Local clock times as minutes after midnight; `null` when no night ended that day. */
        val bedMinute: Int? = null,
        val wakeMinute: Int? = null,
    )

    /** The last 7 days, oldest first, with each day's main sleep (naps left out). */
    fun lastSevenNights(sessions: List<SleepSession>, today: LocalDate): List<NightBar> =
        lastNights(sessions, today, SleepGuidelines.WINDOW_DAYS)

    /** The last [days] days, oldest first, with each day's main sleep (naps left out). */
    fun lastNights(sessions: List<SleepSession>, today: LocalDate, days: Int): List<NightBar> {
        val byDate = mainSleeps(sessions).associateBy { nightDate(it) }
        return (days - 1 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val night = byDate[date]
            NightBar(
                date = date,
                sleepMillis = night?.sleepEstimateMillis(),
                timeInBedOnly = night != null && !night.hasCheckIn,
                bedMinute = night?.let { minuteOfDay(it.startTime, it.zoneId) },
                wakeMinute = night?.let { minuteOfDay(it.endTime!!, it.zoneId) },
            )
        }
    }

    /**
     * For each day, the mean sleep of the nights in the [window] days ending that day, or `null`
     * when fewer than 3 of them were recorded. Smooths out one-off nights in long charts.
     */
    fun rollingAverage(nights: List<NightBar>, window: Int = SleepGuidelines.WINDOW_DAYS): List<Long?> =
        nights.indices.map { i ->
            val values = nights.subList(maxOf(0, i - window + 1), i + 1).mapNotNull { it.sleepMillis }
            if (values.size < SleepGuidelines.MIN_NIGHTS) null else values.sum() / values.size
        }

    /** Average sleep and usual bedtime for one part of the week. */
    data class WeekPart(val nights: Int, val averageSleepMillis: Long, val bedtime: TimeConsistency)

    /** Weekday nights against weekend nights; `null` sides have fewer than 2 nights. */
    data class WeekdayWeekend(val weekdays: WeekPart?, val weekend: WeekPart?)

    /**
     * Splits the last [days] days' main sleeps by the day they ended on. Weekend nights are the
     * ones ending on Saturday or Sunday, i.e. Friday and Saturday night.
     */
    fun weekdayWeekend(sessions: List<SleepSession>, today: LocalDate, days: Int): WeekdayWeekend {
        val (weekend, weekdays) = nightsWithin(sessions, today, days).partition {
            nightDate(it)!!.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        }
        fun part(nights: List<SleepSession>): WeekPart? {
            if (nights.size < 2) return null
            val sleep = nights.map { it.sleepEstimateMillis() ?: 0 }
            return WeekPart(nights.size, sleep.sum() / sleep.size, consistency(nights.map { minuteOfDay(it.startTime, it.zoneId) }))
        }
        return WeekdayWeekend(part(weekdays), part(weekend))
    }

    private fun mainSleeps(sessions: List<SleepSession>): List<SleepSession> {
        val naps = napIds(sessions)
        return sessions.filter { it.endTime != null && it.id !in naps }
    }

    /** Main sleeps whose night date is within the last [days] days, [today] included. */
    private fun nightsWithin(sessions: List<SleepSession>, today: LocalDate, days: Int): List<SleepSession> {
        val firstDay = today.minusDays(days - 1L)
        return mainSleeps(sessions).filter { session ->
            val date = nightDate(session)!!
            !date.isBefore(firstDay) && !date.isAfter(today)
        }
    }

    /** A clock time that's usual for this person, and how far the nights typically were from it. */
    data class TimeConsistency(val usualMinuteOfDay: Int, val averageDifferenceMinutes: Int)

    sealed interface Weekly {
        data class NotEnoughData(val nights: Int) : Weekly

        data class Summary(
            val nights: Int,
            val averageSleepMillis: Long,
            val meetsGuideline: Boolean,
            val nightsMeetingGuideline: Int,
            /** Nights without a check-in, so their sleep estimate is just time in bed. */
            val timeInBedOnlyNights: Int,
            val bedtime: TimeConsistency,
            val wakeTime: TimeConsistency,
        ) : Weekly
    }

    /**
     * Duration and schedule for the main sleeps whose night date is within the last [days] days
     * (7 by default), [today] included. Naps are left out, and missing nights don't count as zero.
     */
    fun weekly(sessions: List<SleepSession>, today: LocalDate, days: Int = SleepGuidelines.WINDOW_DAYS): Weekly {
        val nights = nightsWithin(sessions, today, days)
        if (nights.size < SleepGuidelines.MIN_NIGHTS) return Weekly.NotEnoughData(nights.size)

        val sleep = nights.map { it.sleepEstimateMillis() ?: 0 }
        val average = sleep.sum() / sleep.size
        return Weekly.Summary(
            nights = nights.size,
            averageSleepMillis = average,
            meetsGuideline = average >= SleepGuidelines.MIN_ADULT_SLEEP_MILLIS,
            nightsMeetingGuideline = sleep.count { it >= SleepGuidelines.MIN_ADULT_SLEEP_MILLIS },
            timeInBedOnlyNights = nights.count { !it.hasCheckIn },
            bedtime = consistency(nights.map { minuteOfDay(it.startTime, it.zoneId) }),
            wakeTime = consistency(nights.map { minuteOfDay(it.endTime!!, it.zoneId) }),
        )
    }

    private fun minuteOfDay(millis: Long, zoneId: String): Int {
        val time = Instant.ofEpochMilli(millis).atZone(ZoneId.of(zoneId)).toLocalTime()
        return time.hour * 60 + time.minute
    }

    /**
     * Clock times sit on a 24-hour circle, so the usual time is a circular mean and differences
     * wrap around midnight: 23:30 and 00:30 are 60 minutes apart, not 23 hours.
     */
    fun consistency(minutesOfDay: List<Int>): TimeConsistency {
        val angles = minutesOfDay.map { it * 2 * PI / MINUTES_PER_DAY }
        val meanAngle = atan2(angles.sumOf { sin(it) }, angles.sumOf { cos(it) })
        val usual = ((meanAngle * MINUTES_PER_DAY / (2 * PI)).roundToInt() + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val averageDifference = minutesOfDay.map { minutesApart(it, usual) }.average()
        return TimeConsistency(usual, averageDifference.roundToInt())
    }

    /** Distance between two clock times, the short way round. */
    fun minutesApart(a: Int, b: Int): Int {
        val diff = abs(a - b) % MINUTES_PER_DAY
        return minOf(diff, MINUTES_PER_DAY - diff)
    }
}
