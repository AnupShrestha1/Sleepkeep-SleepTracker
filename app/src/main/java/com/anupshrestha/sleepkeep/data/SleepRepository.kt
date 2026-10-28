package com.anupshrestha.sleepkeep.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
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

    // Step 4 adds: stop monitoring first if this is the active session.
    suspend fun deleteSession(sessionId: Long) = dao.deleteSession(sessionId)
}
