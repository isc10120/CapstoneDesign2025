package com.example.zamgavocafront

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.btn_setting_morning_time).setOnClickListener {
            startActivity(Intent(this, MorningTimeSettingActivity::class.java))
        }
        findViewById<Button>(R.id.btn_setting_dnd_apps).setOnClickListener {
            startActivity(Intent(this, DndAppsSettingActivity::class.java))
        }
        findViewById<Button>(R.id.btn_setting_nudge_interval).setOnClickListener {
            startActivity(Intent(this, NudgeIntervalSettingActivity::class.java))
        }
        findViewById<Button>(R.id.btn_overlay_test).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<Button>(R.id.btn_settings_back).setOnClickListener {
            finish()
        }
    }
}
