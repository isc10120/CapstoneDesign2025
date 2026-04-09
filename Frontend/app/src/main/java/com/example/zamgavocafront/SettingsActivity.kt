package com.example.zamgavocafront

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

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvMorningTime: TextView
    private lateinit var switchMorning: SwitchMaterial
    private lateinit var switchNudge: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tvMorningTime = findViewById(R.id.tv_morning_time)
        switchMorning = findViewById(R.id.switch_morning_alarm)
        switchNudge = findViewById(R.id.switch_nudge_alarm)

        setupNavigationButtons()
        setupAlarmControls()
        setupOverlayTestButtons()
        loadAlarmSettings()
    }

    override fun onResume() {
        super.onResume()
        loadAlarmSettings()
    }

    private fun loadAlarmSettings() {
        val (hour, minute) = AlarmScheduler.getMorningAlarmHourMinute(this)
        tvMorningTime.text = "%02d:%02d".format(hour, minute)

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
                sendAction(OverlayService.ACTION_START_NUDGE_SCHEDULE)
            } else {
                AlarmScheduler.stopNudgeSchedule(this)
                sendAction(OverlayService.ACTION_STOP_NUDGE_SCHEDULE)
            }
        }
    }

    private fun setupNavigationButtons() {
        findViewById<Button>(R.id.btn_setting_morning_time).setOnClickListener {
            startActivity(Intent(this, MorningTimeSettingActivity::class.java))
        }
        findViewById<Button>(R.id.btn_setting_dnd_apps).setOnClickListener {
            startActivity(Intent(this, DndAppsSettingActivity::class.java))
        }
        findViewById<Button>(R.id.btn_setting_nudge_interval).setOnClickListener {
            startActivity(Intent(this, NudgeIntervalSettingActivity::class.java))
        }
        findViewById<Button>(R.id.btn_settings_back).setOnClickListener {
            finish()
        }
    }

    private fun setupAlarmControls() {
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
    }

    private fun setupOverlayTestButtons() {
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
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
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
