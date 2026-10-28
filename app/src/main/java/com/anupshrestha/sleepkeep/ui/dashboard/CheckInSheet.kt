package com.anupshrestha.sleepkeep.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.R

private data class Choice(val minutes: Int, @StringRes val label: Int)

private val fallAsleepChoices = listOf(
    Choice(5, R.string.choice_under_5),
    Choice(15, R.string.choice_15),
    Choice(30, R.string.choice_30),
    Choice(45, R.string.choice_45),
    Choice(60, R.string.choice_60_plus),
)

private val awakeChoices = listOf(
    Choice(0, R.string.choice_none),
    Choice(15, R.string.choice_15),
    Choice(30, R.string.choice_30),
    Choice(60, R.string.choice_60),
    Choice(90, R.string.choice_90_plus),
)

/** Optional morning questions; an unanswered question is saved as "not answered". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInSheet(
    onSave: (fallAsleepMinutes: Int?, awakeMinutes: Int?) -> Unit,
    onSkip: () -> Unit,
) {
    var fallAsleep by rememberSaveable { mutableStateOf<Int?>(null) }
    var awake by rememberSaveable { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.check_in_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.check_in_intro), style = MaterialTheme.typography.bodyMedium)

            Text(stringResource(R.string.check_in_fall_asleep), style = MaterialTheme.typography.titleSmall)
            Choices(fallAsleepChoices, fallAsleep) { fallAsleep = it }

            Text(stringResource(R.string.check_in_awake), style = MaterialTheme.typography.titleSmall)
            Choices(awakeChoices, awake) { awake = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End),
            ) {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.skip)) }
                Button(onClick = { onSave(fallAsleep, awake) }) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

/** Single choice; tapping the selected chip clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choices(choices: List<Choice>, selected: Int?, onSelect: (Int?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { choice ->
            FilterChip(
                selected = selected == choice.minutes,
                onClick = { onSelect(if (selected == choice.minutes) null else choice.minutes) },
                label = { Text(stringResource(choice.label)) },
            )
        }
    }
}
