package com.remindme.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var stopping = false

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarm_service",
                "Alarm",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            return START_NOT_STICKY
        }

        val id = intent?.getIntExtra("id", 0) ?: 0
        val title = intent?.getStringExtra("title") ?: "Reminder"
        val ringtone = intent?.getStringExtra("ringtone") ?: "Default"
        val snoozeMinutes = intent?.getIntExtra("snoozeMinutes", 10) ?: 10

        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
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
            this,
            id,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            id + 100000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "alarm_service")
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(1, notification)
        }

        startRingtone(ringtone)

        // Best-effort: bring the alarm UI to the front. On modern Android this may be
        // blocked from the background, but the full-screen intent above handles it.
        try {
            startActivity(alarmIntent)
        } catch (_: Exception) {
        }

        return START_NOT_STICKY
    }

    private fun stopAlarm() {
        if (stopping) return
        stopping = true
        stopRingtone()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun getRawUri(context: Context, rawName: String): Uri {
        val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }

    private fun startRingtone(ringtoneName: String) {
        stopRingtone()
        try {
            val name = ringtoneName.lowercase()
            val player = MediaPlayer()
            mediaPlayer = player
            val uri = when (name) {
                "alarm" -> getRawUri(this, "alarm")
                "bell" -> getRawUri(this, "bell")
                "birds" -> getRawUri(this, "birds")
                "chime" -> getRawUri(this, "chime")
                "digital" -> getRawUri(this, "digital")
                "ding" -> getRawUri(this, "ding")
                "drop" -> getRawUri(this, "drop")
                "harmony" -> getRawUri(this, "harmony")
                "marimba" -> getRawUri(this, "marimba")
                "melody" -> getRawUri(this, "melody")
                "morning" -> getRawUri(this, "morning")
                "music" -> getRawUri(this, "music")
                "piano" -> getRawUri(this, "piano")
                "pop" -> getRawUri(this, "pop")
                "radar" -> getRawUri(this, "radar")
                "signal" -> getRawUri(this, "signal")
                "siren" -> getRawUri(this, "siren")
                "star" -> getRawUri(this, "star")
                "sunrise" -> getRawUri(this, "sunrise")
                "twinkle" -> getRawUri(this, "twinkle")
                "whistle" -> getRawUri(this, "whistle")
                "default" -> getRawUri(this, "defaulttone")
                else -> getRawUri(this, "defaulttone")
            }
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(this, uri)
            player.isLooping = true
            player.prepare()
            player.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 1000), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 1000), 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopRingtone() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        stopRingtone()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.remindme.app.STOP_ALARM"
    }
}
