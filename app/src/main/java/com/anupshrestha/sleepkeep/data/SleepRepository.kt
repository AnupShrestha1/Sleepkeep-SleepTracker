package com.anupshrestha.sleepkeep.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.ZoneId

/** The only place that writes sleep data, so every rule is enforced in one spot. */
class SleepRepository(
    private val db: SleepDatabase,
    private val clock: Clock,
    private val currentZone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val dao = db.sleepDao()

    val activeSession: Flow<SleepSession?> = dao.observeActiveSession()
    val completedSessions: Flow<List<SleepSession>> = dao.observeCompletedSessions()
    val hasSessions: Flow<Boolean> = dao.observeHasSessions()

    sealed interface StartResult {
        data class Started(val sessionId: Long) : StartResult
        data object AlreadyActive : StartResult
    }

    /** Starts a session unless one is already active; checked and inserted in one transaction. */
    suspend fun startSession(monitoringRequested: Boolean = false): StartResult =
        db.withTransaction {
            if (dao.getActiveSession() != null) {
                StartResult.AlreadyActive
            } else {
                val id = dao.insertSession(
                    SleepSession(
                        startTime = clock.millis(),
                        zoneId = currentZone().id,
                        monitoringRequested = monitoringRequested,
                    )
                )
                StartResult.Started(id)
            }
        }

    /** Ends the active session now and closes any open monitoring segment. */
    suspend fun endSession(): SleepSession? = db.withTransaction {
        val active = dao.getActiveSession() ?: return@withTransaction null
        val now = clock.millis()
        val ended = active.copy(endTime = now)
        dao.updateSession(ended)
        dao.closeOpenSegments(active.id, now, EndReason.SESSION_ENDED)
        ended
    }

    /** Changes a completed session's times; returns why not if the times break a rule. */
    suspend fun updateTimes(sessionId: Long, start: Long, end: Long): SessionRules.TimeError? =
        db.withTransaction {
            val session = dao.getSession(sessionId) ?: return@withTransaction null
            val now = clock.millis()
            SessionRules.validate(start, end, now)?.let { return@withTransaction it }
            if (dao.countOverlapping(start, end, sessionId, now) > 0) {
                return@withTransaction SessionRules.TimeError.OVERLAPS
            }
            dao.updateSession(session.copy(startTime = start, endTime = end))
            null
        }

    suspend fun saveCheckIn(sessionId: Long, fallAsleepMinutes: Int?, awakeMinutes: Int?) {
        val session = dao.getSession(sessionId) ?: return
        dao.updateSession(
            session.copy(fallAsleepMinutes = fallAsleepMinutes, awakeMinutes = awakeMinutes)
        )
    }

    suspend fun getSession(sessionId: Long): SleepSession? = dao.getSession(sessionId)

    fun observeSession(sessionId: Long): Flow<SleepSession?> = dao.observeSession(sessionId)

    // Only completed sessions can be deleted from the UI, so monitoring is never running here.
    suspend fun deleteSession(sessionId: Long) = dao.deleteSession(sessionId)

    /** Removes every session, segment and sound event. The caller stops monitoring first. */
    suspend fun deleteAll() = withContext(Dispatchers.IO) { db.clearAllTables() }

    // Monitoring

    val lastMonitoringChoice: Flow<Boolean?> = dao.observeLastMonitoringChoice()

    fun observeSegments(sessionId: Long): Flow<List<MonitoringSegment>> = dao.observeSegments(sessionId)

    suspend fun setMonitoringRequested(sessionId: Long, requested: Boolean) =
        dao.setMonitoringRequested(sessionId, requested)

    suspend fun startSegment(sessionId: Long): Long {
        val now = clock.millis()
        return dao.insertSegment(MonitoringSegment(sessionId = sessionId, startTime = now, lastHeartbeat = now))
    }

    suspend fun heartbeat(segmentId: Long) = dao.updateHeartbeat(segmentId, clock.millis())

    suspend fun closeSegment(segmentId: Long, endTime: Long, reason: String) =
        dao.closeSegment(segmentId, endTime, reason)

    fun observeSoundEvents(sessionId: Long): Flow<List<SoundEvent>> = dao.observeSoundEvents(sessionId)

    suspend fun saveSoundEvent(sessionId: Long, startTime: Long, endTime: Long, peakAboveBackgroundDb: Float) {
        dao.insertSoundEvent(
            SoundEvent(
                sessionId = sessionId,
                startTime = startTime,
                endTime = endTime,
                peakAboveBackgroundDb = peakAboveBackgroundDb,
            )
        )
    }

    /**
     * Called when the app process starts. Nothing can be monitoring yet, so any open segment
     * belongs to a service that was killed; it ends at its last heartbeat.
     */
    suspend fun closeStaleSegments(processStart: Long) =
        dao.closeStaleSegments(processStart, EndReason.STOPPED_UNEXPECTEDLY)
}
