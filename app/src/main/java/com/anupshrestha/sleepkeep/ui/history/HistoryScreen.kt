package com.anupshrestha.sleepkeep.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.sleepEstimateMillis
import com.anupshrestha.sleepkeep.data.timeInBedMillis
import com.anupshrestha.sleepkeep.guidance.SleepGuidelines
import com.anupshrestha.sleepkeep.ui.common.Hint
import com.anupshrestha.sleepkeep.ui.common.IconBadge
import com.anupshrestha.sleepkeep.ui.common.NightCard
import com.anupshrestha.sleepkeep.ui.common.ScreenHeader
import com.anupshrestha.sleepkeep.ui.common.StatusPill
import com.anupshrestha.sleepkeep.ui.common.formatClock
import com.anupshrestha.sleepkeep.ui.common.formatDay
import com.anupshrestha.sleepkeep.ui.common.formatDuration

@Composable
fun HistoryScreen(
    onOpenNight: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") { ScreenHeader(stringResource(R.string.history_title), R.drawable.ic_history) }
        if (state.loading) return@LazyColumn
        if (state.activeSession == null && state.sessions.isEmpty()) {
            item(key = "empty") { Hint(stringResource(R.string.history_empty)) }
        }
        state.activeSession?.let { active ->
            item(key = "active") {
                // Not tappable: an active session is managed from Home.
                NightCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconBadge(R.drawable.ic_moon, tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text(stringResource(R.string.history_in_progress), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.history_since, formatClock(active.startTime, active.zoneId)),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
        items(state.sessions, key = { it.id }) { session ->
            SessionCard(session, isNap = session.id in state.napIds, onClick = { onOpenNight(session.id) })
        }
    }
}

@Composable
private fun SessionCard(session: SleepSession, isNap: Boolean, onClick: () -> Unit) {
    val end = session.endTime ?: return
    val zone = session.zoneId
    NightCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    formatDay(end, zone),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatDuration(session.timeInBedMillis() ?: 0), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.history_times, formatClock(session.startTime, zone), formatClock(end, zone)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // A factual marker against the guideline, not a grade.
            when {
                isNap -> StatusPill(stringResource(R.string.nap), MaterialTheme.colorScheme.onSurfaceVariant)
                (session.sleepEstimateMillis() ?: 0) >= SleepGuidelines.MIN_ADULT_SLEEP_MILLIS ->
                    StatusPill(stringResource(R.string.pill_seven_plus), MaterialTheme.colorScheme.tertiary)
            }
            Icon(
                painterResource(R.drawable.ic_chevron_right), contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
