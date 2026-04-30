package com.example.zamgavocafront

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.zamgavocafront.viewmodel.LoginUiState
import com.example.zamgavocafront.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.et_username)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> {
                            btnLogin.isEnabled = true
                            btnLogin.text = "로그인"
                        }
                        is LoginUiState.Loading -> {
                            btnLogin.isEnabled = false
                            btnLogin.text = "로그인 중..."
                        }
                        is LoginUiState.Success -> {
                            saveUserPrefs(state)
                            applyNudgeSettings(state)
                            startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                            finish()
                        }
                        is LoginUiState.Error -> {
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetIdle()
                        }
                    }
                }
            }
        }

        btnLogin.setOnClickListener {
            val email = etUsername.text.toString().trim()
            val password = etPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.signIn(email, password)
        }

        findViewById<TextView>(R.id.tv_signup).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        findViewById<TextView>(R.id.tv_find_password).setOnClickListener {
            Toast.makeText(this, "비밀번호 찾기 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserPrefs(data: LoginUiState.Success) {
        getSharedPreferences("user_prefs", MODE_PRIVATE)
            .edit()
            .putLong("userId", data.userId)
            .putString("nickName", data.nickName)
            .putString("email", data.email)
            .apply()
    }

    private fun applyNudgeSettings(data: LoginUiState.Success) {
        if (data.nudgeEnabled) {
            AlarmScheduler.saveNudgeInterval(this, data.nudgeInterval, data.nudgeInterval)
            AlarmScheduler.startNudgeSchedule(this)
        } else {
            AlarmScheduler.stopNudgeSchedule(this)
        }
    }
}
