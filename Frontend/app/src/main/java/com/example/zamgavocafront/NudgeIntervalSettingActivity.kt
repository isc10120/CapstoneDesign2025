package com.example.zamgavocafront

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NudgeIntervalSettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nudge_interval_setting)

        val etMin = findViewById<EditText>(R.id.et_nudge_interval_min)
        val etMax = findViewById<EditText>(R.id.et_nudge_interval_max)

        val (min, max) = AlarmScheduler.getNudgeIntervalMinutes(this)
        etMin.setText(min.toString())
        etMax.setText(max.toString())

        findViewById<Button>(R.id.btn_nudge_interval_save).setOnClickListener {
            val minVal = etMin.text.toString().toIntOrNull()
            val maxVal = etMax.text.toString().toIntOrNull()
            if (minVal == null || maxVal == null || minVal < 1 || maxVal < 1 || minVal > maxVal) {
                Toast.makeText(this, "올바른 값을 입력하세요 (최솟값 ≤ 최댓값, 1분 이상)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlarmScheduler.saveNudgeInterval(this, minVal, maxVal)
            Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btn_nudge_interval_back).setOnClickListener {
            finish()
        }
    }
}
