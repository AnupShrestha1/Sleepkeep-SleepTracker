package com.anupshrestha.sleepkeep.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.data.EndReason
import com.anupshrestha.sleepkeep.data.MonitoringSegment
import com.anupshrestha.sleepkeep.data.SessionRules
import com.anupshrestha.sleepkeep.data.SleepRepository
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.SoundEvent
import com.anupshrestha.sleepkeep.guidance.SleepStats
import com.anupshrestha.sleepkeep.monitor.MonitorController
import com.anupshrestha.sleepkeep.monitor.MonitorStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class DashboardUiState(
    val loading: Boolean = true,
    val activeSession: SleepSession? = null,
    /** The most recently ended session. */
    val lastSleep: SleepSession? = null,
    val now: Long = 0,
    val monitorStatus: MonitorStatus = MonitorStatus.NotRunning,
    /** The active session's monitoring segments, oldest first. */
    val segments: List<MonitoringSegment> = emptyList(),
    /** Default for the "Monitor sounds" switch: what the previous session used. */
    val lastMonitoringChoice: Boolean = false,
    val weekly: SleepStats.Weekly? = null,
    /** The last 7 days for the mini chart, oldest first. */
    val nights: List<SleepStats.NightBar> = emptyList(),
    /** Loud sounds so far in the active session, oldest first. */
    val activeEvents: List<SoundEvent> = emptyList(),
)

private data class Sessions(
    val active: SleepSession?,
    val lastSleep: SleepSession?,
    val now: Long,
    val weekly: SleepStats.Weekly,
    val nights: List<SleepStats.NightBar>,
)

/** What the dashboard asks right after End sleep. */
sealed interface EndPrompt {
    data class LongSession(
        val session: SleepSession,
        val error: SessionRules.TimeError? = null,
    ) : EndPrompt

    data class CheckIn(val sessionId: Long) : EndPrompt
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: SleepRepository,
    private val monitor: MonitorController,
    private val clock: Clock,
) : ViewModel() {

    // Ticks on each minute boundary so the elapsed time stays current.
    private val minuteTicker = flow {
        while (true) {
            emit(clock.millis())
            delay(60_000 - clock.millis() % 60_000)
        }
    }

    private val activeSegments = repository.activeSession.flatMapLatest { active ->
        if (active == null) flowOf(emptyList()) else repository.observeSegments(active.id)
    }

    private val activeEvents = repository.activeSession.flatMapLatest { active ->
        if (active == null) flowOf(emptyList()) else repository.observeSoundEvents(active.id)
    }

    private val sessions = combine(
        repository.activeSession,
        repository.completedSessions,
        minuteTicker,
    ) { active, completed, now ->
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        Sessions(
            active, completed.firstOrNull(), now,
            SleepStats.weekly(completed, today), SleepStats.lastSevenNights(completed, today),
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        sessions,
        monitor.status,
        activeSegments,
        repository.lastMonitoringChoice,
        activeEvents,
    ) { sessions, status, segments, lastChoice, events ->
        DashboardUiState(
            loading = false,
            activeSession = sessions.active,
            lastSleep = sessions.lastSleep,
            now = sessions.now,
            monitorStatus = status,
            segments = segments,
            lastMonitoringChoice = lastChoice ?: false,
            weekly = sessions.weekly,
            nights = sessions.nights,
            activeEvents = events,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** The microphone's live level, for the meter on the sleeping screen. */
    val liveLevel: StateFlow<Float> = monitor.level

    private val _prompt = MutableStateFlow<EndPrompt?>(null)
    val prompt: StateFlow<EndPrompt?> = _prompt.asStateFlow()

    /**
     * [monitor] is the user's choice; [micGranted] whether monitoring can actually start. A
     * session that wanted monitoring but has no microphone still starts, and says so.
     */
    fun startSleep(monitor: Boolean, micGranted: Boolean) {
        viewModelScope.launch {
            // A second tap while the first is saving gets AlreadyActive, which needs no message:
            // the button turns into End sleep as soon as the session exists.
            val result = repository.startSession(monitoringRequested = monitor)
            if (result is SleepRepository.StartResult.Started && monitor && micGranted) {
                this@DashboardViewModel.monitor.start(result.sessionId)
            }
        }
    }

    /** Turns monitoring on mid-session, or resumes it after it stopped. */
    fun startMonitoring(sessionId: Long) {
        viewModelScope.launch {
            repository.setMonitoringRequested(sessionId, true)
            monitor.start(sessionId)
        }
    }

    fun stopMonitoring() = monitor.stop(EndReason.USER_STOPPED)

    fun endSleep() {
        viewModelScope.launch {
            val ended = repository.endSession() ?: return@launch
            monitor.stop(EndReason.SESSION_ENDED)
            val end = ended.endTime ?: return@launch
            _prompt.value = if (SessionRules.isSuspiciouslyLong(ended.startTime, end)) {
                EndPrompt.LongSession(ended)
            } else {
                EndPrompt.CheckIn(ended.id)
            }
        }
    }

    fun keepLongSession(sessionId: Long) {
        _prompt.value = EndPrompt.CheckIn(sessionId)
    }

    fun setWakeTime(session: SleepSession, wakeTime: LocalTime) {
        viewModelScope.launch {
            val end = SessionRules.nextOccurrenceAfter(
                session.startTime, wakeTime, ZoneId.of(session.zoneId)
            )
            val error = repository.updateTimes(session.id, session.startTime, end)
            _prompt.value = if (error == null) {
                EndPrompt.CheckIn(session.id)
            } else {
                EndPrompt.LongSession(session, error)
            }
        }
    }

    fun saveCheckIn(sessionId: Long, fallAsleepMinutes: Int?, awakeMinutes: Int?, restedRating: Int?) {
        _prompt.value = null
        viewModelScope.launch { repository.saveCheckIn(sessionId, fallAsleepMinutes, awakeMinutes, restedRating) }
    }

    fun dismissPrompt() {
        _prompt.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SleepApp).container
                DashboardViewModel(container.repository, container.monitor, container.clock)
            }
        }
    }
}
