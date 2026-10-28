package com.anupshrestha.sleepkeep.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.anupshrestha.sleepkeep.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val clock24Formatter = DateTimeFormatter.ofPattern("HH:mm")
private val clock12Formatter = DateTimeFormatter.ofPattern("h:mm a")

/** "7 h 40 m", or "40 m" under an hour. */
@Composable
fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours == 0L) {
        stringResource(R.string.duration_m, minutes)
    } else {
        stringResource(R.string.duration_hm, hours, minutes)
    }
}

/** Clock time in the session's own time zone, following the phone's 12/24-hour setting. */
@Composable
fun formatClock(millis: Long, zoneId: String): String {
    val formatter = if (DateFormat.is24HourFormat(LocalContext.current)) clock24Formatter else clock12Formatter
    return formatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.of(zoneId)))
}

/** "Tue 27 Oct". */
fun formatDay(millis: Long, zoneId: String): String =
    dayFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.of(zoneId)))

@Composable
fun is24HourClock(): Boolean = DateFormat.is24HourFormat(LocalContext.current)
