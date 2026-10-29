package com.anupshrestha.sleepkeep.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One press of Start sleep to one press of End sleep. Times are UTC epoch milliseconds;
 * [zoneId] is the phone's time zone at the start, used to show local clock times.
 */
@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    /** `null` while the session is active. */
    val endTime: Long? = null,
    val zoneId: String,
    /** Morning check-in answers; `null` means not answered. */
    val fallAsleepMinutes: Int? = null,
    val awakeMinutes: Int? = null,
    val monitoringRequested: Boolean = false,
    /** "How rested do you feel?" from 1 (exhausted) to 5 (very rested); `null` = not answered. */
    val restedRating: Int? = null,
)

val SleepSession.isActive: Boolean get() = endTime == null

fun SleepSession.timeInBedMillis(): Long? = endTime?.let { it - startTime }

/** Time in bed minus the user's own check-in answers. */
fun SleepSession.sleepEstimateMillis(): Long? {
    val inBed = timeInBedMillis() ?: return null
    val notAsleep = ((fallAsleepMinutes ?: 0) + (awakeMinutes ?: 0)) * 60_000L
    return (inBed - notAsleep).coerceAtLeast(0)
}

/** False when the check-in was skipped, so the estimate is just time in bed. */
val SleepSession.hasCheckIn: Boolean get() = fallAsleepMinutes != null || awakeMinutes != null
