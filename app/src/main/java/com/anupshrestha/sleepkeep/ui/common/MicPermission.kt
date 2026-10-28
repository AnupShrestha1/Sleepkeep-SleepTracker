package com.anupshrestha.sleepkeep.ui.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.anupshrestha.sleepkeep.R

fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

fun notificationsEnabled(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

/** The microphone, plus notifications on Android 13+ so the monitoring notification shows. */
fun monitoringPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }

/** The app's page in system settings, where a permanently denied permission can be allowed. */
fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/** The app's own explanation, shown before Android's permission prompt. */
@Composable
fun MicRationaleDialog(onContinue: () -> Unit, onNotNow: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNotNow,
        title = { Text(stringResource(R.string.mic_rationale_title)) },
        text = { Text(stringResource(R.string.mic_rationale_message)) },
        confirmButton = { TextButton(onClick = onContinue) { Text(stringResource(R.string.continue_label)) } },
        dismissButton = { TextButton(onClick = onNotNow) { Text(stringResource(R.string.not_now)) } },
    )
}
