package com.anupshrestha.sleepkeep.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.guidance.SleepGuidelines
import com.anupshrestha.sleepkeep.guidance.SleepStats
import com.anupshrestha.sleepkeep.ui.common.formatDuration

/** Short weekly summary; duration and schedule stay separate measurements. */
@Composable
fun WeeklyCard(weekly: SleepStats.Weekly, onDetails: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (weekly) {
                is SleepStats.Weekly.NotEnoughData -> Text(
                    stringResource(R.string.weekly_not_enough, SleepGuidelines.MIN_NIGHTS, weekly.nights),
                    style = MaterialTheme.typography.bodyMedium,
                )
                is SleepStats.Weekly.Summary -> {
                    Text(
                        pluralStringResource(R.plurals.weekly_title, weekly.nights, weekly.nights),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        stringResource(R.string.weekly_average, formatDuration(weekly.averageSleepMillis)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(if (weekly.meetsGuideline) R.string.weekly_meets else R.string.weekly_below),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.weekly_bedtime_varied, weekly.bedtime.averageDifferenceMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.weekly_wake_varied, weekly.wakeTime.averageDifferenceMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(stringResource(R.string.weekly_disclaimer_short), style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = onDetails, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.weekly_details))
            }
        }
    }
}
