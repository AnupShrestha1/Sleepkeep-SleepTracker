package com.anupshrestha.sleepkeep.ui.dashboard

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.hasCheckIn
import com.anupshrestha.sleepkeep.data.sleepEstimateMillis
import com.anupshrestha.sleepkeep.data.timeInBedMillis
import com.anupshrestha.sleepkeep.ui.common.MicRationaleDialog
import com.anupshrestha.sleepkeep.ui.common.formatClock
import com.anupshrestha.sleepkeep.ui.common.formatDay
import com.anupshrestha.sleepkeep.ui.common.formatDuration
import com.anupshrestha.sleepkeep.ui.common.hasMicPermission
import com.anupshrestha.sleepkeep.ui.common.is24HourClock
import com.anupshrestha.sleepkeep.ui.common.monitoringPermissions
import com.anupshrestha.sleepkeep.ui.common.notificationsEnabled
import com.anupshrestha.sleepkeep.ui.common.openAppSettings
import com.anupshrestha.sleepkeep.ui.common.timeErrorText
import java.time.LocalTime

/** What to do once the microphone question is answered. */
private enum class AfterMicPrompt { START_SESSION, START_MONITORING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenHistory: () -> Unit,
    onOpenNight: (Long) -> Unit,
    onOpenGuidance: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prompt by viewModel.prompt.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permissions can change in system settings while the app is in the background.
    var permissionCheck by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionCheck++ }
    val micGranted = remember(permissionCheck) { hasMicPermission(context) }
    val notificationsOn = remember(permissionCheck, state.monitorStatus) { notificationsEnabled(context) }

    // null = not changed by the user, so use what the previous session did.
    var monitorChoice by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val monitorSounds = monitorChoice ?: state.lastMonitoringChoice

    var afterMicPrompt by rememberSaveable { mutableStateOf<AfterMicPrompt?>(null) }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionCheck++
        val granted = result[Manifest.permission.RECORD_AUDIO] == true
        when (afterMicPrompt) {
            AfterMicPrompt.START_SESSION -> viewModel.startSleep(monitor = true, micGranted = granted)
            AfterMicPrompt.START_MONITORING -> state.activeSession?.let { if (granted) viewModel.startMonitoring(it.id) }
            null -> Unit
        }
        afterMicPrompt = null
    }

    fun askForMicThen(next: AfterMicPrompt) {
        afterMicPrompt = next
        showRationale = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
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
                    active != null -> ActiveCard(active, state.now) {
                        MonitoringStatusLine(
                            session = active,
                            status = state.monitorStatus,
                            segments = state.segments,
                            micGranted = micGranted,
                            notificationsOn = notificationsOn,
                            onStart = {
                                if (micGranted) viewModel.startMonitoring(active.id)
                                else askForMicThen(AfterMicPrompt.START_MONITORING)
                            },
                            onStop = viewModel::stopMonitoring,
                            onOpenSettings = { openAppSettings(context) },
                        )
                    }
                    lastSleep != null -> LastSleepCard(lastSleep, onView = { onOpenNight(lastSleep.id) })
                    else -> EmptyCard()
                }
                state.weekly?.let { WeeklyCard(it, onDetails = onOpenGuidance) }
                if (active == null) {
                    MonitorSoundsSwitch(checked = monitorSounds, onCheckedChange = { monitorChoice = it })
                }
                Button(
                    onClick = {
                        when {
                            active != null -> viewModel.endSleep()
                            !monitorSounds -> viewModel.startSleep(monitor = false, micGranted = micGranted)
                            micGranted -> viewModel.startSleep(monitor = true, micGranted = true)
                            else -> askForMicThen(AfterMicPrompt.START_SESSION)
                        }
                    },
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
                TextButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.open_history))
                }
            }
        }
    }

    if (showRationale) {
        MicRationaleDialog(
            onContinue = {
                showRationale = false
                permissionLauncher.launch(monitoringPermissions())
            },
            onNotNow = {
                showRationale = false
                // Declining here starts the session without monitoring, as the user chose.
                if (afterMicPrompt == AfterMicPrompt.START_SESSION) {
                    viewModel.startSleep(monitor = false, micGranted = false)
                }
                afterMicPrompt = null
            },
        )
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
private fun ActiveCard(session: SleepSession, now: Long, monitoring: @Composable () -> Unit) {
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
            monitoring()
        }
    }
}

@Composable
private fun LastSleepCard(session: SleepSession, onView: () -> Unit) {
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
            TextButton(onClick = onView, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.view_night))
            }
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
