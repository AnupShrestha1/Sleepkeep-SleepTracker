package com.anupshrestha.sleepkeep.ui.night

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.MonitoringSegment
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.SoundEvent
import com.anupshrestha.sleepkeep.monitor.Coverage
import com.anupshrestha.sleepkeep.ui.common.Hint
import com.anupshrestha.sleepkeep.ui.common.IconBadge
import com.anupshrestha.sleepkeep.ui.common.LegendItem
import com.anupshrestha.sleepkeep.ui.common.NightCard
import com.anupshrestha.sleepkeep.ui.common.NightTimeline
import com.anupshrestha.sleepkeep.ui.common.SoundEventRow
import com.anupshrestha.sleepkeep.ui.common.formatClock
import com.anupshrestha.sleepkeep.ui.common.formatDuration

/** Events beyond this are behind a "Show all" button, so a noisy night stays readable. */
private const val EVENTS_SHOWN = 20

/** Timeline, coverage, gaps and loud sounds for one night. Shown only if monitoring was on. */
@Composable
fun SoundSection(session: SleepSession, segments: List<MonitoringSegment>, events: List<SoundEvent>) {
    val end = session.endTime ?: return
    val zone = session.zoneId
    val coverage = Coverage.of(session.startTime, end, segments)
    var showAll by rememberSaveable { mutableStateOf(false) }

    NightCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(R.drawable.ic_sound)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.sound_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (events.isEmpty()) stringResource(R.string.sound_none)
                    else pluralStringResource(R.plurals.sound_count, events.size, events.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        NightTimeline(session.startTime, end, segments, events)
        Row(Modifier.fillMaxWidth()) {
            Hint(formatClock(session.startTime, zone), Modifier.weight(1f))
            Hint(formatClock(end, zone))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendItem(MaterialTheme.colorScheme.primary, stringResource(R.string.legend_monitored))
            LegendItem(MaterialTheme.colorScheme.outline, stringResource(R.string.legend_not_monitored))
            LegendItem(MaterialTheme.colorScheme.secondary, stringResource(R.string.legend_loud))
        }
        Text(
            stringResource(
                R.string.sound_coverage,
                formatDuration(coverage.monitoredMillis),
                formatDuration(end - session.startTime),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        coverage.gaps.forEach { gap ->
            Hint(
                stringResource(
                    R.string.sound_gap,
                    formatClock(gap.start, zone),
                    formatClock(gap.end, zone),
                    stringResource(gapReasonText(gap.reason)),
                )
            )
        }

        if (events.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val shown = if (showAll) events else events.take(EVENTS_SHOWN)
            shown.forEach { SoundEventRow(it, zone) }
            if (!showAll && events.size > EVENTS_SHOWN) {
                TextButton(onClick = { showAll = true }) {
                    Text(stringResource(R.string.sound_show_all, events.size))
                }
            }
        }
        Hint(stringResource(R.string.sound_note))
    }
}

@StringRes
private fun gapReasonText(reason: Coverage.GapReason): Int = when (reason) {
    Coverage.GapReason.NOT_RUNNING -> R.string.gap_not_running
    Coverage.GapReason.USER_STOPPED -> R.string.gap_user_stopped
    Coverage.GapReason.MIC_SILENCED -> R.string.gap_mic_silenced
    Coverage.GapReason.MIC_ERROR -> R.string.gap_mic_error
    Coverage.GapReason.STOPPED_UNEXPECTEDLY -> R.string.gap_stopped_unexpectedly
    Coverage.GapReason.SOUNDS_PLAYING -> R.string.gap_sounds_playing
}
