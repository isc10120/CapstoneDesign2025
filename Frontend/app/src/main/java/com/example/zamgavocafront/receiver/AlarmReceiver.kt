package com.example.zamgavocafront.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.zamgavocafront.service.OverlayService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = when (intent.action) {
            "com.example.zamgavocafront.MORNING_ALARM" -> OverlayService.ACTION_SHOW_MORNING
            "com.example.zamgavocafront.NUDGE_ALARM" -> OverlayService.ACTION_SHOW_NUDGE_RANDOM
            Intent.ACTION_BOOT_COMPLETED -> null  // TODO: re-schedule alarms after reboot
            else -> return
        } ?: return

        val serviceIntent = Intent(context, OverlayService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
