package com.anupshrestha.sleepkeep.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.data.SleepRepository
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.guidance.SleepStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val loading: Boolean = true,
    val activeSession: SleepSession? = null,
    /** Newest first. */
    val sessions: List<SleepSession> = emptyList(),
    val napIds: Set<Long> = emptySet(),
)

class HistoryViewModel(repository: SleepRepository) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.activeSession,
        repository.completedSessions,
    ) { active, completed ->
        HistoryUiState(
            loading = false,
            activeSession = active,
            sessions = completed,
            napIds = SleepStats.napIds(completed),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HistoryViewModel((this[APPLICATION_KEY] as SleepApp).container.repository)
            }
        }
    }
}
