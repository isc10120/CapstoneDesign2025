package com.example.zamgavocafront

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.dto.SignInRequest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.et_username)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)

        btnLogin.setOnClickListener {
            val email = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "로그인 중..."

            lifecycleScope.launch {
                try {
                    val response = ApiClient.authApi.signIn(
                        SignInRequest(email = email, password = password)
                    )
                    if (response.success && response.data != null) {
                        // 사용자 정보 저장
                        getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                            .putLong("userId", response.data.userId)
                            .putString("nickName", response.data.nickName)
                            .putString("email", response.data.email)
                            .apply()
                        goToHome()
                    } else {
                        val msg = response.error?.message ?: "로그인에 실패했습니다."
                        Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
                        resetButton(btnLogin)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "서버 연결에 실패했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                    resetButton(btnLogin)
                }
            }
        }

        // 회원가입 화면으로 이동
        findViewById<TextView>(R.id.tv_signup).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // 비밀번호 찾기 (미구현)
        findViewById<TextView>(R.id.tv_find_password).setOnClickListener {
            Toast.makeText(this, "비밀번호 찾기 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun resetButton(btn: Button) {
        btn.isEnabled = true
        btn.text = "로그인"
    }
}
