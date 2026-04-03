package com.example.zamgavocafront

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private var waitingForPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 1.2초 스플래시 후 권한 체크
        Handler(Looper.getMainLooper()).postDelayed({ checkAndProceed() }, 1200)
    }

    override fun onResume() {
        super.onResume()
        // 권한 설정 화면에서 돌아왔을 때 재확인
        if (waitingForPermission) {
            waitingForPermission = false
            checkAndProceed()
        }
    }

    private fun checkAndProceed() {
        val missing = getMissingPermissions()
        if (missing.isEmpty()) {
            goToLogin()
        } else {
            showPermissionDialog(missing)
        }
    }

    private fun getMissingPermissions(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        if (!Settings.canDrawOverlays(this)) {
            list.add("오버레이 표시 권한" to "다른 앱 위에 단어 위젯을 표시하는 데 필요합니다.")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                list.add("정확한 알람 권한" to "정해진 시각에 알람을 울리는 데 필요합니다.")
            }
        }
        return list
    }

    private fun showPermissionDialog(missing: List<Pair<String, String>>) {
        val msgBody = missing.joinToString("\n\n") { (title, desc) -> "• $title\n  $desc" }
        val message = "앱을 원활하게 사용하려면\n다음 권한이 필요합니다:\n\n$msgBody"

        AlertDialog.Builder(this)
            .setTitle("권한 설정 안내")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("권한 설정하기") { _, _ ->
                waitingForPermission = true
                openNextMissingPermission()
            }
            .setNegativeButton("나중에 설정") { _, _ -> goToLogin() }
            .show()
    }

    private fun openNextMissingPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
