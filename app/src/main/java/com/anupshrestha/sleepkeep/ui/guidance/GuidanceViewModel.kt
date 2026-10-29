package com.anupshrestha.sleepkeep.ui.guidance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.data.SleepRepository
import com.anupshrestha.sleepkeep.guidance.SleepStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** The periods Insights can show. */
enum class InsightsRange(val days: Int) { WEEK(7), MONTH(30), QUARTER(90) }

data class InsightsState(
    val range: InsightsRange,
    val weekly: SleepStats.Weekly,
    val nights: List<SleepStats.NightBar>,
    val rolling: List<Long?>,
    val weekdayWeekend: SleepStats.WeekdayWeekend,
    /** `null` with fewer than 3 rated nights. */
    val rested: SleepStats.Rested?,
)

class GuidanceViewModel(repository: SleepRepository, clock: Clock) : ViewModel() {

    private val range = MutableStateFlow(InsightsRange.WEEK)

    /** `null` while loading. */
    val state: StateFlow<InsightsState?> = combine(repository.completedSessions, range) { sessions, range ->
        val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
        val nights = SleepStats.lastNights(sessions, today, range.days)
        InsightsState(
            range = range,
            weekly = SleepStats.weekly(sessions, today, range.days),
            nights = nights,
            rolling = SleepStats.rollingAverage(nights),
            weekdayWeekend = SleepStats.weekdayWeekend(sessions, today, range.days),
            rested = SleepStats.rested(sessions, today, range.days),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRange(newRange: InsightsRange) {
        range.value = newRange
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as SleepApp).container
                GuidanceViewModel(container.repository, container.clock)
            }
        }
    }
}
