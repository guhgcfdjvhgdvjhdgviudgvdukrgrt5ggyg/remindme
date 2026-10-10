package com.remindme.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "android.intent.action.REBOOT"
        ) {
            rescheduleAlarms(context)
        }
    }

    private fun rescheduleAlarms(context: Context) {
        var db: SQLiteDatabase? = null
        var cursor: android.database.Cursor? = null
        try {
            val dbPath = context.getDatabasePath("remindme.db")
            if (!dbPath.exists()) return

            db = SQLiteDatabase.openDatabase(dbPath.path, null, SQLiteDatabase.OPEN_READONLY)
            cursor = db.rawQuery(
                "SELECT id, dateTime, title, ringtone, snoozeMinutes FROM reminders WHERE isActive = 1 AND isDone = 0",
                null
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val now = System.currentTimeMillis()

            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val timeMillis = cursor.getLong(1)
                val title = cursor.getString(2) ?: "Reminder"
                val ringtone = cursor.getString(3) ?: "Default"
                val snoozeMinutes = cursor.getInt(4)

                if (timeMillis <= now) continue

                val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("id", id)
                    putExtra("title", title)
                    putExtra("ringtone", ringtone)
                    putExtra("snoozeMinutes", snoozeMinutes)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    id,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            !alarmManager.canScheduleExactAlarms()
                        ) {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent
                            )
                        } else {
                            alarmManager.setAlarmClock(
                                AlarmManager.AlarmClockInfo(timeMillis, pendingIntent),
                                pendingIntent
                            )
                        }
                    } catch (e: SecurityException) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
            db?.close()
        }
    }
}
