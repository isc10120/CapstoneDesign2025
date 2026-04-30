package com.example.zamgavocafront.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.dto.CreateQuestionRequest
import com.example.zamgavocafront.api.dto.EvaluateRequest
import com.example.zamgavocafront.pvp.CollectedCardManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** PveQuestionActivity의 UI 상태 머신 */
sealed class PveQuestionUiState {
    object Idle : PveQuestionUiState()
    object LoadingQuestion : PveQuestionUiState()
    data class QuestionReady(val koreanSentence: String, val hint: String) : PveQuestionUiState()
    object Evaluating : PveQuestionUiState()
    data class Correct(
        val score: Int,
        val imageUrl: String?,
        val imageBase64: String?
    ) : PveQuestionUiState()
    data class Wrong(
        val score: Int,
        val feedback: String?,
        val correction: String?
    ) : PveQuestionUiState()
    data class Error(val message: String) : PveQuestionUiState()
}

class PveQuestionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PveQuestionUiState>(PveQuestionUiState.Idle)
    val uiState: StateFlow<PveQuestionUiState> = _uiState.asStateFlow()

    // Intent에서 받은 값들 — onCreate 이후 한 번만 설정됨
    var wordId: Int = 0
    var wordText: String = ""
    var userLevel: String = "intermediate"

    private var koreanSentence = ""
    private var idealTranslation = ""

    fun loadQuestion() {
        // Idle/Error 상태에서만 로드 — 화면 회전 시 재호출 방지
        val current = _uiState.value
        if (current !is PveQuestionUiState.Idle && current !is PveQuestionUiState.Error) return
        _uiState.value = PveQuestionUiState.LoadingQuestion
        viewModelScope.launch {
            try {
                val resp = ApiClient.mockApi.createQuestion(
                    CreateQuestionRequest(targetWord = wordText, userLevel = userLevel)
                )
                if (resp.success) {
                    koreanSentence = resp.koreanSentence ?: ""
                    idealTranslation = resp.ideal ?: ""
                    _uiState.value = PveQuestionUiState.QuestionReady(
                        koreanSentence = koreanSentence,
                        hint = resp.wordHint?.let { "힌트: $it" } ?: ""
                    )
                } else {
                    _uiState.value = PveQuestionUiState.Error("문제 생성 실패: ${resp.error}")
                }
            } catch (e: Exception) {
                _uiState.value = PveQuestionUiState.Error("오류: ${e.message}")
            }
        }
    }

    fun submitAnswer(answer: String) {
        if (answer.isBlank() || koreanSentence.isEmpty()) return
        if (_uiState.value is PveQuestionUiState.Evaluating) return
        _uiState.value = PveQuestionUiState.Evaluating
        viewModelScope.launch {
            try {
                val evalResp = ApiClient.mockApi.evaluate(
                    EvaluateRequest(
                        koreanSentence = koreanSentence,
                        userAnswer = answer,
                        idealTranslation = idealTranslation,
                        targetWord = wordText,
                        userLevel = userLevel
                    )
                )
                val score = evalResp.score ?: 0
                if (score >= PASS_SCORE) {
                    // 이미 수집된 카드에서 이미지 정보를 조회
                    val existing = CollectedCardManager.getCards(getApplication())
                        .find { it.wordId == wordId }
                    _uiState.value = PveQuestionUiState.Correct(
                        score = score,
                        imageUrl = existing?.imageUrl,
                        imageBase64 = existing?.imageBase64
                    )
                } else {
                    _uiState.value = PveQuestionUiState.Wrong(
                        score = score,
                        feedback = evalResp.feedback,
                        correction = evalResp.correction
                    )
                }
            } catch (e: Exception) {
                _uiState.value = PveQuestionUiState.Error("채점 오류: ${e.message}")
            }
        }
    }

    /** Correct 상태를 소비해 Idle로 리셋 — 다이얼로그 중복 표시 방지 */
    fun onCorrectConsumed() {
        if (_uiState.value is PveQuestionUiState.Correct) {
            _uiState.value = PveQuestionUiState.Idle
        }
    }

    companion object {
        private const val PASS_SCORE = 60
    }
}
