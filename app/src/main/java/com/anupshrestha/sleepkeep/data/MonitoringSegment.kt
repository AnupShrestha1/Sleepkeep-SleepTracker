package com.anupshrestha.sleepkeep.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A stretch of a session during which the microphone was actually being monitored. */
@Entity(
    tableName = "monitoring_segments",
    foreignKeys = [
        ForeignKey(
            entity = SleepSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class MonitoringSegment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startTime: Long,
    /** Updated every minute while monitoring runs, so a killed process leaves a trace. */
    val lastHeartbeat: Long,
    /** `null` while running. */
    val endTime: Long? = null,
    /** One of [EndReason]'s constants; `null` while running. */
    val endReason: String? = null,
)

object EndReason {
    const val USER_STOPPED = "USER_STOPPED"
    const val SESSION_ENDED = "SESSION_ENDED"
    const val MIC_SILENCED = "MIC_SILENCED"
    const val MIC_ERROR = "MIC_ERROR"
    const val STOPPED_UNEXPECTEDLY = "STOPPED_UNEXPECTEDLY"

    /** Paused while sleep sounds played, so the speaker isn't counted as loud sounds. */
    const val SOUNDS_PLAYING = "SOUNDS_PLAYING"
}
