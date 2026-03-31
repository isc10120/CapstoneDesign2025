package com.example.zamgavocafront

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.zamgavocafront.service.OverlayService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
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

    private fun setupButtons() {
        findViewById<Button>(R.id.btn_request_permission).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

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
