package com.anupshrestha.sleepkeep.ui.night

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.data.SessionRules
import com.anupshrestha.sleepkeep.data.SleepSession
import com.anupshrestha.sleepkeep.ui.common.formatClock
import com.anupshrestha.sleepkeep.ui.common.formatDay
import com.anupshrestha.sleepkeep.ui.common.is24HourClock
import com.anupshrestha.sleepkeep.ui.common.timeErrorText
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private enum class Picker { START_DATE, START_TIME, END_DATE, END_TIME }

private fun toWholeMinute(millis: Long): Long = millis - millis % 60_000

/** Bedtime and wake-up, each as a date and a clock time in the session's own time zone. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimesDialog(
    session: SleepSession,
    error: SessionRules.TimeError?,
    onSave: (start: Long, end: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val zoneId = session.zoneId
    val zone = ZoneId.of(zoneId)
    var start by rememberSaveable { mutableLongStateOf(session.startTime) }
    var end by rememberSaveable { mutableLongStateOf(session.endTime ?: session.startTime) }
    var picker by rememberSaveable { mutableStateOf<Picker?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_times)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeRow(
                    label = stringResource(R.string.bedtime),
                    day = formatDay(start, zoneId),
                    clock = formatClock(start, zoneId),
                    onDay = { picker = Picker.START_DATE },
                    onClock = { picker = Picker.START_TIME },
                )
                TimeRow(
                    label = stringResource(R.string.wake_up),
                    day = formatDay(end, zoneId),
                    clock = formatClock(end, zoneId),
                    onDay = { picker = Picker.END_DATE },
                    onClock = { picker = Picker.END_TIME },
                )
                error?.let {
                    Text(stringResource(timeErrorText(it)), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        // The pickers only show minutes, so leftover seconds from the original times are dropped.
        confirmButton = {
            TextButton(onClick = { onSave(toWholeMinute(start), toWholeMinute(end)) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )

    when (picker) {
        Picker.START_DATE -> DateDialog(start, zone, { start = it }, { picker = null })
        Picker.START_TIME -> TimeDialog(start, zone, { start = it }, { picker = null })
        Picker.END_DATE -> DateDialog(end, zone, { end = it }, { picker = null })
        Picker.END_TIME -> TimeDialog(end, zone, { end = it }, { picker = null })
        null -> Unit
    }
}

@Composable
private fun TimeRow(label: String, day: String, clock: String, onDay: () -> Unit, onClock: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onDay) { Text(day) }
            OutlinedButton(onClick = onClock) { Text(clock) }
        }
    }
}

/** Changes the date of [millis] and keeps its clock time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(millis: Long, zone: ZoneId, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val current = Instant.ofEpochMilli(millis).atZone(zone)
    // The date picker works in UTC midnights.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    val date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    onPicked(current.with(date).toInstant().toEpochMilli())
                }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

/** Changes the clock time of [millis] and keeps its date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(millis: Long, zone: ZoneId, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val current = Instant.ofEpochMilli(millis).atZone(zone)
    val state = rememberTimePickerState(
        initialHour = current.hour,
        initialMinute = current.minute,
        is24Hour = is24HourClock(),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onPicked(current.with(LocalTime.of(state.hour, state.minute)).toInstant().toEpochMilli())
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
