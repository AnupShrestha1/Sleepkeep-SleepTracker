package com.anupshrestha.sleepkeep.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.timeInBedMillis
import com.anupshrestha.sleepkeep.ui.common.BackButton
import com.anupshrestha.sleepkeep.ui.common.formatClock
import com.anupshrestha.sleepkeep.ui.common.formatDay
import com.anupshrestha.sleepkeep.ui.common.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenNight: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { innerPadding ->
        if (state.loading) return@Scaffold
        if (state.activeSession == null && state.sessions.isEmpty()) {
            Text(
                text = stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            state.activeSession?.let { active ->
                item(key = "active") {
                    // Not tappable: an active session is managed from the dashboard.
                    HistoryRow(
                        title = stringResource(R.string.history_in_progress),
                        times = stringResource(R.string.history_since, formatClock(active.startTime, active.zoneId)),
                        duration = null,
                        isNap = false,
                        onClick = null,
                    )
                    HorizontalDivider()
                }
            }
            items(state.sessions, key = { it.id }) { session ->
                SessionRow(session, isNap = session.id in state.napIds, onClick = { onOpenNight(session.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SessionRow(session: SleepSession, isNap: Boolean, onClick: () -> Unit) {
    val end = session.endTime ?: return
    HistoryRow(
        title = formatDay(end, session.zoneId),
        times = stringResource(
            R.string.history_times,
            formatClock(session.startTime, session.zoneId),
            formatClock(end, session.zoneId),
        ),
        duration = formatDuration(session.timeInBedMillis() ?: 0),
        isNap = isNap,
        onClick = onClick,
    )
}

@Composable
private fun HistoryRow(
    title: String,
    times: String,
    duration: String?,
    isNap: Boolean,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(times, style = MaterialTheme.typography.bodyMedium)
        }
        if (isNap) {
            AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.nap)) })
        }
        duration?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
    }
}
