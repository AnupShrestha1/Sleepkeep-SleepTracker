package com.anupshrestha.sleepkeep.ui.night

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.hasCheckIn
import com.anupshrestha.sleepkeep.data.sleepEstimateMillis
import com.anupshrestha.sleepkeep.data.timeInBedMillis
import com.anupshrestha.sleepkeep.ui.common.BackButton
import com.anupshrestha.sleepkeep.ui.common.formatClock
import com.anupshrestha.sleepkeep.ui.common.formatDay
import com.anupshrestha.sleepkeep.ui.common.formatDuration
import com.anupshrestha.sleepkeep.ui.dashboard.CheckInSheet
import com.anupshrestha.sleepkeep.ui.dashboard.awakeChoices
import com.anupshrestha.sleepkeep.ui.dashboard.choiceLabel
import com.anupshrestha.sleepkeep.ui.dashboard.fallAsleepChoices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NightDetailScreen(
    onBack: () -> Unit,
    viewModel: NightDetailViewModel = viewModel(factory = NightDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timesEdit by viewModel.timesEdit.collectAsStateWithLifecycle()
    var editingCheckIn by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    val session = state.session
    // Deleted (here or elsewhere): nothing left to show.
    LaunchedEffect(state.loading, session) {
        if (!state.loading && session == null) onBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    session?.endTime?.let { Text(formatDay(it, session.zoneId)) }
                },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { innerPadding ->
        if (session?.endTime == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryCard(session)
            CheckInCard(session)
            if (session.monitoringRequested || state.segments.isNotEmpty()) {
                SoundSection(session, state.segments, state.soundEvents)
            }
            OutlinedButton(onClick = viewModel::openTimesEdit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.edit_times))
            }
            OutlinedButton(onClick = { editingCheckIn = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.edit_check_in))
            }
            OutlinedButton(
                onClick = { confirmingDelete = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.delete_session))
            }
        }

        timesEdit?.let { edit ->
            EditTimesDialog(
                session = session,
                error = edit.error,
                onSave = viewModel::saveTimes,
                onDismiss = viewModel::closeTimesEdit,
            )
        }
        if (editingCheckIn) {
            CheckInSheet(
                onSave = { fallAsleep, awake ->
                    viewModel.saveCheckIn(fallAsleep, awake)
                    editingCheckIn = false
                },
                onSkip = { editingCheckIn = false },
                initialFallAsleep = session.fallAsleepMinutes,
                initialAwake = session.awakeMinutes,
            )
        }
        if (confirmingDelete) {
            AlertDialog(
                onDismissRequest = { confirmingDelete = false },
                title = { Text(stringResource(R.string.delete_confirm_title)) },
                text = { Text(stringResource(R.string.delete_confirm_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmingDelete = false
                        viewModel.delete()
                    }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
    }
}

@Composable
private fun SummaryCard(session: SleepSession) {
    val end = session.endTime ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
private fun CheckInCard(session: SleepSession) {
    val notAnswered = stringResource(R.string.not_answered)
    val fallAsleep = choiceLabel(fallAsleepChoices, session.fallAsleepMinutes)?.let { stringResource(it) } ?: notAnswered
    val awake = choiceLabel(awakeChoices, session.awakeMinutes)?.let { stringResource(it) } ?: notAnswered
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.check_in_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.detail_fall_asleep, fallAsleep), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.detail_awake, awake), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
