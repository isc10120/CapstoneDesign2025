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
                // 다음 날 같은 시각으로 재등록
                if (AlarmScheduler.isMorningAlarmEnabled(context)) {
                    val (hour, minute) = AlarmScheduler.getMorningAlarmHourMinute(context)
                    AlarmScheduler.scheduleMorningAlarm(context, hour, minute)
                }
            }
            "com.example.zamgavocafront.NUDGE_ALARM" -> {
                // 방해금지 앱 사용 중이 아닐 때만 오버레이 표시
                if (!DndAppsManager.isForegroundAppBlocked(context)) {
                    startOverlayService(context, OverlayService.ACTION_SHOW_NUDGE_RANDOM)
                }
                if (AlarmScheduler.isNudgeEnabled(context)) {
                    AlarmScheduler.scheduleNextNudgeAlarm(context)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // 기기 재부팅 시 알람 복원
                if (AlarmScheduler.isMorningAlarmEnabled(context)) {
                    val (hour, minute) = AlarmScheduler.getMorningAlarmHourMinute(context)
                    AlarmScheduler.scheduleMorningAlarm(context, hour, minute)
                }
                if (AlarmScheduler.isNudgeEnabled(context)) {
                    AlarmScheduler.scheduleNextNudgeAlarm(context)
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
        } catch (e: Exception) {
            // Android 12+에서 Alarms & Reminders 권한 없이 백그라운드 FGS 시작 시 실패할 수 있음
            // MainActivity의 권한 안내 UI를 통해 사용자가 권한을 부여해야 함
        }
    }
}
