package com.example.zamgavocafront.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.dto.SignInRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(
        val userId: Long,
        val nickName: String,
        val email: String,
        val nudgeEnabled: Boolean,
        val nudgeInterval: Int
    ) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (_uiState.value is LoginUiState.Loading) return
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.signIn(SignInRequest(email = email, password = password))
                if (response.success && response.data != null) {
                    _uiState.value = LoginUiState.Success(
                        userId = response.data.userId,
                        nickName = response.data.nickName,
                        email = response.data.email,
                        nudgeEnabled = response.data.nudgeEnabled,
                        nudgeInterval = response.data.nudgeInterval
                    )
                } else {
                    _uiState.value = LoginUiState.Error(response.error?.message ?: "로그인에 실패했습니다.")
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("서버 연결에 실패했습니다: ${e.message}")
            }
        }
    }

    fun resetIdle() {
        _uiState.value = LoginUiState.Idle
    }
}
