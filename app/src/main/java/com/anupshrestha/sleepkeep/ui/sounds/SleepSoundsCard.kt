package com.anupshrestha.sleepkeep.ui.sounds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.SleepApp
import com.anupshrestha.sleepkeep.sounds.SleepSound
import com.anupshrestha.sleepkeep.sounds.SleepSoundService
import com.anupshrestha.sleepkeep.sounds.SoundStatus
import com.anupshrestha.sleepkeep.ui.common.Hint
import com.anupshrestha.sleepkeep.ui.common.IconBadge
import com.anupshrestha.sleepkeep.ui.common.NightCard
import com.anupshrestha.sleepkeep.ui.common.PillButton
import com.anupshrestha.sleepkeep.ui.common.StatusPill
import com.anupshrestha.sleepkeep.ui.common.formatClock
import java.time.ZoneId

/** Timer choices in minutes; `null` plays until stopped. */
private val timers = listOf<Int?>(15, 30, 60, null)

/**
 * Pick a generated sound and a fade-out timer, then play. [monitoring] notes that listening
 * pauses while it plays, so the sound isn't logged as loud sounds.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepSoundsCard(monitoring: Boolean) {
    val controller = (LocalContext.current.applicationContext as SleepApp).container.sounds
    val status by controller.status.collectAsStateWithLifecycle()
    var sound by rememberSaveable { mutableStateOf(SleepSound.RAIN) }
    var minutes by rememberSaveable { mutableStateOf<Int?>(30) }

    NightCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(R.drawable.ic_sound, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.sounds_title), style = MaterialTheme.typography.titleMedium)
                Hint(stringResource(R.string.sounds_subtitle))
            }
        }

        when (val s = status) {
            is SoundStatus.Playing -> {
                StatusPill(
                    stringResource(R.string.sounds_playing, stringResource(SleepSoundService.labelOf(s.sound))),
                    MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    s.endsAt?.let { stringResource(R.string.sounds_stops_at, formatClock(it, ZoneId.systemDefault().id)) }
                        ?: stringResource(R.string.sounds_no_timer),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (monitoring) Hint(stringResource(R.string.sounds_pauses_monitoring))
                PillButton(
                    stringResource(R.string.sounds_stop), R.drawable.ic_stop, controller::stop,
                    container = MaterialTheme.colorScheme.surfaceContainerHighest,
                    content = MaterialTheme.colorScheme.onSurface,
                )
            }
            SoundStatus.Off -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SleepSound.entries.forEach { option ->
                        FilterChip(
                            selected = sound == option,
                            onClick = { sound = option },
                            label = { Text(stringResource(SleepSoundService.labelOf(option))) },
                            colors = chipColors(),
                        )
                    }
                }
                Hint(stringResource(R.string.sounds_timer_label))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    timers.forEach { option ->
                        FilterChip(
                            selected = minutes == option,
                            onClick = { minutes = option },
                            label = {
                                Text(option?.let { stringResource(R.string.sounds_minutes, it) } ?: stringResource(R.string.sounds_until_stopped))
                            },
                            colors = chipColors(),
                        )
                    }
                }
                if (monitoring) Hint(stringResource(R.string.sounds_pauses_monitoring))
                PillButton(
                    stringResource(R.string.sounds_play), R.drawable.ic_sound, { controller.play(sound, minutes) },
                    container = MaterialTheme.colorScheme.secondary,
                    content = MaterialTheme.colorScheme.onSecondary,
                )
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)
