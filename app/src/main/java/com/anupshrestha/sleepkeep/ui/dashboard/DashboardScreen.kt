package com.anupshrestha.sleepkeep.ui.dashboard

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.anupshrestha.sleepkeep.guidance.SleepGuidelines
import com.anupshrestha.sleepkeep.guidance.SleepStats
import com.anupshrestha.sleepkeep.monitor.MonitorStatus
import com.anupshrestha.sleepkeep.ui.common.Hint
import com.anupshrestha.sleepkeep.ui.common.IconBadge
import com.anupshrestha.sleepkeep.ui.common.LEVEL_BARS
import com.anupshrestha.sleepkeep.ui.common.LabeledValue
import com.anupshrestha.sleepkeep.ui.common.LegendItem
import com.anupshrestha.sleepkeep.ui.common.LevelBars
import com.anupshrestha.sleepkeep.ui.common.MicRationaleDialog
import com.anupshrestha.sleepkeep.ui.common.NightCard
import com.anupshrestha.sleepkeep.ui.common.PillButton
import com.anupshrestha.sleepkeep.ui.common.SleepRing
import com.anupshrestha.sleepkeep.ui.common.SoundEventRow
import com.anupshrestha.sleepkeep.ui.common.StatusPill
import com.anupshrestha.sleepkeep.ui.common.WeekBars
import com.anupshrestha.sleepkeep.ui.common.formatClock
import com.anupshrestha.sleepkeep.ui.common.formatDay
import com.anupshrestha.sleepkeep.ui.common.formatDuration
import com.anupshrestha.sleepkeep.ui.common.hasMicPermission
import com.anupshrestha.sleepkeep.ui.common.is24HourClock
import com.anupshrestha.sleepkeep.ui.common.monitoringPermissions
import com.anupshrestha.sleepkeep.ui.common.notificationsEnabled
import com.anupshrestha.sleepkeep.ui.common.openAppSettings
import com.anupshrestha.sleepkeep.ui.common.timeErrorText
import com.anupshrestha.sleepkeep.ui.theme.MoonGold
import com.anupshrestha.sleepkeep.ui.theme.NumberStyle
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** What to do once the microphone question is answered. */
private enum class AfterMicPrompt { START_SESSION, START_MONITORING }

/** Home when awake; the sleeping screen while a session is active. */
@Composable
fun DashboardScreen(
    onOpenNight: (Long) -> Unit,
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

    // A fresh scroll position when switching between Home and the sleeping view.
    val scrollState = remember(state.activeSession?.id) { ScrollState(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.loading) return@Column
        val active = state.activeSession
        if (active != null) {
            SleepingContent(
                session = active,
                state = state,
                liveLevel = viewModel.liveLevel,
                monitoring = {
                    MonitoringStatusCard(
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
                },
                onEnd = viewModel::endSleep,
            )
        } else {
            HomeContent(
                state = state,
                monitorSounds = monitorSounds,
                onMonitorChange = { monitorChoice = it },
                onOpenNight = onOpenNight,
                onStart = {
                    when {
                        !monitorSounds -> viewModel.startSleep(monitor = false, micGranted = micGranted)
                        micGranted -> viewModel.startSleep(monitor = true, micGranted = true)
                        else -> askForMicThen(AfterMicPrompt.START_SESSION)
                    }
                },
            )
        }
        Spacer(Modifier.height(8.dp))
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

// Home

@Composable
private fun HomeContent(
    state: DashboardUiState,
    monitorSounds: Boolean,
    onMonitorChange: (Boolean) -> Unit,
    onOpenNight: (Long) -> Unit,
    onStart: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_moon), contentDescription = null, tint = MoonGold, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
    }
    Column {
        Text(stringResource(greeting(state.now)), style = MaterialTheme.typography.headlineMedium)
        Hint(
            stringResource(if (state.lastSleep != null) R.string.home_subtitle_last else R.string.home_subtitle_none)
        )
    }
    val lastSleep = state.lastSleep
    if (lastSleep != null) LastSleepCard(lastSleep, onView = { onOpenNight(lastSleep.id) })
    state.weekly?.let { WeekCard(it, state.nights) }
    MonitorSoundsSwitch(checked = monitorSounds, onCheckedChange = onMonitorChange)
    PillButton(stringResource(R.string.start_sleep), R.drawable.ic_moon, onStart)
}

private fun greeting(now: Long): Int {
    val hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
    return when (hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..16 -> R.string.greeting_afternoon
        in 17..21 -> R.string.greeting_evening
        else -> R.string.greeting_night
    }
}

@Composable
private fun LastSleepCard(session: SleepSession, onView: () -> Unit) {
    val end = session.endTime ?: return
    val zone = session.zoneId
    val estimate = session.sleepEstimateMillis() ?: 0
    NightCard(onClick = onView) {
        Text(
            stringResource(R.string.last_sleep_label, formatDay(end, zone)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBadge(R.drawable.ic_bed, size = 28.dp)
                    Text(stringResource(R.string.time_in_bed_label), style = MaterialTheme.typography.labelLarge)
                }
                Text(formatDuration(session.timeInBedMillis() ?: 0), style = NumberStyle)
                Hint(
                    if (session.hasCheckIn) stringResource(R.string.sleep_estimate, formatDuration(estimate))
                    else stringResource(R.string.no_check_in_short)
                )
            }
            // How much of the 7-hour guideline this night's sleep estimate covers.
            val fraction = estimate.toFloat() / SleepGuidelines.MIN_ADULT_SLEEP_MILLIS
            SleepRing(progress = fraction, size = 92.dp, stroke = 9.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(fraction * 100).toInt().coerceAtMost(999)}%", style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.ring_of_guideline),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabeledValue(stringResource(R.string.bedtime), formatClock(session.startTime, zone), Modifier.weight(1f), R.drawable.ic_moon)
            LabeledValue(stringResource(R.string.wake_up), formatClock(end, zone), Modifier.weight(1f), R.drawable.ic_sun)
            Icon(
                painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.view_night),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Duration and schedule for the week, kept as separate measurements. */
@Composable
private fun WeekCard(weekly: SleepStats.Weekly, nights: List<SleepStats.NightBar>) {
    NightCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.week_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (weekly is SleepStats.Weekly.Summary) {
                StatusPill(
                    stringResource(if (weekly.meetsGuideline) R.string.pill_meets else R.string.pill_below),
                    if (weekly.meetsGuideline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                )
            }
        }
        when (weekly) {
            is SleepStats.Weekly.Summary -> {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(formatDuration(weekly.averageSleepMillis), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        pluralStringResource(R.plurals.average_over_nights, weekly.nights, weekly.nights),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            is SleepStats.Weekly.NotEnoughData -> Text(
                stringResource(R.string.weekly_not_enough, SleepGuidelines.MIN_NIGHTS, weekly.nights),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        WeekBars(nights, height = 96.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendItem(MaterialTheme.colorScheme.primary, stringResource(R.string.legend_sleep))
            LegendItem(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f), stringResource(R.string.legend_bed_only))
            LegendItem(MaterialTheme.colorScheme.secondary, stringResource(R.string.legend_guideline))
        }
        if (weekly is SleepStats.Weekly.Summary) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(R.drawable.ic_history, size = 32.dp)
                Text(
                    stringResource(
                        R.string.schedule_short,
                        weekly.bedtime.averageDifferenceMinutes,
                        weekly.wakeTime.averageDifferenceMinutes,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Hint(stringResource(R.string.weekly_disclaimer_short))
    }
}

// Sleeping

@Composable
private fun ColumnScope.SleepingContent(
    session: SleepSession,
    state: DashboardUiState,
    liveLevel: StateFlow<Float>,
    monitoring: @Composable () -> Unit,
    onEnd: () -> Unit,
) {
    val zone = session.zoneId
    val elapsed = (state.now - session.startTime).coerceAtLeast(0)
    val status = state.monitorStatus
    val running = status is MonitorStatus.Running && status.sessionId == session.id

    Text(stringResource(R.string.sleeping_title), style = MaterialTheme.typography.headlineSmall)
    SleepRing(
        progress = elapsed.toFloat() / SleepGuidelines.MIN_ADULT_SLEEP_MILLIS,
        size = 250.dp,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ic_moon), contentDescription = null, tint = MoonGold, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.in_bed_for),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(formatDuration(elapsed), style = NumberStyle, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.since_time, formatClock(session.startTime, zone)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (running) {
        // The last few seconds of microphone level, so it's clear monitoring really hears the room.
        val levels = remember { mutableStateListOf<Float>() }
        LaunchedEffect(Unit) {
            liveLevel.collect {
                levels.add(it)
                if (levels.size > LEVEL_BARS) levels.removeAt(0)
            }
        }
        LevelBars(levels)
    }

    monitoring()

    if (session.monitoringRequested) {
        NightCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(R.drawable.ic_sound)
                Text(stringResource(R.string.sounds_so_far), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${state.activeEvents.size}", style = MaterialTheme.typography.titleMedium)
            }
            if (state.activeEvents.isEmpty()) {
                Hint(stringResource(R.string.sounds_none_yet))
            } else {
                state.activeEvents.takeLast(3).asReversed().forEach { SoundEventRow(it, zone) }
            }
        }
    }

    PillButton(stringResource(R.string.end_sleep), R.drawable.ic_stop, onEnd)
}

// Long-session prompt

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
