package com.anupshrestha.sleepkeep.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.ui.common.BackButton
import com.anupshrestha.sleepkeep.ui.common.hasMicPermission
import com.anupshrestha.sleepkeep.ui.common.notificationsEnabled
import com.anupshrestha.sleepkeep.ui.common.openAppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val context = LocalContext.current
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    // Re-read permissions when coming back from system settings.
    var permissionCheck by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionCheck++ }
    val micGranted = remember(permissionCheck) { hasMicPermission(context) }
    val notificationsOn = remember(permissionCheck) { notificationsEnabled(context) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            Section(stringResource(R.string.privacy_title)) {
                listOf(
                    R.string.privacy_on_device,
                    R.string.privacy_no_internet,
                    R.string.privacy_no_backup,
                    R.string.privacy_no_audio,
                    R.string.privacy_no_tracking,
                ).forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
            }

            Section(stringResource(R.string.permissions_title)) {
                Text(
                    stringResource(if (micGranted) R.string.permission_mic_on else R.string.permission_mic_off),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(
                        if (notificationsOn) R.string.permission_notifications_on else R.string.permission_notifications_off
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { openAppSettings(context) }) { Text(stringResource(R.string.open_app_settings)) }
            }

            Section(stringResource(R.string.battery_title)) {
                Text(stringResource(R.string.battery_text), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { openAppSettings(context) }) { Text(stringResource(R.string.open_app_settings)) }
            }

            Section(stringResource(R.string.delete_all_title)) {
                Text(stringResource(R.string.delete_all_text), style = MaterialTheme.typography.bodyMedium)
                if (deleted) {
                    Text(stringResource(R.string.delete_all_done), style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete_all_button))
                }
            }

            version?.let {
                Text(stringResource(R.string.app_version, it), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.delete_all_confirm_title)) },
            text = { Text(stringResource(R.string.delete_all_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    viewModel.deleteAll()
                }) { Text(stringResource(R.string.delete_all_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
