package com.anupshrestha.sleepkeep.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.EndReason
import com.anupshrestha.sleepkeep.data.MonitoringSegment
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.monitor.MonitorStatus
import com.anupshrestha.sleepkeep.ui.common.formatClock

@Composable
fun MonitorSoundsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    // The whole row toggles, not just the small switch.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.monitor_sounds), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.monitor_sounds_hint), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * One line saying exactly what the microphone is doing for the active session, with the one
 * action that makes sense. Never implies monitoring is on when it isn't.
 */
@Composable
fun MonitoringStatusLine(
    session: SleepSession,
    status: MonitorStatus,
    segments: List<MonitoringSegment>,
    micGranted: Boolean,
    notificationsOn: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val zone = session.zoneId
    val running = status is MonitorStatus.Running && status.sessionId == session.id
    val silencedSince = if (running) status.silencedSince else null
    val last = segments.lastOrNull()
    // An open segment while nothing runs belongs to a killed service; it's closed at its heartbeat.
    val lastStop = last?.let { it.endTime ?: it.lastHeartbeat }

    val (text, action) = when {
        silencedSince != null ->
            stringResource(R.string.monitor_silenced, formatClock(silencedSince, zone)) to
                (R.string.stop_monitoring to onStop)
        running ->
            stringResource(R.string.monitor_listening) to (R.string.stop_monitoring to onStop)
        !session.monitoringRequested ->
            stringResource(R.string.monitor_off) to (R.string.start_monitoring to onStart)
        !micGranted ->
            stringResource(R.string.monitor_mic_denied) to (R.string.open_settings to onOpenSettings)
        last == null || lastStop == null ->
            stringResource(R.string.monitor_not_running) to (R.string.resume to onStart)
        last.endReason == EndReason.USER_STOPPED ->
            stringResource(R.string.monitor_stopped_by_you, formatClock(lastStop, zone)) to (R.string.resume to onStart)
        last.endReason == EndReason.MIC_ERROR ->
            stringResource(R.string.monitor_mic_error, formatClock(lastStop, zone)) to (R.string.resume to onStart)
        else ->
            stringResource(R.string.monitor_stopped_unexpectedly, formatClock(lastStop, zone)) to (R.string.resume to onStart)
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
        if (running && !notificationsOn) {
            Text(stringResource(R.string.monitor_notifications_off), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = action.second) { Text(stringResource(action.first)) }
    }
}
