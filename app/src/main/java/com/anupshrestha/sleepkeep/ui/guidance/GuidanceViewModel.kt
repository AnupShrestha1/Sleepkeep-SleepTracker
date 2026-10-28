package com.anupshrestha.sleepkeep.ui.guidance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.data.SleepRepository
import com.anupshrestha.sleepkeep.guidance.SleepStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class GuidanceViewModel(repository: SleepRepository, clock: Clock) : ViewModel() {

    /** `null` while loading. */
    val weekly: StateFlow<SleepStats.Weekly?> = repository.completedSessions
        .map { SleepStats.weekly(it, LocalDate.now(clock.withZone(ZoneId.systemDefault()))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SleepApp).container
                GuidanceViewModel(container.repository, container.clock)
            }
        }
    }
}
