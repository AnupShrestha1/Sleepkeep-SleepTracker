package com.anupshrestha.sleepkeep.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.ui.common.Hint
import com.anupshrestha.sleepkeep.ui.common.IconBadge
import com.anupshrestha.sleepkeep.ui.common.MoonLogo
import com.anupshrestha.sleepkeep.ui.common.NightCard
import com.anupshrestha.sleepkeep.ui.common.ScreenHeader
import com.anupshrestha.sleepkeep.ui.common.StatusPill
import com.anupshrestha.sleepkeep.ui.common.hasMicPermission
import com.anupshrestha.sleepkeep.ui.common.notificationsEnabled
import com.anupshrestha.sleepkeep.ui.common.openAppSettings
import com.anupshrestha.sleepkeep.ui.theme.MoonGold

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
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
    val good = MaterialTheme.colorScheme.tertiary
    val warn = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(stringResource(R.string.settings_title), R.drawable.ic_settings)

        Section(R.drawable.ic_lock, stringResource(R.string.privacy_title), tint = MaterialTheme.colorScheme.tertiary) {
            listOf(
                R.string.privacy_on_device,
                R.string.privacy_no_internet,
                R.string.privacy_no_backup,
                R.string.privacy_no_audio,
                R.string.privacy_no_tracking,
            ).forEach { Bullet(stringResource(it)) }
        }

        Section(R.drawable.ic_mic, stringResource(R.string.permissions_title)) {
            PermissionRow(
                stringResource(R.string.permission_mic),
                if (micGranted) stringResource(R.string.permission_allowed) else stringResource(R.string.permission_not_allowed),
                if (micGranted) good else warn,
            )
            if (!micGranted) Hint(stringResource(R.string.permission_mic_off_hint))
            PermissionRow(
                stringResource(R.string.permission_notifications),
                if (notificationsOn) stringResource(R.string.permission_on) else stringResource(R.string.permission_off),
                if (notificationsOn) good else warn,
            )
            if (!notificationsOn) Hint(stringResource(R.string.permission_notifications_off_hint))
            TextButton(onClick = { openAppSettings(context) }) { Text(stringResource(R.string.open_app_settings)) }
        }

        Section(R.drawable.ic_battery, stringResource(R.string.battery_title), tint = MaterialTheme.colorScheme.secondary) {
            Text(stringResource(R.string.battery_text), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { openAppSettings(context) }) { Text(stringResource(R.string.open_app_settings)) }
        }

        Section(R.drawable.ic_delete, stringResource(R.string.delete_all_title), tint = MaterialTheme.colorScheme.error) {
            Text(stringResource(R.string.delete_all_text), style = MaterialTheme.typography.bodyMedium)
            if (deleted) StatusPill(stringResource(R.string.delete_all_done), good)
            TextButton(onClick = { confirmingDelete = true }) {
                Text(stringResource(R.string.delete_all_button), color = MaterialTheme.colorScheme.error)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MoonLogo(72.dp)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
            version?.let { Hint(stringResource(R.string.app_version, it), Modifier.padding(top = 2.dp)) }
            Hint(
                stringResource(R.string.settings_footer),
                Modifier.padding(top = 6.dp),
            )
            Hint(stringResource(R.string.sound_credits), Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(8.dp))
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
private fun Section(
    @DrawableRes icon: Int,
    title: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    NightCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(icon, tint = tint)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        content()
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            painterResource(R.drawable.ic_check_circle), contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp).padding(top = 2.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PermissionRow(name: String, state: String, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        StatusPill(state, color)
    }
}

