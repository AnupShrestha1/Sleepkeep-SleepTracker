package com.anupshrestha.sleepkeep.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {

    // Sessions

    @Insert
    suspend fun insertSession(session: SleepSession): Long

    @Update
    suspend fun updateSession(session: SleepSession)

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun getSession(id: Long): SleepSession?

    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(): SleepSession?

    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NULL LIMIT 1")
    fun observeActiveSession(): Flow<SleepSession?>

    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NOT NULL ORDER BY endTime DESC")
    fun observeCompletedSessions(): Flow<List<SleepSession>>

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<SleepSession?>

    /** Also deletes the session's monitoring segments and sound events (foreign-key cascade). */
    @Query("DELETE FROM sleep_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    /** Sessions other than [excludeId] that overlap start..end; an active one counts up to [now]. */
    @Query(
        """
        SELECT COUNT(*) FROM sleep_sessions
        WHERE id != :excludeId
          AND startTime < :end
          AND COALESCE(endTime, :now) > :start
        """
    )
    suspend fun countOverlapping(start: Long, end: Long, excludeId: Long, now: Long): Int

    // Monitoring segments

    @Insert
    suspend fun insertSegment(segment: MonitoringSegment): Long

    @Query("SELECT * FROM monitoring_segments WHERE sessionId = :sessionId ORDER BY startTime")
    suspend fun getSegments(sessionId: Long): List<MonitoringSegment>

    @Query("SELECT * FROM monitoring_segments WHERE sessionId = :sessionId ORDER BY startTime")
    fun observeSegments(sessionId: Long): Flow<List<MonitoringSegment>>

    @Query("UPDATE monitoring_segments SET lastHeartbeat = :time WHERE id = :segmentId")
    suspend fun updateHeartbeat(segmentId: Long, time: Long)

    @Query(
        """
        UPDATE monitoring_segments SET endTime = :endTime, endReason = :reason
        WHERE id = :segmentId AND endTime IS NULL
        """
    )
    suspend fun closeSegment(segmentId: Long, endTime: Long, reason: String)

    /** Open segments from before [before] whose service died; they end at their last heartbeat. */
    @Query(
        """
        UPDATE monitoring_segments SET endTime = lastHeartbeat, endReason = :reason
        WHERE endTime IS NULL AND startTime < :before
        """
    )
    suspend fun closeStaleSegments(before: Long, reason: String)

    @Query("UPDATE sleep_sessions SET monitoringRequested = :requested WHERE id = :sessionId")
    suspend fun setMonitoringRequested(sessionId: Long, requested: Boolean)

    /** Whether the most recent session had monitoring on; `null` before the first session. */
    @Query("SELECT monitoringRequested FROM sleep_sessions ORDER BY startTime DESC LIMIT 1")
    fun observeLastMonitoringChoice(): Flow<Boolean?>

    @Query(
        """
        UPDATE monitoring_segments SET endTime = :endTime, endReason = :reason
        WHERE sessionId = :sessionId AND endTime IS NULL
        """
    )
    suspend fun closeOpenSegments(sessionId: Long, endTime: Long, reason: String)
}
