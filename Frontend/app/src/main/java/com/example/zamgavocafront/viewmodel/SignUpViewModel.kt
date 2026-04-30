package com.example.zamgavocafront.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.dto.SignUpRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SignUpUiState {
    object Idle : SignUpUiState()
    object Loading : SignUpUiState()
    object Success : SignUpUiState()
    data class Error(val message: String) : SignUpUiState()
}

class SignUpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String, nickname: String) {
        if (_uiState.value is SignUpUiState.Loading) return
        _uiState.value = SignUpUiState.Loading
        viewModelScope.launch {
            try {
                val response = ApiClient.authApi.signUp(
                    SignUpRequest(email = email, password = password, nickName = nickname)
                )
                if (response.success) {
                    _uiState.value = SignUpUiState.Success
                } else {
                    val msg = response.error?.message ?: "회원가입에 실패했습니다."
                    _uiState.value = SignUpUiState.Error(msg)
                }
            } catch (e: Exception) {
                _uiState.value = SignUpUiState.Error("서버 연결에 실패했습니다: ${e.message}")
            }
        }
    }

    fun resetIdle() {
        _uiState.value = SignUpUiState.Idle
    }
}
