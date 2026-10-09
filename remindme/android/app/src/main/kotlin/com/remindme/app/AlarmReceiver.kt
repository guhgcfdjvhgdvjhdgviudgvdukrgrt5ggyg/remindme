package com.remindme.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Reminder"
        val ringtone = intent.getStringExtra("ringtone") ?: "Default"
        val intentService = Intent(context, AlarmService::class.java)
        intentService.putExtra("title", title)
        intentService.putExtra("ringtone", ringtone)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intentService)
        } else {
            context.startService(intentService)
        }
    }
}
