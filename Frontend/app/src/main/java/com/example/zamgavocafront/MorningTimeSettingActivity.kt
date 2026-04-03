package com.example.zamgavocafront

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MorningTimeSettingActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_morning_time_setting)

        tvTime = findViewById(R.id.tv_morning_time_setting)
        updateTimeDisplay()

        findViewById<Button>(R.id.btn_change_morning_time).setOnClickListener {
            val (hour, minute) = AlarmScheduler.getMorningAlarmHourMinute(this)
            TimePickerDialog(this, { _, h, m ->
                if (AlarmScheduler.isMorningAlarmEnabled(this)) {
                    AlarmScheduler.scheduleMorningAlarm(this, h, m)
                } else {
                    AlarmScheduler.saveMorningTime(this, h, m)
                }
                updateTimeDisplay()
            }, hour, minute, true).show()
        }

        findViewById<Button>(R.id.btn_morning_time_back).setOnClickListener {
            finish()
        }
    }

    private fun updateTimeDisplay() {
        val (h, m) = AlarmScheduler.getMorningAlarmHourMinute(this)
        tvTime.text = "%02d:%02d".format(h, m)
    }
}
