package com.anupshrestha.sleepkeep.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SleepRepositoryTest {

    private val hour = 60 * 60_000L
    private var nowMillis = 1_000 * hour

    // A clock the tests can move forward.
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(nowMillis)
    }

    private lateinit var db: SleepDatabase
    private lateinit var repository: SleepRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SleepDatabase::class.java
        ).build()
        repository = SleepRepository(db, clock) { ZoneOffset.UTC }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun onlyOneActiveSessionCanExist() = runTest {
        assertTrue(repository.startSession() is SleepRepository.StartResult.Started)
        assertEquals(SleepRepository.StartResult.AlreadyActive, repository.startSession())
    }

    @Test
    fun endingSetsEndTimeAndAllowsANewStart() = runTest {
        repository.startSession()
        nowMillis += 8 * hour
        val ended = repository.endSession()
        assertNotNull(ended)
        assertEquals(nowMillis, ended!!.endTime)
        assertNull(db.sleepDao().getActiveSession())
        assertTrue(repository.startSession() is SleepRepository.StartResult.Started)
    }

    @Test
    fun endingClosesOpenMonitoringSegments() = runTest {
        val id = (repository.startSession() as SleepRepository.StartResult.Started).sessionId
        db.sleepDao().insertSegment(MonitoringSegment(sessionId = id, startTime = nowMillis, lastHeartbeat = nowMillis))
        nowMillis += 8 * hour
        repository.endSession()
        val segment = db.sleepDao().getSegments(id).single()
        assertEquals(nowMillis, segment.endTime)
        assertEquals(EndReason.SESSION_ENDED, segment.endReason)
    }

    @Test
    fun editingIntoAnotherSessionIsRejected() = runTest {
        repository.startSession()
        nowMillis += 8 * hour
        val first = repository.endSession()!!
        nowMillis += 2 * hour
        repository.startSession()
        nowMillis += 8 * hour
        val second = repository.endSession()!!

        // Move the second session's start back into the first one.
        val error = repository.updateTimes(second.id, first.endTime!! - hour, second.endTime!!)
        assertEquals(SessionRules.TimeError.OVERLAPS, error)

        // A change that stays clear of the first night is saved.
        assertNull(repository.updateTimes(second.id, second.startTime - hour, second.endTime!!))
        assertEquals(second.startTime - hour, repository.getSession(second.id)!!.startTime)
    }

    @Test
    fun deletingASessionDeletesItsSegmentsAndEvents() = runTest {
        val id = (repository.startSession() as SleepRepository.StartResult.Started).sessionId
        val dao = db.sleepDao()
        dao.insertSegment(MonitoringSegment(sessionId = id, startTime = nowMillis, lastHeartbeat = nowMillis))
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO sound_events (sessionId, startTime, endTime, peakAboveBackgroundDb) VALUES ($id, $nowMillis, $nowMillis, 20)"
        )
        repository.deleteSession(id)
        assertNull(repository.getSession(id))
        assertTrue(dao.getSegments(id).isEmpty())
        val events = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sound_events").use {
            it.moveToFirst(); it.getInt(0)
        }
        assertEquals(0, events)
    }

    @Test
    fun deleteAllLeavesNothing() = runTest {
        val id = (repository.startSession() as SleepRepository.StartResult.Started).sessionId
        db.sleepDao().insertSegment(MonitoringSegment(sessionId = id, startTime = nowMillis, lastHeartbeat = nowMillis))
        repository.saveSoundEvent(id, nowMillis, nowMillis + 1000, 20f)
        nowMillis += 8 * hour
        repository.endSession()
        repository.startSession()

        repository.deleteAll()

        assertNull(db.sleepDao().getActiveSession())
        assertNull(repository.getSession(id))
        assertTrue(db.sleepDao().getSegments(id).isEmpty())
        val events = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sound_events").use {
            it.moveToFirst(); it.getInt(0)
        }
        assertEquals(0, events)
    }

    @Test
    fun checkInIsSaved() = runTest {
        repository.startSession()
        nowMillis += 8 * hour
        val ended = repository.endSession()!!
        repository.saveCheckIn(ended.id, fallAsleepMinutes = 15, awakeMinutes = null, restedRating = 4)
        val saved = repository.getSession(ended.id)!!
        assertEquals(15, saved.fallAsleepMinutes)
        assertNull(saved.awakeMinutes)
        assertEquals(4, saved.restedRating)
    }
}
