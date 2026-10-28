package com.anupshrestha.sleepkeep.ui.night

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.MonitoringSegment
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.data.SoundEvent
import com.anupshrestha.sleepkeep.monitor.Coverage
import com.anupshrestha.sleepkeep.ui.common.formatClock
import com.anupshrestha.sleepkeep.ui.common.formatDuration

/** Events beyond this are behind a "Show all" button, so a noisy night stays readable. */
private const val EVENTS_SHOWN = 20

/** Coverage, gaps and loud sounds for one night. Shown only if monitoring was on. */
@Composable
fun SoundSection(session: SleepSession, segments: List<MonitoringSegment>, events: List<SoundEvent>) {
    val end = session.endTime ?: return
    val zone = session.zoneId
    val coverage = Coverage.of(session.startTime, end, segments)
    var showAll by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.sound_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.sound_coverage,
                    formatDuration(coverage.monitoredMillis),
                    formatDuration(end - session.startTime),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            coverage.gaps.forEach { gap ->
                Text(
                    stringResource(
                        R.string.sound_gap,
                        formatClock(gap.start, zone),
                        formatClock(gap.end, zone),
                        stringResource(gapReasonText(gap.reason)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            HorizontalDivider()

            Text(
                if (events.isEmpty()) stringResource(R.string.sound_none)
                else pluralStringResource(R.plurals.sound_count, events.size, events.size),
                style = MaterialTheme.typography.bodyMedium,
            )
            val shown = if (showAll) events else events.take(EVENTS_SHOWN)
            shown.forEach { event ->
                Text(
                    stringResource(
                        R.string.sound_event,
                        formatClock(event.startTime, zone),
                        formatEventLength(event.endTime - event.startTime),
                        stringResource(loudnessText(event.peakAboveBackgroundDb)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!showAll && events.size > EVENTS_SHOWN) {
                TextButton(onClick = { showAll = true }) {
                    Text(stringResource(R.string.sound_show_all, events.size))
                }
            }
            Text(stringResource(R.string.sound_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** "3 s" for short sounds, otherwise the usual "1 h 2 m" / "4 m". */
@Composable
private fun formatEventLength(millis: Long): String =
    if (millis < 60_000) stringResource(R.string.duration_s, ((millis + 999) / 1000).coerceAtLeast(1))
    else formatDuration(millis)

/** Words, not numbers: phone microphones aren't calibrated. */
@StringRes
private fun loudnessText(aboveBackgroundDb: Float): Int = when {
    aboveBackgroundDb >= 35f -> R.string.loudness_very_loud
    aboveBackgroundDb >= 25f -> R.string.loudness_loud
    else -> R.string.loudness_moderate
}

@StringRes
private fun gapReasonText(reason: Coverage.GapReason): Int = when (reason) {
    Coverage.GapReason.NOT_RUNNING -> R.string.gap_not_running
    Coverage.GapReason.USER_STOPPED -> R.string.gap_user_stopped
    Coverage.GapReason.MIC_SILENCED -> R.string.gap_mic_silenced
    Coverage.GapReason.MIC_ERROR -> R.string.gap_mic_error
    Coverage.GapReason.STOPPED_UNEXPECTEDLY -> R.string.gap_stopped_unexpectedly
}
