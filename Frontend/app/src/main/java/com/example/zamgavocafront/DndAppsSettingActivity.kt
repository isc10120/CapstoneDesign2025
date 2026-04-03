package com.example.zamgavocafront

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DndAppsSettingActivity : AppCompatActivity() {

    private lateinit var adapter: DndAppsAdapter
    private lateinit var checkedPackages: MutableSet<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dnd_apps_setting)

        checkedPackages = DndAppsManager.getDndPackages(this).toMutableSet()

        updatePermissionStatus()

        findViewById<Button>(R.id.btn_request_usage_permission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        val apps = DndAppsManager.getInstalledApps(this)
        adapter = DndAppsAdapter(apps, checkedPackages)
        val rv = findViewById<RecyclerView>(R.id.rv_apps)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<Button>(R.id.btn_dnd_save).setOnClickListener {
            DndAppsManager.setDndPackages(this, checkedPackages)
            finish()
        }
        findViewById<Button>(R.id.btn_dnd_back).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val tv = findViewById<TextView>(R.id.tv_usage_permission_status)
        val btn = findViewById<Button>(R.id.btn_request_usage_permission)
        if (DndAppsManager.hasUsageStatsPermission(this)) {
            tv.text = "✅ 앱 사용 기록 권한 허용됨"
            tv.setBackgroundColor(0xFFE8F5E9.toInt())
            btn.isEnabled = false
        } else {
            tv.text = "⚠️ 앱 사용 기록 접근 권한이 필요합니다. 아래 버튼을 눌러 권한을 허용하세요."
            tv.setBackgroundColor(0xFFFFF3E0.toInt())
            btn.isEnabled = true
        }
    }
}
