package com.remindme.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibrationEffect
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

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
        val ringtoneName = intent?.getStringExtra("ringtone") ?: "Default"
        findViewById<TextView>(R.id.alarmTitle).text = title
        findViewById<TextView>(R.id.alarmTime).text =
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        startRingtone(ringtoneName)
    }

    private fun snoozeAlarm() {
        val id = intent?.getIntExtra("id", 0) ?: 0
        val title = intent?.getStringExtra("title") ?: "Reminder"
        val ringtone = intent?.getStringExtra("ringtone") ?: "Default"

        stopRingtone()

        val newTime = System.currentTimeMillis() + 10 * 60 * 1000
        updateReminderTime(id, newTime)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("ringtone", ringtone)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, id, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, newTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, newTime, pendingIntent)
        }

        finish()
    }

    private fun doneAlarm() {
        val id = intent?.getIntExtra("id", 0) ?: 0

        stopRingtone()
        updateReminderDone(id)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, id, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        finish()
    }

    private fun updateReminderTime(id: Int, newTimeMillis: Long) {
        try {
            val dbPath = getDatabasePath("remindme.db")
            if (!dbPath.exists()) return
            val db = SQLiteDatabase.openDatabase(dbPath.path, null, SQLiteDatabase.OPEN_READWRITE)
            val values = android.content.ContentValues().apply {
                put("dateTime", newTimeMillis)
            }
            db.update("reminders", values, "id = ?", arrayOf(id.toString()))
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateReminderDone(id: Int) {
        try {
            val dbPath = getDatabasePath("remindme.db")
            if (!dbPath.exists()) return
            val db = SQLiteDatabase.openDatabase(dbPath.path, null, SQLiteDatabase.OPEN_READWRITE)
            val values = android.content.ContentValues().apply {
                put("isDone", 1)
                put("isActive", 0)
            }
            db.update("reminders", values, "id = ?", arrayOf(id.toString()))
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getRawUri(context: Context, rawName: String): Uri {
        val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }

    private fun startRingtone(ringtoneName: String) {
        try {
            val name = ringtoneName.lowercase()
            val mediaPlayer = MediaPlayer()
            this.mediaPlayer = mediaPlayer
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
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mediaPlayer.setDataSource(this, uri)
            mediaPlayer.prepare()
            mediaPlayer.isLooping = true
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 1000), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 1000), 0)
        }
    }

    private fun stopRingtone() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }
}
