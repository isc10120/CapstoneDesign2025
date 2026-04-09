package com.example.zamgavocafront

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.dto.SignUpRequest
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etNickname = findViewById<EditText>(R.id.et_nickname)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnSignup = findViewById<Button>(R.id.btn_signup)
        val btnBack = findViewById<Button>(R.id.btn_back_to_login)

        btnSignup.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val nickname = etNickname.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || nickname.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignup.isEnabled = false
            btnSignup.text = "처리 중..."

            lifecycleScope.launch {
                try {
                    val response = ApiClient.authApi.signUp(
                        SignUpRequest(email = email, password = password, nickName = nickname)
                    )
                    if (response.success) {
                        Toast.makeText(this@SignUpActivity, "회원가입이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val msg = response.error?.message ?: "회원가입에 실패했습니다."
                        Toast.makeText(this@SignUpActivity, msg, Toast.LENGTH_LONG).show()
                        resetButton(btnSignup)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@SignUpActivity, "서버 연결에 실패했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                    resetButton(btnSignup)
                }
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun resetButton(btn: Button) {
        btn.isEnabled = true
        btn.text = "회원가입"
    }
}
