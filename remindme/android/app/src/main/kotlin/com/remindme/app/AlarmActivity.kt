package com.remindme.app

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        handleIntent(intent)

        findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            snoozeAlarm()
        }

        findViewById<Button>(R.id.btnDone).setOnClickListener {
            doneAlarm()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val title = intent?.getStringExtra("title") ?: "Reminder"
        findViewById<TextView>(R.id.alarmTitle).text = title
        findViewById<TextView>(R.id.alarmTime).text =
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    private fun stopServiceSound() {
        try {
            stopService(Intent(this, AlarmService::class.java))
        } catch (_: Exception) {
        }
    }

    private fun snoozeAlarm() {
        val id = intent?.getIntExtra("id", 0) ?: 0
        val title = intent?.getStringExtra("title") ?: "Reminder"
        val ringtone = intent?.getStringExtra("ringtone") ?: "Default"
        val snoozeMinutes = intent?.getIntExtra("snoozeMinutes", 10) ?: 10

        stopServiceSound()
        cancelNotification(id)

        val newTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L
        updateReminderTime(id, newTime)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("ringtone", ringtone)
            putExtra("snoozeMinutes", snoozeMinutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, id, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        exactOrInexact(alarmManager, newTime, pendingIntent)

        finish()
    }

    private fun doneAlarm() {
        val id = intent?.getIntExtra("id", 0) ?: 0

        stopServiceSound()
        cancelNotification(id)
        // The reminder is intentionally NOT deleted or marked complete here.
        // Tapping Done only silences the alarm; the reminder stays in the list.

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, id, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        finish()
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

    private fun updateReminderTime(id: Int, newTimeMillis: Long) {
        var db: SQLiteDatabase? = null
        try {
            val dbPath = getDatabasePath("remindme.db")
            if (!dbPath.exists()) return
            db = SQLiteDatabase.openDatabase(dbPath.path, null, SQLiteDatabase.OPEN_READWRITE)
            val values = android.content.ContentValues().apply {
                put("dateTime", newTimeMillis)
            }
            db.update("reminders", values, "id = ?", arrayOf(id.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
    }

    private fun cancelNotification(id: Int) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(id)
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            stopServiceSound()
        }
    }
}
