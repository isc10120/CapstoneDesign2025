package com.example.zamgavocafront.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.SkillCache
import com.example.zamgavocafront.api.dto.CreateQuestionRequest
import com.example.zamgavocafront.api.dto.EvaluateRequest
import com.example.zamgavocafront.api.dto.PvpSkillRequest
import com.example.zamgavocafront.api.dto.SkillResponse
import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.pvp.CollectedCardManager
import com.example.zamgavocafront.pvp.PvpWordManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PvpQuestionUiState {
    object Idle : PvpQuestionUiState()
    object LoadingQuestion : PvpQuestionUiState()
    data class QuestionReady(val koreanSentence: String, val hint: String) : PvpQuestionUiState()
    object Evaluating : PvpQuestionUiState()
    data class Correct(
        val score: Int,
        val skill: SkillResponse?,
        val pvpDamage: Int? = null,
        val effectType: String? = null,
        val effectTurns: Int? = null
    ) : PvpQuestionUiState()
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
        val current = _uiState.value
        if (current !is PvpQuestionUiState.Idle && current !is PvpQuestionUiState.Error) return
        _uiState.value = PvpQuestionUiState.LoadingQuestion
        viewModelScope.launch {
            try {
                // 문제생성 API 미구현 → Mock 사용
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
        if (_uiState.value is PvpQuestionUiState.Evaluating) return

        _uiState.value = PvpQuestionUiState.Evaluating
        val context = getApplication<Application>()
        PvpWordManager.consumeAttack(context)
        refreshAttacks()

        viewModelScope.launch {
            try {
                // 채점 API 미구현 → Mock 사용
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

        // 스킬 이미지/상세 정보 (다이얼로그 표시용)
        val skill = runCatching {
            SkillCache.fetchOrGenerate(wordId, skillId, wordText, wordMeaning)
        }.getOrNull()

        // pvp/skill 호출 — 서버에서 데미지 적용 + 스킬 수집 처리
        var pvpDamage: Int? = null
        var effectType: String? = null
        var effectTurns: Int? = null
        if (skillId != null) {
            val pvpResp = runCatching {
                ApiClient.api.usePvpSkill(
                    PvpSkillRequest(skillId = skillId!!, wordId = wordId.toLong())
                )
            }.getOrNull()?.data
            pvpDamage = pvpResp?.damageDealt
            effectType = pvpResp?.statusApplied?.type
            effectTurns = pvpResp?.statusApplied?.turns
        }

        // 로컬 데미지 누적 (다이얼로그 총 데미지 표시용)
        PvpWordManager.addDamage(context, pvpDamage ?: skill?.damage ?: 50)

        if (skill != null) {
            CollectedCardManager.addCard(
                context,
                CollectedCardManager.CollectedCard(
                    wordId = wordId,
                    word = wordText,
                    skillName = skill.name,
                    skillDescription = skill.explain,
                    damage = skill.damage,
                    imageBase64 = skill.imageBase64,
                    grade = difficulty.grade(),
                    imageUrl = skill.imageURL.takeIf { it.isNotBlank() },
                    wordMeaning = wordMeaning
                )
            )
        }

        _uiState.value = PvpQuestionUiState.Correct(
            score = score,
            skill = skill,
            pvpDamage = pvpDamage,
            effectType = effectType,
            effectTurns = effectTurns
        )
    }

    fun onCorrectConsumed() {
        if (_uiState.value is PvpQuestionUiState.Correct) {
            _uiState.value = PvpQuestionUiState.Idle
        }
    }

    companion object {
        private const val PASS_SCORE = 60
    }
}
