package com.example.zamgavocafront

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 로그인 버튼 → HomeActivity (로그인 시스템 구현 전까지 형식상 바로 이동)
        findViewById<Button>(R.id.btn_login).setOnClickListener {
            goToHome()
        }

        // 게스트로 시작하기
        findViewById<TextView>(R.id.tv_guest).setOnClickListener {
            goToHome()
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
