package com.anupshrestha.sleepkeep.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A period of sound louder than the room's background. No audio is stored. */
@Entity(
    tableName = "sound_events",
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
data class SoundEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long,
    /** Relative to the room's background; phone microphones aren't calibrated. */
    val peakAboveBackgroundDb: Float,
)
