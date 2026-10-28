package com.anupshrestha.sleepkeep.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SleepSession::class, MonitoringSegment::class, SoundEvent::class],
    version = 1,
    exportSchema = true,
)
abstract class SleepDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao

    companion object {
        fun create(context: Context): SleepDatabase =
            Room.databaseBuilder(context, SleepDatabase::class.java, "sleepkeep.db").build()
    }
}
