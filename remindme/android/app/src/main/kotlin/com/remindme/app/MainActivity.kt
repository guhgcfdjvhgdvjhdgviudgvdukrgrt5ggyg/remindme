package com.remindme.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val channel = "com.remindme.app/alarm"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channel).setMethodCallHandler { call, result ->
            when (call.method) {
                "init" -> {
                    requestAlarmPermissionsOnce()
                    result.success(true)
                }
                "scheduleAlarm" -> {
                    try {
                        val id = call.argument<Int>("id") ?: 0
                        val timeMillis = call.argument<Long>("timeMillis") ?: 0L
                        val title = call.argument<String>("title") ?: "Reminder"
                        val ringtone = call.argument<String>("ringtone") ?: "Default"
                        val snoozeMinutes = call.argument<Int>("snoozeMinutes") ?: 10
                        scheduleAlarm(id, timeMillis, title, ringtone, snoozeMinutes)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("SCHEDULE_ERROR", e.message, null)
                    }
                }
                "cancelAlarm" -> {
                    try {
                        val id = call.argument<Int>("id") ?: 0
                        cancelAlarm(id)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("CANCEL_ERROR", e.message, null)
                    }
                }
                "snoozeAlarm" -> {
                    try {
                        val id = call.argument<Int>("id") ?: 0
                        val timeMillis = call.argument<Long>("timeMillis") ?: 0L
                        val title = call.argument<String>("title") ?: "Reminder"
                        val ringtone = call.argument<String>("ringtone") ?: "Default"
                        val snoozeMinutes = call.argument<Int>("snoozeMinutes") ?: 10
                        rescheduleAlarm(id, timeMillis, title, ringtone, snoozeMinutes)
                        result.success(true)
                    } catch (e: Exception) {
                        result.error("RESCHEDULE_ERROR", e.message, null)
                    }
                }
                "cancelNotification" -> {
                    cancelFlutterNotification(call.argument<Int>("id") ?: 0)
                    result.success(true)
                }
                "snoozeNotification" -> {
                    snoozeFlutterNotification(
                        call.argument<Int>("id") ?: 0,
                        call.argument<Long>("timeMillis") ?: 0L
                    )
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
                            val i = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                            i.data = Uri.parse("package:$packageName")
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
                            val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            i.data = Uri.parse("package:$packageName")
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

    // Ask the user once for the permissions that make alarms reliable in the
    // background: exact alarms, ignoring battery optimization and full-screen
    // alarm display (Android 14+). Best-effort on every app launch until done.
    private fun requestAlarmPermissionsOnce() {
        try {
            val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("permission_nudge_done", false)) return
            prefs.edit().putBoolean("permission_nudge_done", true).apply()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!am.canScheduleExactAlarms()) {
                    val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    i.data = Uri.parse("package:$packageName")
                    startActivity(i)
                }
            }

            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val batOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                powerManager.isIgnoringBatteryOptimizations(packageName)
            if (!batOk) {
                val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                i.data = Uri.parse("package:$packageName")
                startActivity(i)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                if (!nm.canUseFullScreenIntent()) {
                    val i = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    i.data = Uri.parse("package:$packageName")
                    startActivity(i)
                }
            }
        } catch (_: Exception) {
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
        scheduleAlarmClock(alarmManager, timeMillis, pendingIntent)
    }

    // setAlarmClock is the most reliable alarm form: it wakes the device from
    // Doze, fires exactly on time and is exempt from background restrictions.
    private fun scheduleAlarmClock(
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
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent),
                        pendingIntent
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        } catch (e: Exception) {
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
        } catch (_: Exception) {
        }
    }

    private fun snoozeFlutterNotification(id: Int, timeMillis: Long) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancel(id)
        } catch (_: Exception) {
        }
    }
}