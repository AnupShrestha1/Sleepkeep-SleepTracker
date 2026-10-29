package com.anupshrestha.sleepkeep.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.EndReason
import com.anupshrestha.sleepkeep.data.MonitoringSegment
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.monitor.MonitorStatus
import com.anupshrestha.sleepkeep.ui.common.Hint
import com.anupshrestha.sleepkeep.ui.common.IconBadge
import com.anupshrestha.sleepkeep.ui.common.NightCard
import com.anupshrestha.sleepkeep.ui.common.StatusPill
import com.anupshrestha.sleepkeep.ui.common.formatClock

@Composable
fun MonitorSoundsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    NightCard {
        // The whole row toggles, not just the small switch.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconBadge(R.drawable.ic_mic)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.monitor_sounds), style = MaterialTheme.typography.titleMedium)
                Hint(stringResource(R.string.monitor_sounds_hint))
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

/**
 * Exactly what the microphone is doing for the active session, with the one action that makes
 * sense. Never implies monitoring is on when it isn't.
 */
@Composable
fun MonitoringStatusCard(
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
    val pausedForSounds = running && status.pausedForSounds
    val last = segments.lastOrNull()
    // An open segment while nothing runs belongs to a killed service; it's closed at its heartbeat.
    val lastStop = last?.let { it.endTime ?: it.lastHeartbeat }

    val good = MaterialTheme.colorScheme.tertiary
    val warn = MaterialTheme.colorScheme.secondary
    val off = MaterialTheme.colorScheme.onSurfaceVariant

    data class Line(val pill: Int, val color: Color, val text: String, val action: Int, val onAction: () -> Unit)

    val line = when {
        pausedForSounds -> Line(
            R.string.pill_paused, MaterialTheme.colorScheme.primary, stringResource(R.string.monitor_paused_for_sounds),
            R.string.stop_monitoring, onStop,
        )
        silencedSince != null -> Line(
            R.string.pill_silenced, warn,
            stringResource(R.string.monitor_silenced, formatClock(silencedSince, zone)),
            R.string.stop_monitoring, onStop,
        )
        running -> Line(
            R.string.pill_listening, good, stringResource(R.string.monitor_listening),
            R.string.stop_monitoring, onStop,
        )
        !session.monitoringRequested -> Line(
            R.string.pill_off, off, stringResource(R.string.monitor_off), R.string.start_monitoring, onStart,
        )
        !micGranted -> Line(
            R.string.pill_off, warn, stringResource(R.string.monitor_mic_denied), R.string.open_settings, onOpenSettings,
        )
        last == null || lastStop == null -> Line(
            R.string.pill_stopped, warn, stringResource(R.string.monitor_not_running), R.string.resume, onStart,
        )
        last.endReason == EndReason.USER_STOPPED -> Line(
            R.string.pill_stopped, off,
            stringResource(R.string.monitor_stopped_by_you, formatClock(lastStop, zone)), R.string.resume, onStart,
        )
        last.endReason == EndReason.MIC_ERROR -> Line(
            R.string.pill_stopped, warn,
            stringResource(R.string.monitor_mic_error, formatClock(lastStop, zone)), R.string.resume, onStart,
        )
        else -> Line(
            R.string.pill_stopped, warn,
            stringResource(R.string.monitor_stopped_unexpectedly, formatClock(lastStop, zone)), R.string.resume, onStart,
        )
    }

    NightCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(R.drawable.ic_mic, tint = line.color)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusPill(stringResource(line.pill), line.color)
                Text(line.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (running && !notificationsOn) Hint(stringResource(R.string.monitor_notifications_off))
        TextButton(onClick = line.onAction, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(line.action))
        }
    }
}
