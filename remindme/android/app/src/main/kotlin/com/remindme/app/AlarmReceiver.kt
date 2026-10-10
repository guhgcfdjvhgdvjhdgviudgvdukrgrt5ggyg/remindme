package com.remindme.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title") ?: "Reminder"
        val ringtone = intent.getStringExtra("ringtone") ?: "Default"
        val snoozeMinutes = intent.getIntExtra("snoozeMinutes", 10)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("ringtone", ringtone)
            putExtra("snoozeMinutes", snoozeMinutes)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // If the foreground service can't be started from the background,
            // fall back to showing the full-screen alarm notification directly.
            try {
                showNotification(context, id, title, ringtone, snoozeMinutes)
            } catch (_: Exception) {
            }
        }
    }

    private fun showNotification(
        context: Context,
        id: Int,
        title: String,
        ringtone: String,
        snoozeMinutes: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "alarm_service",
                "Alarm",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(channel)
        }

        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("ringtone", ringtone)
            putExtra("snoozeMinutes", snoozeMinutes)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        val contentPending = PendingIntent.getActivity(
            context,
            id,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            context,
            id + 100000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(context, "alarm_service")
            .setContentTitle(title)
            .setContentText("Alarm — tap to open")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPending)
            .setFullScreenIntent(contentPending, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSound(null)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                stopPending
            )
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)

        try {
            context.startActivity(alarmIntent)
        } catch (_: Exception) {
        }
    }
}