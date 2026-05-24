package com.example.zamgavocafront.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.zamgavocafront.AlarmScheduler
import com.example.zamgavocafront.DndAppsManager
import com.example.zamgavocafront.service.OverlayService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.example.zamgavocafront.MORNING_ALARM" -> {
                startOverlayService(context, OverlayService.ACTION_SHOW_MORNING)
                if (AlarmScheduler.isMorningAlarmEnabled(context)) {
                    val (hour, minute) = AlarmScheduler.getMorningAlarmHourMinute(context)
                    AlarmScheduler.scheduleMorningAlarm(context, hour, minute)
                }
            }
            "com.example.zamgavocafront.NUDGE_ALARM" -> {
                if (!DndAppsManager.isForegroundAppBlocked(context)) {
                    startOverlayService(context, OverlayService.ACTION_SHOW_NUDGE_RANDOM)
                }
                if (AlarmScheduler.isNudgeEnabled(context)) {
                    AlarmScheduler.scheduleNextNudgeAlarm(context)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                if (AlarmScheduler.isMorningAlarmEnabled(context)) {
                    val (hour, minute) = AlarmScheduler.getMorningAlarmHourMinute(context)
                    AlarmScheduler.scheduleMorningAlarm(context, hour, minute)
                }
                if (AlarmScheduler.isNudgeEnabled(context)) {
                    AlarmScheduler.scheduleNextNudgeAlarm(context)
                    startOverlayService(context, OverlayService.ACTION_START_NUDGE_SCHEDULE)
                }
            }
            else -> return
        }
    }

    private fun startOverlayService(context: Context, action: String) {
        val serviceIntent = Intent(context, OverlayService::class.java).apply {
            this.action = action
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) { }
    }
}
