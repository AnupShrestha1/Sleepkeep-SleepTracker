package com.anupshrestha.sleepkeep.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Builds a real version-1 database file (exactly as the first release creates it) and checks
 * that opening it with the current app keeps every row. Update this when the schema changes.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val name = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun version1DataSurvivesUpgrade() = runTest {
        context.deleteDatabase(name)
        // The v1 tables, copied from app/schemas/.../1.json.
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `sleep_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, `zoneId` TEXT NOT NULL, `fallAsleepMinutes` INTEGER, `awakeMinutes` INTEGER, `monitoringRequested` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `monitoring_segments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `lastHeartbeat` INTEGER NOT NULL, `endTime` INTEGER, `endReason` TEXT, FOREIGN KEY(`sessionId`) REFERENCES `sleep_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_monitoring_segments_sessionId` ON `monitoring_segments` (`sessionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `sound_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `peakAboveBackgroundDb` REAL NOT NULL, FOREIGN KEY(`sessionId`) REFERENCES `sleep_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sound_events_sessionId` ON `sound_events` (`sessionId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'eb197e83550e492569e67f33211a7386')")
            db.execSQL("INSERT INTO sleep_sessions (id, startTime, endTime, zoneId, fallAsleepMinutes, awakeMinutes, monitoringRequested) VALUES (1, 1000, 29801000, 'Asia/Kathmandu', 15, 0, 1)")
            db.execSQL("INSERT INTO sound_events (sessionId, startTime, endTime, peakAboveBackgroundDb) VALUES (1, 5000, 9000, 27.5)")
            db.version = 1
        }

        val upgraded = Room.databaseBuilder(context, SleepDatabase::class.java, name).build()
        try {
            val session = upgraded.sleepDao().getSession(1)!!
            assertEquals(29801000L, session.endTime)
            assertEquals("Asia/Kathmandu", session.zoneId)
            assertEquals(15, session.fallAsleepMinutes)
            assertEquals(true, session.monitoringRequested)
            assertNull(session.restedRating)
            val events = upgraded.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sound_events").use {
                it.moveToFirst(); it.getInt(0)
            }
            assertEquals(1, events)
        } finally {
            upgraded.close()
        }
    }
}
