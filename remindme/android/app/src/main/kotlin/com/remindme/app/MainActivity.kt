package com.remindme.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val channel = "com.remindme.app/alarm"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channel).setMethodCallHandler { call, result ->
            when (call.method) {
                "scheduleAlarm" -> {
                    val id = call.argument<Int>("id") ?: 0
                    val timeMillis = call.argument<Long>("timeMillis") ?: 0L
                    val title = call.argument<String>("title") ?: "Reminder"
                    val ringtone = call.argument<String>("ringtone") ?: "Default"
                    val snoozeMinutes = call.argument<Int>("snoozeMinutes") ?: 10
                    scheduleAlarm(id, timeMillis, title, ringtone, snoozeMinutes)
                    result.success(true)
                }
                "cancelAlarm" -> {
                    val id = call.argument<Int>("id") ?: 0
                    cancelAlarm(id)
                    result.success(true)
                }
                "snoozeAlarm" -> {
                    val id = call.argument<Int>("id") ?: 0
                    val timeMillis = call.argument<Long>("timeMillis") ?: 0L
                    val title = call.argument<String>("title") ?: "Reminder"
                    val ringtone = call.argument<String>("ringtone") ?: "Default"
                    val snoozeMinutes = call.argument<Int>("snoozeMinutes") ?: 10
                    rescheduleAlarm(id, timeMillis, title, ringtone, snoozeMinutes)
                    result.success(true)
                }
                "cancelNotification" -> {
                    val id = call.argument<Int>("id") ?: 0
                    cancelFlutterNotification(id)
                    result.success(true)
                }
                "snoozeNotification" -> {
                    val id = call.argument<Int>("id") ?: 0
                    val timeMillis = call.argument<Long>("timeMillis") ?: 0L
                    val title = call.argument<String>("title") ?: "Reminder"
                    val ringtone = call.argument<String>("ringtone") ?: "Default"
                    snoozeFlutterNotification(id, timeMillis, title, ringtone)
                    result.success(true)
                }
                "canUseFullScreenIntent" -> {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        nm.canUseFullScreenIntent()
                    } else {
                        true
                    }
                    result.success(allowed)
                }
                "requestFullScreenIntentPermission" -> {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val i = Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                            i.data = android.net.Uri.parse("package:$packageName")
                            startActivity(i)
                        }
                    } catch (_: Exception) {
                    }
                    result.success(true)
                }
                "canScheduleExactAlarms" -> {
                    val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        am.canScheduleExactAlarms()
                    } else {
                        true
                    }
                    result.success(allowed)
                }
                "requestExactAlarmPermission" -> {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val i = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            i.data = android.net.Uri.parse("package:$packageName")
                            startActivity(i)
                        }
                    } catch (_: Exception) {
                    }
                    result.success(true)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun scheduleAlarm(id: Int, timeMillis: Long, title: String, ringtone: String, snoozeMinutes: Int = 10) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("ringtone", ringtone)
            putExtra("snoozeMinutes", snoozeMinutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        exactOrInexact(alarmManager, timeMillis, pendingIntent)
    }

    private fun exactOrInexact(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !alarmManager.canScheduleExactAlarms()
                ) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun rescheduleAlarm(id: Int, timeMillis: Long, title: String, ringtone: String, snoozeMinutes: Int = 10) {
        cancelAlarm(id)
        scheduleAlarm(id, timeMillis, title, ringtone, snoozeMinutes)
    }

    private fun cancelAlarm(id: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun cancelFlutterNotification(id: Int) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancel(id)
        } catch (_: Exception) {}
    }

    private fun snoozeFlutterNotification(id: Int, timeMillis: Long, title: String, ringtone: String) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancel(id)
        } catch (_: Exception) {}
    }
}
