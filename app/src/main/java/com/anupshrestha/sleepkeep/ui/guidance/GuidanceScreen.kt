package com.anupshrestha.sleepkeep.ui.guidance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.anupshrestha.sleepkeep.ui.common.BedtimeRangeChart
import com.anupshrestha.sleepkeep.ui.common.Hint
import com.anupshrestha.sleepkeep.ui.common.IconBadge
import com.anupshrestha.sleepkeep.ui.common.LegendItem
import com.anupshrestha.sleepkeep.ui.common.NightCard
import com.anupshrestha.sleepkeep.ui.common.ScreenHeader
import com.anupshrestha.sleepkeep.ui.common.StatusPill
import com.anupshrestha.sleepkeep.ui.common.TrendBars
import com.anupshrestha.sleepkeep.ui.common.WeekBars
import com.anupshrestha.sleepkeep.ui.common.formatDuration
import com.anupshrestha.sleepkeep.ui.common.formatMinuteOfDay
import com.anupshrestha.sleepkeep.ui.theme.NumberStyle

/** Sleep duration and schedule over a week, a month or three months, against adult guidance. */
@Composable
fun GuidanceScreen(viewModel: GuidanceViewModel = viewModel(factory = GuidanceViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(stringResource(R.string.insights_title), R.drawable.ic_insights)
        val s = state ?: return@Column
        RangeSwitch(s.range, viewModel::setRange)
        DurationCard(s)
        ScheduleCard(s)
        WeekdayWeekendCard(s.weekdayWeekend)
        AboutCard()
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSwitch(selected: InsightsRange, onSelect: (InsightsRange) -> Unit) {
    val labels = mapOf(
        InsightsRange.WEEK to R.string.range_week,
        InsightsRange.MONTH to R.string.range_month,
        InsightsRange.QUARTER to R.string.range_quarter,
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        InsightsRange.entries.forEachIndexed { i, range ->
            SegmentedButton(
                selected = range == selected,
                onClick = { onSelect(range) },
                shape = SegmentedButtonDefaults.itemShape(i, InsightsRange.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                ),
                icon = {},
            ) { Text(stringResource(labels.getValue(range))) }
        }
    }
}

@Composable
private fun DurationCard(s: InsightsState) {
    NightCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.guidance_duration_title), style = MaterialTheme.typography.titleMedium)
                Hint(stringResource(R.string.last_days, s.range.days))
            }
            val w = s.weekly
            if (w is SleepStats.Weekly.Summary) {
                StatusPill(
                    stringResource(if (w.meetsGuideline) R.string.pill_meets else R.string.pill_below),
                    if (w.meetsGuideline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                )
            }
        }
        when (val w = s.weekly) {
            is SleepStats.Weekly.Summary -> {
                Text(formatDuration(w.averageSleepMillis), style = NumberStyle)
                Hint(pluralStringResource(R.plurals.average_over_nights, w.nights, w.nights))
            }
            is SleepStats.Weekly.NotEnoughData -> Text(
                stringResource(R.string.weekly_not_enough, SleepGuidelines.MIN_NIGHTS, w.nights),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (s.range == InsightsRange.WEEK) {
            WeekBars(s.nights, height = 170.dp)
        } else {
            TrendBars(s.nights, s.rolling)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(MaterialTheme.colorScheme.primary, stringResource(R.string.legend_sleep))
            LegendItem(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f), stringResource(R.string.legend_bed_only))
            LegendItem(MaterialTheme.colorScheme.secondary, stringResource(R.string.legend_guideline))
        }
        if (s.range != InsightsRange.WEEK) {
            LegendItem(MaterialTheme.colorScheme.onSurface, stringResource(R.string.legend_rolling))
        }
        val w = s.weekly
        if (w is SleepStats.Weekly.Summary) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val average = formatDuration(w.averageSleepMillis)
            Text(
                if (w.meetsGuideline) stringResource(R.string.duration_met_days, average, w.nights, s.range.days)
                else stringResource(R.string.duration_not_met_days, average, w.nights, w.nightsMeetingGuideline, s.range.days),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (w.timeInBedOnlyNights > 0) {
                Hint(pluralStringResource(R.plurals.time_in_bed_caveat, w.timeInBedOnlyNights, w.timeInBedOnlyNights))
            }
        }
    }
}

@Composable
private fun ScheduleCard(s: InsightsState) {
    NightCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(R.drawable.ic_history)
            Column {
                Text(stringResource(R.string.guidance_schedule_title), style = MaterialTheme.typography.titleMedium)
                Hint(stringResource(R.string.bedtime_chart_hint))
            }
        }
        BedtimeRangeChart(s.nights)
        val w = s.weekly
        if (w is SleepStats.Weekly.Summary) {
            Row {
                TimeStat(
                    stringResource(R.string.usual_bedtime), formatMinuteOfDay(w.bedtime.usualMinuteOfDay),
                    minutes(w.bedtime.averageDifferenceMinutes), Modifier.weight(1f),
                )
                TimeStat(
                    stringResource(R.string.usual_wake_up), formatMinuteOfDay(w.wakeTime.usualMinuteOfDay),
                    minutes(w.wakeTime.averageDifferenceMinutes), Modifier.weight(1f),
                )
            }
            Text(
                stringResource(
                    R.string.schedule_text,
                    formatMinuteOfDay(w.bedtime.usualMinuteOfDay),
                    minutes(w.bedtime.averageDifferenceMinutes),
                    formatMinuteOfDay(w.wakeTime.usualMinuteOfDay),
                    minutes(w.wakeTime.averageDifferenceMinutes),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Hint(stringResource(R.string.schedule_cdc))
    }
}

@Composable
private fun WeekdayWeekendCard(split: SleepStats.WeekdayWeekend) {
    NightCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(R.drawable.ic_insights, tint = MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.ww_title), style = MaterialTheme.typography.titleMedium)
        }
        val weekdays = split.weekdays
        val weekend = split.weekend
        if (weekdays == null || weekend == null) {
            Hint(stringResource(R.string.ww_needs))
            return@NightCard
        }
        Row {
            PartStat(stringResource(R.string.ww_weekdays), weekdays, Modifier.weight(1f))
            PartStat(stringResource(R.string.ww_weekend), weekend, Modifier.weight(1f))
        }
        val diff = weekend.averageSleepMillis - weekdays.averageSleepMillis
        Text(
            when {
                diff >= 30 * 60_000 -> stringResource(R.string.ww_more_weekend, formatDuration(diff))
                diff <= -30 * 60_000 -> stringResource(R.string.ww_less_weekend, formatDuration(-diff))
                else -> stringResource(R.string.ww_similar)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PartStat(label: String, part: SleepStats.WeekPart, modifier: Modifier = Modifier) {
    Column(modifier) {
        Hint(label)
        Text(formatDuration(part.averageSleepMillis), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.ww_bed_around, formatMinuteOfDay(part.bedtime.usualMinuteOfDay)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TimeStat(label: String, time: String, variation: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Hint(label)
        Text(time, style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.varies_by, variation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AboutCard() {
    val uriHandler = LocalUriHandler.current
    NightCard(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(R.drawable.ic_shield, tint = MaterialTheme.colorScheme.tertiary)
            Text(stringResource(R.string.guidance_about_title), style = MaterialTheme.typography.titleMedium)
        }
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
        Hint(stringResource(R.string.guidance_checked, SleepGuidelines.CHECKED_ON))
    }
}

@Composable
private fun minutes(count: Int): String = pluralStringResource(R.plurals.minutes_count, count, count)
