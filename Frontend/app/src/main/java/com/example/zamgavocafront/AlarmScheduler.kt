package com.example.zamgavocafront

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.zamgavocafront.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    private const val PREFS_NAME = "alarm_settings"
    private const val KEY_MORNING_HOUR = "morning_hour"
    private const val KEY_MORNING_MINUTE = "morning_minute"
    private const val KEY_MORNING_ENABLED = "morning_enabled"
    private const val KEY_NUDGE_ENABLED = "nudge_enabled"
    private const val KEY_NUDGE_INTERVAL_MIN = "nudge_interval_min"
    private const val KEY_NUDGE_INTERVAL_MAX = "nudge_interval_max"

    private const val MORNING_REQUEST_CODE = 1001
    private const val NUDGE_REQUEST_CODE = 1002

    // ─── 넛지 등장 간격 설정 ────────────────────────────────────────────────
    // 이 두 상수를 수정해서 넛지 위젯이 뜨는 주기를 조절하세요.
    const val NUDGE_INTERVAL_MIN_MINUTES = 1   // 최소 간격 (분)
    const val NUDGE_INTERVAL_MAX_MINUTES = 3   // 최대 간격 (분)
    // ────────────────────────────────────────────────────────────────────────

    // ── 아침 알람 ─────────────────────────────────────────────────────────

    /** 시각만 저장하고 알람은 등록하지 않는다. (알람이 꺼진 상태에서 시각만 미리 설정할 때 사용) */
    fun saveMorningTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_MORNING_HOUR, hour)
            .putInt(KEY_MORNING_MINUTE, minute)
            .apply()
    }

    fun scheduleMorningAlarm(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_MORNING_HOUR, hour)
            .putInt(KEY_MORNING_MINUTE, minute)
            .putBoolean(KEY_MORNING_ENABLED, true)
            .apply()

        val triggerMillis = nextDailyTriggerMillis(hour, minute)
        setExactAlarm(context, MORNING_REQUEST_CODE, "com.example.zamgavocafront.MORNING_ALARM", triggerMillis)
    }

    fun cancelMorningAlarm(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_MORNING_ENABLED, false)
            .apply()
        cancelAlarm(context, MORNING_REQUEST_CODE, "com.example.zamgavocafront.MORNING_ALARM")
    }

    fun isMorningAlarmEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MORNING_ENABLED, false)

    fun getMorningAlarmHourMinute(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Pair(prefs.getInt(KEY_MORNING_HOUR, 7), prefs.getInt(KEY_MORNING_MINUTE, 0))
    }

    // ── 넛지 알람 ─────────────────────────────────────────────────────────

    fun startNudgeSchedule(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_NUDGE_ENABLED, true)
            .apply()
        scheduleNextNudgeAlarm(context)
    }

    fun stopNudgeSchedule(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_NUDGE_ENABLED, false)
            .apply()
        cancelAlarm(context, NUDGE_REQUEST_CODE, "com.example.zamgavocafront.NUDGE_ALARM")
    }

    fun isNudgeEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NUDGE_ENABLED, false)

    fun saveNudgeInterval(context: Context, minMinutes: Int, maxMinutes: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_NUDGE_INTERVAL_MIN, minMinutes)
            .putInt(KEY_NUDGE_INTERVAL_MAX, maxMinutes)
            .apply()
    }

    fun getNudgeIntervalMinutes(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Pair(
            prefs.getInt(KEY_NUDGE_INTERVAL_MIN, NUDGE_INTERVAL_MIN_MINUTES),
            prefs.getInt(KEY_NUDGE_INTERVAL_MAX, NUDGE_INTERVAL_MAX_MINUTES)
        )
    }

    /** 다음 넛지 알람을 랜덤 간격 후로 예약한다. AlarmReceiver에서도 호출된다. */
    fun scheduleNextNudgeAlarm(context: Context) {
        val (min, max) = getNudgeIntervalMinutes(context)
        val range = if (min >= max) min..min else min..max
        val delayMs = range.random() * 60 * 1000L
        val triggerMillis = System.currentTimeMillis() + delayMs
        setExactAlarm(context, NUDGE_REQUEST_CODE, "com.example.zamgavocafront.NUDGE_ALARM", triggerMillis)
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────

    /**
     * 오늘 또는 내일 해당 시각의 Unix 밀리초를 반환한다.
     * 이미 지난 시각이면 자동으로 내일로 설정된다.
     */
    private fun nextDailyTriggerMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    private fun setExactAlarm(context: Context, requestCode: Int, action: String, triggerMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, requestCode, action)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    private fun cancelAlarm(context: Context, requestCode: Int, action: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, requestCode, action))
    }

    private fun buildPendingIntent(context: Context, requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
