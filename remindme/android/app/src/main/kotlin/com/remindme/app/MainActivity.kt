package com.remindme.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
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
                    scheduleAlarm(id, timeMillis, title, ringtone)
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
                    rescheduleAlarm(id, timeMillis, title, ringtone)
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
                else -> result.notImplemented()
            }
        }
    }

    private fun scheduleAlarm(id: Int, timeMillis: Long, title: String, ringtone: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("ringtone", ringtone)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        }
    }

    private fun rescheduleAlarm(id: Int, timeMillis: Long, title: String, ringtone: String) {
        cancelAlarm(id)
        scheduleAlarm(id, timeMillis, title, ringtone)
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
        // Best-effort: cancel via shared preferences used by flutter_local_notifications
        try {
            val prefs = getSharedPreferences("flutter_local_notifications", Context.MODE_PRIVATE)
            prefs.edit().remove("notification_$id").apply()
        } catch (_: Exception) {}
    }

    private fun snoozeFlutterNotification(id: Int, timeMillis: Long, title: String, ringtone: String) {
        // Best-effort: handled via method channel to Dart if engine is alive
        // The native alarm reschedule is the primary mechanism
    }
}
