package com.example.zamgavocafront.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.SkillCache
import com.example.zamgavocafront.api.dto.CollectSkillRequest
import com.example.zamgavocafront.api.dto.CreateQuestionRequest
import com.example.zamgavocafront.api.dto.EvaluateRequest
import com.example.zamgavocafront.api.dto.SkillResponse
import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.pvp.CollectedCardManager
import com.example.zamgavocafront.pvp.PvpWordManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** PvpQuestionActivity의 UI 상태 머신 */
sealed class PvpQuestionUiState {
    object Idle : PvpQuestionUiState()
    object LoadingQuestion : PvpQuestionUiState()
    data class QuestionReady(val koreanSentence: String, val hint: String) : PvpQuestionUiState()
    object Evaluating : PvpQuestionUiState()
    data class Correct(val score: Int, val skill: SkillResponse?) : PvpQuestionUiState()
    data class Wrong(
        val score: Int,
        val feedback: String?,
        val correction: String?
    ) : PvpQuestionUiState()
    data class Error(val message: String, val isEvalError: Boolean = false) : PvpQuestionUiState()
}

class PvpQuestionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PvpQuestionUiState>(PvpQuestionUiState.Idle)
    val uiState: StateFlow<PvpQuestionUiState> = _uiState.asStateFlow()

    private val _attacksLeft = MutableStateFlow(0)
    val attacksLeft: StateFlow<Int> = _attacksLeft.asStateFlow()

    // Intent에서 받은 값들 — onCreate 이후 한 번만 설정됨
    var wordId: Int = 0
    var wordText: String = ""
    var wordMeaning: String = ""
    var skillId: Long? = null
    var difficulty: Difficulty = Difficulty.MEDIUM
    var userLevel: String = "intermediate"

    private var koreanSentence = ""
    private var idealTranslation = ""

    fun refreshAttacks() {
        _attacksLeft.value = PvpWordManager.getAttacksLeft(getApplication())
    }

    fun loadQuestion() {
        // Idle/Error 상태에서만 로드 — 화면 회전 시 재호출 방지
        val current = _uiState.value
        if (current !is PvpQuestionUiState.Idle && current !is PvpQuestionUiState.Error) return
        _uiState.value = PvpQuestionUiState.LoadingQuestion
        viewModelScope.launch {
            try {
                val response = ApiClient.mockApi.createQuestion(
                    CreateQuestionRequest(targetWord = wordText, userLevel = userLevel)
                )
                if (response.success) {
                    koreanSentence = response.koreanSentence ?: ""
                    idealTranslation = response.ideal ?: ""
                    _uiState.value = PvpQuestionUiState.QuestionReady(
                        koreanSentence = koreanSentence,
                        hint = response.wordHint?.let { "힌트: $it" } ?: ""
                    )
                } else {
                    _uiState.value = PvpQuestionUiState.Error("문제 생성 실패: ${response.error}")
                }
            } catch (e: Exception) {
                _uiState.value = PvpQuestionUiState.Error(
                    "네트워크 오류: ${e.message}\n\n(백엔드 서버가 실행 중인지 확인하세요)"
                )
            }
        }
    }

    fun submitAnswer(userAnswer: String) {
        if (userAnswer.isBlank() || koreanSentence.isEmpty()) return
        // 이미 채점 중이면 중복 제출 방지
        if (_uiState.value is PvpQuestionUiState.Evaluating) return

        _uiState.value = PvpQuestionUiState.Evaluating
        val context = getApplication<Application>()
        PvpWordManager.consumeAttack(context)
        refreshAttacks()

        viewModelScope.launch {
            try {
                val evalResponse = ApiClient.mockApi.evaluate(
                    EvaluateRequest(
                        koreanSentence = koreanSentence,
                        userAnswer = userAnswer,
                        idealTranslation = idealTranslation,
                        targetWord = wordText,
                        userLevel = userLevel
                    )
                )
                val score = evalResponse.score ?: 0
                if (score >= PASS_SCORE) {
                    handleCorrect(score)
                } else {
                    _uiState.value = PvpQuestionUiState.Wrong(
                        score = score,
                        feedback = evalResponse.feedback,
                        correction = evalResponse.correction
                    )
                }
            } catch (e: Exception) {
                _uiState.value = PvpQuestionUiState.Error(
                    message = "채점 오류: ${e.message}",
                    isEvalError = true
                )
                refreshAttacks()
            }
        }
    }

    private suspend fun handleCorrect(score: Int) {
        val context = getApplication<Application>()
        PvpWordManager.markWordUsed(context, wordId)

        val skill = runCatching {
            SkillCache.fetchOrGenerate(wordId, skillId, wordText, wordMeaning)
        }.getOrNull()

        if (skill != null) {
            PvpWordManager.addDamage(context, skill.damage)
            CollectedCardManager.addCard(
                context,
                CollectedCardManager.CollectedCard(
                    wordId = wordId,
                    word = wordText,
                    skillName = skill.name,
                    skillDescription = skill.explain,
                    damage = skill.damage,
                    imageBase64 = null,
                    grade = difficulty.grade(),
                    imageUrl = skill.imageURL.takeIf { it.isNotBlank() }
                )
            )
            runCatching {
                ApiClient.api.collectSkill(
                    CollectSkillRequest(skillId = skill.skillId, wordId = wordId.toLong())
                )
            }
        } else {
            PvpWordManager.addDamage(context, 50)
        }

        _uiState.value = PvpQuestionUiState.Correct(score = score, skill = skill)
    }

    /**
     * Correct 상태를 소비(consume)해 Idle로 리셋한다.
     * StateFlow가 앱 복귀 시 마지막 값을 재방출할 때 다이얼로그가 중복 표시되는 것을 방지.
     */
    fun onCorrectConsumed() {
        if (_uiState.value is PvpQuestionUiState.Correct) {
            _uiState.value = PvpQuestionUiState.Idle
        }
    }

    companion object {
        private const val PASS_SCORE = 60
    }
}
