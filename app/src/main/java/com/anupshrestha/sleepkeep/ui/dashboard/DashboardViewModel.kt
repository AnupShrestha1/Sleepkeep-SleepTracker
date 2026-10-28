package com.anupshrestha.sleepkeep.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.data.SessionRules
import com.anupshrestha.sleepkeep.data.SleepRepository
import com.anupshrestha.sleepkeep.data.SleepSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId

data class DashboardUiState(
    val loading: Boolean = true,
    val activeSession: SleepSession? = null,
    /** The most recently ended session. */
    val lastSleep: SleepSession? = null,
    val now: Long = 0,
)

/** What the dashboard asks right after End sleep. */
sealed interface EndPrompt {
    data class LongSession(
        val session: SleepSession,
        val error: SessionRules.TimeError? = null,
    ) : EndPrompt

    data class CheckIn(val sessionId: Long) : EndPrompt
}

class DashboardViewModel(
    private val repository: SleepRepository,
    private val clock: Clock,
) : ViewModel() {

    // Ticks on each minute boundary so the elapsed time stays current.
    private val minuteTicker = flow {
        while (true) {
            emit(clock.millis())
            delay(60_000 - clock.millis() % 60_000)
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.activeSession,
        repository.completedSessions,
        minuteTicker,
    ) { active, completed, now ->
        DashboardUiState(
            loading = false,
            activeSession = active,
            lastSleep = completed.firstOrNull(),
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val _prompt = MutableStateFlow<EndPrompt?>(null)
    val prompt: StateFlow<EndPrompt?> = _prompt.asStateFlow()

    fun startSleep() {
        // A second tap while the first is saving gets AlreadyActive, which needs no message:
        // the button turns into End sleep as soon as the session exists.
        viewModelScope.launch { repository.startSession() }
    }

    fun endSleep() {
        viewModelScope.launch {
            val ended = repository.endSession() ?: return@launch
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

    fun saveCheckIn(sessionId: Long, fallAsleepMinutes: Int?, awakeMinutes: Int?) {
        _prompt.value = null
        viewModelScope.launch { repository.saveCheckIn(sessionId, fallAsleepMinutes, awakeMinutes) }
    }

    fun dismissPrompt() {
        _prompt.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SleepApp).container
                DashboardViewModel(container.repository, container.clock)
            }
        }
    }
}
