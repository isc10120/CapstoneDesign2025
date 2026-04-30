package com.example.zamgavocafront

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.zamgavocafront.viewmodel.SignUpUiState
import com.example.zamgavocafront.viewmodel.SignUpViewModel
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etNickname = findViewById<EditText>(R.id.et_nickname)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnSignup = findViewById<Button>(R.id.btn_signup)
        val btnBack = findViewById<Button>(R.id.btn_back_to_login)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SignUpUiState.Idle -> {
                            btnSignup.isEnabled = true
                            btnSignup.text = "회원가입"
                        }
                        is SignUpUiState.Loading -> {
                            btnSignup.isEnabled = false
                            btnSignup.text = "처리 중..."
                        }
                        is SignUpUiState.Success -> {
                            Toast.makeText(this@SignUpActivity, "회원가입이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is SignUpUiState.Error -> {
                            Toast.makeText(this@SignUpActivity, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetIdle()
                        }
                    }
                }
            }
        }

        btnSignup.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val nickname = etNickname.text.toString().trim()
            val password = etPassword.text.toString()
            if (email.isEmpty() || nickname.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.signUp(email, password, nickname)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
