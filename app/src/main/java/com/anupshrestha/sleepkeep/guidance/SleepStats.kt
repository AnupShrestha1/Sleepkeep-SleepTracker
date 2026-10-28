package com.anupshrestha.sleepkeep.guidance

import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.timeInBedMillis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Sleep calculations, kept free of Android so they can be unit-tested. Weekly figures come in step 6. */
object SleepStats {

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
}
