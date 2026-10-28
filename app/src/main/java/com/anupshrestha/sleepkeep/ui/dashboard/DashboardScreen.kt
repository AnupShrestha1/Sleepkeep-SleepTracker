package com.anupshrestha.sleepkeep.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.SessionRules
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.hasCheckIn
import com.anupshrestha.sleepkeep.data.sleepEstimateMillis
import com.anupshrestha.sleepkeep.data.timeInBedMillis
import com.anupshrestha.sleepkeep.ui.common.formatClock
import com.anupshrestha.sleepkeep.ui.common.formatDay
import com.anupshrestha.sleepkeep.ui.common.formatDuration
import com.anupshrestha.sleepkeep.ui.common.is24HourClock
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.loading) {
                val active = state.activeSession
                val lastSleep = state.lastSleep
                when {
                    active != null -> ActiveCard(active, state.now)
                    lastSleep != null -> LastSleepCard(lastSleep)
                    else -> EmptyCard()
                }
                Button(
                    onClick = if (active != null) viewModel::endSleep else viewModel::startSleep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (active != null) R.string.end_sleep else R.string.start_sleep
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }

    when (val p = prompt) {
        is EndPrompt.LongSession -> LongSessionDialog(
            prompt = p,
            onKeep = { viewModel.keepLongSession(p.session.id) },
            onSetWakeTime = { viewModel.setWakeTime(p.session, it) },
        )
        is EndPrompt.CheckIn -> CheckInSheet(
            onSave = { fallAsleep, awake -> viewModel.saveCheckIn(p.sessionId, fallAsleep, awake) },
            onSkip = viewModel::dismissPrompt,
        )
        null -> Unit
    }
}

@Composable
private fun ActiveCard(session: SleepSession, now: Long) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(
                    R.string.sleeping_since, formatClock(session.startTime, session.zoneId)
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(
                    R.string.elapsed_so_far,
                    formatDuration((now - session.startTime).coerceAtLeast(0)),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun LastSleepCard(session: SleepSession) {
    val end = session.endTime ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.last_sleep_label, formatDay(end, session.zoneId)),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.time_in_bed, formatDuration(session.timeInBedMillis() ?: 0)),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = if (session.hasCheckIn) {
                    stringResource(R.string.sleep_estimate, formatDuration(session.sleepEstimateMillis() ?: 0))
                } else {
                    stringResource(R.string.no_check_in)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.bed_and_up,
                    formatClock(session.startTime, session.zoneId),
                    formatClock(end, session.zoneId),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EmptyCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.no_sleep_yet),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LongSessionDialog(
    prompt: EndPrompt.LongSession,
    onKeep: () -> Unit,
    onSetWakeTime: (LocalTime) -> Unit,
) {
    // Starts on the picker again if the last chosen time was rejected.
    var picking by remember { mutableStateOf(prompt.error != null) }
    val pickerState = rememberTimePickerState(initialHour = 7, initialMinute = 0, is24Hour = is24HourClock())
    val length = formatDuration(prompt.session.timeInBedMillis() ?: 0)

    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(stringResource(R.string.long_session_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (picking) {
                    Text(stringResource(R.string.long_session_pick))
                    TimePicker(state = pickerState)
                    prompt.error?.let {
                        Text(
                            text = stringResource(timeErrorText(it)),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Text(stringResource(R.string.long_session_message, length))
                }
            }
        },
        confirmButton = {
            if (picking) {
                TextButton(onClick = { onSetWakeTime(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                    Text(stringResource(R.string.save))
                }
            } else {
                TextButton(onClick = { picking = true }) { Text(stringResource(R.string.set_wake_time)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onKeep) { Text(stringResource(R.string.keep_as_is)) }
        },
    )
}

private fun timeErrorText(error: SessionRules.TimeError): Int = when (error) {
    SessionRules.TimeError.END_BEFORE_START -> R.string.error_end_before_start
    SessionRules.TimeError.IN_FUTURE -> R.string.error_in_future
    SessionRules.TimeError.TOO_SHORT -> R.string.error_too_short
    SessionRules.TimeError.TOO_LONG -> R.string.error_too_long
    SessionRules.TimeError.OVERLAPS -> R.string.error_overlaps
}
