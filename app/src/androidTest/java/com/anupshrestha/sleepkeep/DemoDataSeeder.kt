package com.anupshrestha.sleepkeep

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anupshrestha.sleepkeep.data.EndReason
import com.anupshrestha.sleepkeep.data.MonitoringSegment
import com.anupshrestha.sleepkeep.data.SleepSession
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Replaces the app's data with six realistic nights, for screenshots and UI checks. Skipped in
 * normal test runs; run it on purpose with:
 *
 * adb shell am instrument -w -e seed true -e class com.anupshrestha.sleepkeep.DemoDataSeeder \
 *   com.anupshrestha.sleepkeep.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class DemoDataSeeder {

    @Test
    fun seed() {
        // Does nothing (and passes) in normal test runs. An assumption would be cleaner, but the
        // Gradle device runner reports a skipped assumption as a failure.
        if (InstrumentationRegistry.getArguments().getString("seed") != "true") return
        runBlocking { seedNights() }
    }

    private suspend fun seedNights() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SleepApp
        val repository = app.container.repository
        val dao = app.container.database.sleepDao()
        repository.deleteAll()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun at(daysAgo: Long, time: String) =
            ZonedDateTime.of(today.minusDays(daysAgo), LocalTime.parse(time), zone).toInstant().toEpochMilli()

        // daysAgo of wake-up, bedtime, wake-up, fall asleep, awake, monitored
        data class Night(val day: Long, val bed: String, val up: String, val fall: Int?, val awake: Int?, val mic: Boolean)
        val nights = listOf(
            Night(6, "23:10", "06:40", 15, 0, false),
            Night(5, "22:50", "06:35", 5, 15, true),
            Night(4, "00:20", "06:30", 30, 15, false),
            Night(3, "23:05", "07:10", 15, 0, true),
            Night(2, "23:40", "06:50", null, null, false),
            Night(1, "22:55", "06:45", 15, 15, true),
        )
        nights.forEach { n ->
            val start = if (n.bed < "12:00") at(n.day, n.bed) else at(n.day + 1, n.bed)
            val end = at(n.day, n.up)
            val id = dao.insertSession(
                SleepSession(
                    startTime = start, endTime = end, zoneId = zone.id,
                    fallAsleepMinutes = n.fall, awakeMinutes = n.awake, monitoringRequested = n.mic,
                )
            )
            if (!n.mic) return@forEach
            if (n.day == 3L) {
                // A night where the phone closed the app for a while.
                val stop = start + 3 * HOUR + 20 * MINUTE
                dao.insertSegment(MonitoringSegment(sessionId = id, startTime = start + MINUTE, lastHeartbeat = stop, endTime = stop, endReason = EndReason.STOPPED_UNEXPECTEDLY))
                dao.insertSegment(MonitoringSegment(sessionId = id, startTime = stop + 40 * MINUTE, lastHeartbeat = end, endTime = end, endReason = EndReason.SESSION_ENDED))
            } else {
                dao.insertSegment(MonitoringSegment(sessionId = id, startTime = start + MINUTE, lastHeartbeat = end, endTime = end, endReason = EndReason.SESSION_ENDED))
            }
            listOf(
                Triple(1 * HOUR + 47 * MINUTE, 6_000L, 22f),
                Triple(3 * HOUR + 2 * MINUTE, 28_000L, 31f),
                Triple(4 * HOUR + 18 * MINUTE, 3_000L, 38f),
                Triple(5 * HOUR + 41 * MINUTE, 12_000L, 19f),
                Triple(6 * HOUR + 55 * MINUTE, 4_000L, 27f),
            ).forEach { (offset, length, peak) ->
                if (start + offset < end) repository.saveSoundEvent(id, start + offset, start + offset + length, peak)
            }
        }
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
