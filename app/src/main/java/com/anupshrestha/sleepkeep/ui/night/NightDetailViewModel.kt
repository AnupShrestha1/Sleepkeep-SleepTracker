package com.anupshrestha.sleepkeep.ui.night

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.data.SessionRules
import com.anupshrestha.sleepkeep.data.SleepRepository
import com.anupshrestha.sleepkeep.data.SleepSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NightUiState(
    val loading: Boolean = true,
    /** `null` once loaded means the session no longer exists. */
    val session: SleepSession? = null,
)

/** The Edit times dialog is open; [error] is why the last Save was refused. */
data class TimesEdit(val error: SessionRules.TimeError? = null)

class NightDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SleepRepository,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle[NIGHT_ID_ARG])

    val uiState: StateFlow<NightUiState> = repository.observeSession(sessionId)
        .map { NightUiState(loading = false, session = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NightUiState())

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
