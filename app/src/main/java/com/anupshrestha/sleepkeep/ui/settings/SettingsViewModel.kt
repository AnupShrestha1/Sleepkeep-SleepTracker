package com.anupshrestha.sleepkeep.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.data.EndReason
import com.anupshrestha.sleepkeep.data.SleepRepository
import com.anupshrestha.sleepkeep.monitor.MonitorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SleepRepository,
    private val monitor: MonitorController,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val _deleted = MutableStateFlow(false)
    /** True once Delete all data has finished, to confirm it on screen. */
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun deleteAll() {
        // Stop the microphone first so nothing new is written after the wipe.
        monitor.stop(EndReason.USER_STOPPED)
        // App scope: the wipe finishes even if the user leaves the screen straight away.
        appScope.launch {
            repository.deleteAll()
            _deleted.value = true
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SleepApp).container
                SettingsViewModel(container.repository, container.monitor, container.appScope)
            }
        }
    }
}
