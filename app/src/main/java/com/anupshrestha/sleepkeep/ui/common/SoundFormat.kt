package com.anupshrestha.sleepkeep.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.SoundEvent
import com.anupshrestha.sleepkeep.ui.theme.Amber
import com.anupshrestha.sleepkeep.ui.theme.Coral
import com.anupshrestha.sleepkeep.ui.theme.MoonGold

/** Words, not numbers: phone microphones aren't calibrated. */
@StringRes
fun loudnessText(aboveBackgroundDb: Float): Int = when {
    aboveBackgroundDb >= 35f -> R.string.loudness_very_loud
    aboveBackgroundDb >= 25f -> R.string.loudness_loud
    else -> R.string.loudness_moderate
}

fun loudnessColor(aboveBackgroundDb: Float): Color = when {
    aboveBackgroundDb >= 35f -> Coral
    aboveBackgroundDb >= 25f -> Amber
    else -> MoonGold
}

/** "3 s" for short sounds, otherwise the usual "1 h 2 m" / "4 m". */
@Composable
fun formatEventLength(millis: Long): String =
    if (millis < 60_000) stringResource(R.string.duration_s, ((millis + 999) / 1000).coerceAtLeast(1))
    else formatDuration(millis)

/** One loud sound: coloured dot, time, loudness word, and length on the right. */
@Composable
fun SoundEventRow(event: SoundEvent, zoneId: String) {
    val color = loudnessColor(event.peakAboveBackgroundDb)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(formatClock(event.startTime, zoneId), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(loudnessText(event.peakAboveBackgroundDb)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatEventLength(event.endTime - event.startTime),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
