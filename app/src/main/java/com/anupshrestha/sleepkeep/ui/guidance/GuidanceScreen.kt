package com.anupshrestha.sleepkeep.ui.guidance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.guidance.SleepGuidelines
import com.anupshrestha.sleepkeep.guidance.SleepStats
import com.anupshrestha.sleepkeep.ui.common.BackButton
import com.anupshrestha.sleepkeep.ui.common.formatDuration
import com.anupshrestha.sleepkeep.ui.common.formatMinuteOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidanceScreen(
    onBack: () -> Unit,
    viewModel: GuidanceViewModel = viewModel(factory = GuidanceViewModel.Factory),
) {
    val weekly by viewModel.weekly.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guidance_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val w = weekly) {
                is SleepStats.Weekly.Summary -> {
                    DurationCard(w)
                    ScheduleCard(w)
                }
                is SleepStats.Weekly.NotEnoughData -> Section(
                    title = stringResource(R.string.guidance_duration_title),
                    body = listOf(stringResource(R.string.weekly_not_enough, SleepGuidelines.MIN_NIGHTS, w.nights)),
                )
                null -> Unit
            }
            AboutCard()
        }
    }
}

@Composable
private fun DurationCard(w: SleepStats.Weekly.Summary) {
    val average = formatDuration(w.averageSleepMillis)
    val lines = buildList {
        add(
            if (w.meetsGuideline) stringResource(R.string.duration_met, average, w.nights)
            else stringResource(R.string.duration_not_met, average, w.nights, w.nightsMeetingGuideline)
        )
        if (w.timeInBedOnlyNights > 0) {
            add(pluralStringResource(R.plurals.time_in_bed_caveat, w.timeInBedOnlyNights, w.timeInBedOnlyNights))
        }
    }
    Section(stringResource(R.string.guidance_duration_title), lines)
}

@Composable
private fun ScheduleCard(w: SleepStats.Weekly.Summary) {
    Section(
        title = stringResource(R.string.guidance_schedule_title),
        body = listOf(
            stringResource(
                R.string.schedule_text,
                formatMinuteOfDay(w.bedtime.usualMinuteOfDay),
                minutes(w.bedtime.averageDifferenceMinutes),
                formatMinuteOfDay(w.wakeTime.usualMinuteOfDay),
                minutes(w.wakeTime.averageDifferenceMinutes),
            ),
            stringResource(R.string.schedule_cdc),
        ),
    )
}

@Composable
private fun AboutCard() {
    val uriHandler = LocalUriHandler.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.guidance_about_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.guidance_recommendation), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.guidance_disclaimer), style = MaterialTheme.typography.bodyMedium)
            SleepGuidelines.sources.forEach { source ->
                TextButton(onClick = {
                    // No browser installed is the only way this fails; nothing useful to show then.
                    runCatching { uriHandler.openUri(source.url) }
                }) {
                    Text(stringResource(R.string.guidance_source, source.name, source.title, source.pageDate))
                }
            }
            Text(
                stringResource(R.string.guidance_checked, SleepGuidelines.CHECKED_ON),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Section(title: String, body: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            body.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun minutes(count: Int): String = pluralStringResource(R.plurals.minutes_count, count, count)
