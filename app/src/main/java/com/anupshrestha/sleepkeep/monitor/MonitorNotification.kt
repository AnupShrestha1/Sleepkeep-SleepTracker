package com.anupshrestha.sleepkeep.monitor

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.anupshrestha.sleepkeep.MainActivity
import com.anupshrestha.sleepkeep.R

/** The ongoing notification Android requires while the microphone service runs. */
object MonitorNotification {
    const val ID = 1
    private const val CHANNEL_ID = "monitoring"

    fun build(context: Context, startedAt: Long): Notification {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.notification_channel))
                .setShowBadge(false)
                .build()
        )
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            context, 1,
            SoundMonitorService.stopIntent(context),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setWhen(startedAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openApp)
            .addAction(0, context.getString(R.string.stop_monitoring), stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
