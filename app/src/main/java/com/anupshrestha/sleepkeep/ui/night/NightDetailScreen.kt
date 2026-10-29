package com.anupshrestha.sleepkeep.ui.night

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import com.anupshrestha.sleepkeep.ui.common.Hint
import com.anupshrestha.sleepkeep.ui.common.IconBadge
import com.anupshrestha.sleepkeep.ui.common.NightCard
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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.night_title))
                        session?.endTime?.let {
                            Text(
                                formatDay(it, session.zoneId),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        if (session?.endTime == null) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryCard(session)
            if (session.monitoringRequested || state.segments.isNotEmpty()) {
                SoundSection(session, state.segments, state.soundEvents)
            }
            CheckInCard(session)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(R.drawable.ic_edit, R.string.edit_times, viewModel::openTimesEdit, Modifier.weight(1f))
                ActionButton(R.drawable.ic_check_circle, R.string.edit_check_in, { editingCheckIn = true }, Modifier.weight(1f))
            }
            ActionButton(
                R.drawable.ic_delete, R.string.delete_session, { confirmingDelete = true },
                Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
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
    val zone = session.zoneId
    NightCard {
        Row(Modifier.fillMaxWidth()) {
            Stat(formatDuration(session.timeInBedMillis() ?: 0), stringResource(R.string.time_in_bed_label), Modifier.weight(1.2f))
            Stat(formatClock(session.startTime, zone), stringResource(R.string.bedtime), Modifier.weight(1f))
            Stat(formatClock(end, zone), stringResource(R.string.wake_up), Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(R.drawable.ic_moon, tint = MaterialTheme.colorScheme.secondary, size = 32.dp)
            Text(
                if (session.hasCheckIn) {
                    stringResource(R.string.sleep_estimate, formatDuration(session.sleepEstimateMillis() ?: 0))
                } else {
                    stringResource(R.string.no_check_in)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier, muted: Boolean = false) {
    Column(modifier) {
        if (muted) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
        Hint(label)
    }
}

@Composable
private fun CheckInCard(session: SleepSession) {
    val notAnswered = stringResource(R.string.not_answered)
    val fallAsleep = choiceLabel(fallAsleepChoices, session.fallAsleepMinutes)?.let { stringResource(it) } ?: notAnswered
    val awake = choiceLabel(awakeChoices, session.awakeMinutes)?.let { stringResource(it) } ?: notAnswered
    NightCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(R.drawable.ic_check_circle)
            Text(stringResource(R.string.check_in_title), style = MaterialTheme.typography.titleMedium)
        }
        Row(Modifier.fillMaxWidth()) {
            Stat(fallAsleep, stringResource(R.string.detail_fall_asleep_label), Modifier.weight(1f), muted = session.fallAsleepMinutes == null)
            Stat(awake, stringResource(R.string.detail_awake_label), Modifier.weight(1f), muted = session.awakeMinutes == null)
        }
    }
}

@Composable
private fun ActionButton(
    @DrawableRes icon: Int,
    text: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = modifier.height(52.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(text))
    }
}
