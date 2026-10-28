package com.anupshrestha.sleepkeep.ui.night

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.data.MonitoringSegment
import com.anupshrestha.sleepkeep.data.SessionRules
import com.anupshrestha.sleepkeep.data.SleepRepository
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.SoundEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NightUiState(
    val loading: Boolean = true,
    /** `null` once loaded means the session no longer exists. */
    val session: SleepSession? = null,
    val segments: List<MonitoringSegment> = emptyList(),
    /** Only those inside the session's current times (they can be edited afterwards). */
    val soundEvents: List<SoundEvent> = emptyList(),
)

/** The Edit times dialog is open; [error] is why the last Save was refused. */
data class TimesEdit(val error: SessionRules.TimeError? = null)

class NightDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SleepRepository,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle[NIGHT_ID_ARG])

    val uiState: StateFlow<NightUiState> = combine(
        repository.observeSession(sessionId),
        repository.observeSegments(sessionId),
        repository.observeSoundEvents(sessionId),
    ) { session, segments, events ->
        val end = session?.endTime
        NightUiState(
            loading = false,
            session = session,
            segments = segments,
            soundEvents = if (session == null || end == null) emptyList()
            else events.filter { it.startTime in session.startTime..end },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NightUiState())

    private val _timesEdit = MutableStateFlow<TimesEdit?>(null)
    val timesEdit: StateFlow<TimesEdit?> = _timesEdit.asStateFlow()

    fun openTimesEdit() {
        _timesEdit.value = TimesEdit()
    }

    fun closeTimesEdit() {
        _timesEdit.value = null
    }

    fun saveTimes(start: Long, end: Long) {
        viewModelScope.launch {
            val error = repository.updateTimes(sessionId, start, end)
            _timesEdit.value = error?.let { TimesEdit(it) }
        }
    }

    fun saveCheckIn(fallAsleepMinutes: Int?, awakeMinutes: Int?) {
        viewModelScope.launch { repository.saveCheckIn(sessionId, fallAsleepMinutes, awakeMinutes) }
    }

    fun delete() {
        viewModelScope.launch { repository.deleteSession(sessionId) }
    }

    companion object {
        const val NIGHT_ID_ARG = "id"

        val Factory = viewModelFactory {
            initializer {
                NightDetailViewModel(
                    createSavedStateHandle(),
                    (this[APPLICATION_KEY] as SleepApp).container.repository,
                )
            }
        }
    }
}
