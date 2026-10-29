package com.anupshrestha.sleepkeep.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Every schema change needs a new version and a migration, or users lose their data on update.
 * The exported schemas in app/schemas/ let Room generate simple migrations.
 *
 * - v2: `sleep_sessions.restedRating` (the morning "How rested do you feel?" answer).
 */
@Database(
    entities = [SleepSession::class, MonitoringSegment::class, SoundEvent::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class SleepDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao

    companion object {
        const val NAME = "sleepkeep.db"

        fun create(context: Context): SleepDatabase =
            Room.databaseBuilder(context, SleepDatabase::class.java, NAME).build()
    }
}
