package com.remindme.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
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

        val title = intent.getStringExtra("title") ?: "Reminder"
        val ringtoneName = intent.getStringExtra("ringtone") ?: "Default"
        findViewById<TextView>(R.id.alarmTitle).text = title
        findViewById<TextView>(R.id.alarmTime).text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            stopRingtone()
            finish()
        }

        findViewById<Button>(R.id.btnDone).setOnClickListener {
            stopRingtone()
            finish()
        }

        startRingtone(ringtoneName)
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
