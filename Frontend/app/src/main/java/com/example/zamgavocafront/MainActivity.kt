package com.example.zamgavocafront

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.zamgavocafront.service.OverlayService
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var tvMorningTime: TextView
    private lateinit var switchMorning: SwitchMaterial
    private lateinit var switchNudge: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvMorningTime = findViewById(R.id.tv_morning_time)
        switchMorning = findViewById(R.id.switch_morning_alarm)
        switchNudge = findViewById(R.id.switch_nudge_alarm)

        setupButtons()
        loadAlarmSettings()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateAlarmPermissionStatus()
        loadAlarmSettings()
    }

    private fun loadAlarmSettings() {
        val (hour, minute) = AlarmScheduler.getMorningAlarmHourMinute(this)
        tvMorningTime.text = "%02d:%02d".format(hour, minute)

        // 리스너 제거 후 상태 세팅 (루프 방지)
        switchMorning.setOnCheckedChangeListener(null)
        switchNudge.setOnCheckedChangeListener(null)

        switchMorning.isChecked = AlarmScheduler.isMorningAlarmEnabled(this)
        switchNudge.isChecked = AlarmScheduler.isNudgeEnabled(this)

        switchMorning.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val (h, m) = AlarmScheduler.getMorningAlarmHourMinute(this)
                AlarmScheduler.scheduleMorningAlarm(this, h, m)
            } else {
                AlarmScheduler.cancelMorningAlarm(this)
            }
        }

        switchNudge.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AlarmScheduler.startNudgeSchedule(this)
            } else {
                AlarmScheduler.stopNudgeSchedule(this)
            }
        }
    }

    private fun updatePermissionStatus() {
        val tv = findViewById<TextView>(R.id.tv_permission_status)
        val btn = findViewById<Button>(R.id.btn_request_permission)
        if (Settings.canDrawOverlays(this)) {
            tv.text = "✅ 오버레이 권한 허용됨"
            tv.setBackgroundColor(0xFFE8F5E9.toInt())
            btn.isEnabled = false
        } else {
            tv.text = "⚠️ 오버레이 권한이 필요합니다 → 버튼을 눌러 설정하세요"
            tv.setBackgroundColor(0xFFFFF3E0.toInt())
            btn.isEnabled = true
        }
    }

    /**
     * Android 12+ 에서 정확한 알람 권한(Alarms & Reminders)이 없으면
     * 백그라운드에서 서비스 시작이 차단됩니다. → 권한 상태를 UI로 안내합니다.
     */
    private fun updateAlarmPermissionStatus() {
        val tv = findViewById<TextView>(R.id.tv_alarm_permission_status)
        val btn = findViewById<Button>(R.id.btn_request_alarm_permission)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (alarmManager.canScheduleExactAlarms()) {
                tv.text = "✅ 정확한 알람 권한 허용됨"
                tv.setBackgroundColor(0xFFE8F5E9.toInt())
                btn.isEnabled = false
            } else {
                tv.text = "⚠️ 정확한 알람 권한 필요 (알람이 작동하지 않을 수 있음) → 설정 이동"
                tv.setBackgroundColor(0xFFFFF3E0.toInt())
                btn.isEnabled = true
            }
        } else {
            tv.text = "✅ 정확한 알람 권한 (Android 11 이하 자동 허용)"
            tv.setBackgroundColor(0xFFE8F5E9.toInt())
            btn.isEnabled = false
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btn_request_permission).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        // Android 12+ 정확한 알람 권한 설정 이동
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            findViewById<Button>(R.id.btn_request_alarm_permission).setOnClickListener {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }

        // 시각 변경 버튼
        findViewById<Button>(R.id.btn_set_morning_time).setOnClickListener {
            val (hour, minute) = AlarmScheduler.getMorningAlarmHourMinute(this)
            TimePickerDialog(this, { _, h, m ->
                tvMorningTime.text = "%02d:%02d".format(h, m)
                if (AlarmScheduler.isMorningAlarmEnabled(this)) {
                    AlarmScheduler.scheduleMorningAlarm(this, h, m)
                } else {
                    AlarmScheduler.saveMorningTime(this, h, m)
                }
            }, hour, minute, true).show()
        }

        // 설정 화면
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 오늘의 단어 화면
        findViewById<Button>(R.id.btn_today_words).setOnClickListener {
            startActivity(Intent(this, TodayWordActivity::class.java))
        }

        // 오버레이 테스트 버튼
        findViewById<Button>(R.id.btn_morning_overlay).setOnClickListener {
            if (!hasOverlayPermission()) return@setOnClickListener
            sendAction(OverlayService.ACTION_SHOW_MORNING)
        }

        findViewById<Button>(R.id.btn_word_list_overlay).setOnClickListener {
            if (!hasOverlayPermission()) return@setOnClickListener
            sendAction(OverlayService.ACTION_SHOW_WORD_LIST)
        }

        findViewById<Button>(R.id.btn_nudge_drag).setOnClickListener {
            if (!hasOverlayPermission()) return@setOnClickListener
            sendAction(OverlayService.ACTION_SHOW_NUDGE_DRAG)
        }

        findViewById<Button>(R.id.btn_nudge_tap).setOnClickListener {
            if (!hasOverlayPermission()) return@setOnClickListener
            sendAction(OverlayService.ACTION_SHOW_NUDGE_TAP)
        }

        findViewById<Button>(R.id.btn_nudge_bounce).setOnClickListener {
            if (!hasOverlayPermission()) return@setOnClickListener
            sendAction(OverlayService.ACTION_SHOW_NUDGE_BOUNCE)
        }

        findViewById<Button>(R.id.btn_stop_service).setOnClickListener {
            sendAction(OverlayService.ACTION_STOP)
        }
    }

    private fun hasOverlayPermission(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            updatePermissionStatus()
            return false
        }
        return true
    }

    private fun sendAction(action: String) {
        val intent = Intent(this, OverlayService::class.java).apply { this.action = action }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
