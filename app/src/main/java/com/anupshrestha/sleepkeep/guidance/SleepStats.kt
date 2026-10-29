package com.anupshrestha.sleepkeep.guidance

import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.hasCheckIn
import com.anupshrestha.sleepkeep.data.sleepEstimateMillis
import com.anupshrestha.sleepkeep.data.timeInBedMillis
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
    data class NightBar(val date: LocalDate, val sleepMillis: Long?, val timeInBedOnly: Boolean)

    /** The last 7 days, oldest first, with each day's main sleep (naps left out). */
    fun lastSevenNights(sessions: List<SleepSession>, today: LocalDate): List<NightBar> {
        val naps = napIds(sessions)
        val byDate = sessions
            .filter { it.endTime != null && it.id !in naps }
            .associateBy { nightDate(it) }
        return (SleepGuidelines.WINDOW_DAYS - 1 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val night = byDate[date]
            NightBar(date, night?.sleepEstimateMillis(), night != null && !night.hasCheckIn)
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
     * Duration and schedule for the main sleeps whose night date is within the last 7 days,
     * [today] included. Naps are left out, and missing nights don't count as zero.
     */
    fun weekly(sessions: List<SleepSession>, today: LocalDate): Weekly {
        val firstDay = today.minusDays(SleepGuidelines.WINDOW_DAYS - 1L)
        val naps = napIds(sessions)
        val nights = sessions.filter { session ->
            val date = nightDate(session)
            date != null && session.id !in naps && !date.isBefore(firstDay) && !date.isAfter(today)
        }
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
