package com.example.zamgavocafront.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.SkillCache
import com.example.zamgavocafront.api.dto.EvaluateNewRequest
import com.example.zamgavocafront.api.dto.PvpSkillRequest
import com.example.zamgavocafront.api.dto.PvpSkillResponse
import com.example.zamgavocafront.api.dto.QuestionRequest
import com.example.zamgavocafront.api.dto.QuestionResponse
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
    data class QuestionReady(
        val question: String,
        val hint: String?,
        val questionType: String,
        val options: List<String>? = null
    ) : PvpQuestionUiState()
    object Evaluating : PvpQuestionUiState()
    data class Correct(
        val score: Int,
        val skill: SkillResponse?,
        val pvpDamage: Int? = null,
        val effectType: String? = null,
        val effectTurns: Int? = null,
        val shieldBlocked: Boolean = false,
        val poisonDamageTaken: Int = 0,
        val paralyzed: Boolean = false,
        val dominantColor: String? = null
    ) : PvpQuestionUiState()
    data class Wrong(
        val score: Int,
        val feedback: String?,
        val correction: String?,
        val poisonDamageTaken: Int = 0
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
    var partOfSpeech: String = ""

    private var currentQuestion: QuestionResponse? = null

    fun refreshAttacks() {
        _attacksLeft.value = PvpWordManager.getAttacksLeft(getApplication())
    }

    fun loadQuestion() {
        val current = _uiState.value
        if (current !is PvpQuestionUiState.Idle && current !is PvpQuestionUiState.Error) return
        _uiState.value = PvpQuestionUiState.LoadingQuestion
        viewModelScope.launch {
            val isNoun = partOfSpeech.contains("noun", ignoreCase = true) ||
                         partOfSpeech.contains("명사", ignoreCase = true)
            val availableTypes = if (isNoun) QUESTION_TYPES.filter { it != "synonym" } else QUESTION_TYPES
            var lastError: String? = null
            for (questionType in availableTypes.shuffled()) {
                try {
                    val response = ApiClient.api.generateQuestion(questionType, QuestionRequest(wordId.toLong())).data
                        ?: continue
                    currentQuestion = response
                    _uiState.value = PvpQuestionUiState.QuestionReady(
                        question = buildQuestionText(response),
                        hint = buildHintText(response),
                        questionType = response.questionType,
                        options = response.options
                    )
                    return@launch
                } catch (e: Exception) {
                    lastError = e.message
                }
            }
            _uiState.value = PvpQuestionUiState.Error(
                "문제 생성 실패: $lastError\n\n(백엔드 서버가 실행 중인지 확인하세요)"
            )
        }
    }

    private fun buildQuestionText(q: QuestionResponse): String = buildString {
        append(q.question)
        if (!q.blankedWord.isNullOrBlank()) append("\n\n${q.blankedWord}")
        else if (!q.shuffledLetters.isNullOrBlank()) append("\n\n${q.shuffledLetters}")
    }

    private fun buildHintText(q: QuestionResponse): String? {
        val isObjective = q.questionType.uppercase() in setOf("SYNONYM", "WORD_DEFINITION")
        if (isObjective) return "단어: ${q.word}"
        return q.hint?.takeIf { it.isNotBlank() }?.let { "힌트: $it" }
    }

    fun submitAnswer(userAnswer: String) {
        if (userAnswer.isBlank()) return
        if (_uiState.value is PvpQuestionUiState.Evaluating) return
        val question = currentQuestion ?: return

        _uiState.value = PvpQuestionUiState.Evaluating
        val context = getApplication<Application>()
        PvpWordManager.consumeAttack(context)
        refreshAttacks()

        viewModelScope.launch {
            try {
                val evalResponse = ApiClient.api.evaluateNewAnswer(
                    EvaluateNewRequest(
                        wordId = wordId.toLong(),
                        questionType = question.questionType,
                        userAnswer = userAnswer,
                        modelAnswer = question.answer
                    )
                ).data ?: throw Exception("채점 응답 데이터가 없습니다")
                if (evalResponse.correct) {
                    handleCorrect(evalResponse.score)
                } else {
                    handleWrong(evalResponse.score, evalResponse.feedback, evalResponse.correctAnswer)
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

        // intent에서 skillId가 null로 전달됐어도 fetch된 스킬의 ID를 사용
        val resolvedSkillId = skillId ?: skill?.skillId?.takeIf { it > 0 }

        var pvpDamage: Int? = null
        var effectType: String? = null
        var effectTurns: Int? = null
        var shieldBlocked = false
        var poisonDamageTaken = 0
        var paralyzed = false
        var dominantColor: String? = null

        if (resolvedSkillId != null) {
            val pvpResp = runCatching {
                ApiClient.api.usePvpSkill(PvpSkillRequest(skillId = resolvedSkillId, wordId = wordId.toLong()))
            }.getOrNull()?.data
            pvpDamage = pvpResp?.damageDealt
            effectType = pvpResp?.statusApplied?.type
            effectTurns = pvpResp?.statusApplied?.turns
            shieldBlocked = pvpResp?.shieldBlocked ?: false
            poisonDamageTaken = pvpResp?.poisonDamageTaken ?: 0
            paralyzed = pvpResp?.paralyzed ?: false
            dominantColor = pvpResp?.skillDominantColor
        }

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
                    wordMeaning = wordMeaning,
                    partOfSpeech = partOfSpeech
                )
            )
        }

        _uiState.value = PvpQuestionUiState.Correct(
            score = score,
            skill = skill,
            pvpDamage = pvpDamage,
            effectType = effectType,
            effectTurns = effectTurns,
            shieldBlocked = shieldBlocked,
            poisonDamageTaken = poisonDamageTaken,
            paralyzed = paralyzed,
            dominantColor = dominantColor
        )
    }

    private suspend fun handleWrong(score: Int, feedback: String?, correction: String?) {
        val sid = skillId
        var poisonDamageTaken = 0
        if (sid != null) {
            runCatching {
                ApiClient.api.failPvpSkill(PvpSkillRequest(skillId = sid, wordId = wordId.toLong()))
            }.getOrNull()?.data?.let { resp ->
                poisonDamageTaken = resp.poisonDamageTaken
            }
        }
        _uiState.value = PvpQuestionUiState.Wrong(
            score = score,
            feedback = feedback,
            correction = correction,
            poisonDamageTaken = poisonDamageTaken
        )
    }

    fun onCorrectConsumed() {
        if (_uiState.value is PvpQuestionUiState.Correct) {
            _uiState.value = PvpQuestionUiState.Idle
        }
    }

    companion object {
        private const val PASS_SCORE = 60
        private val QUESTION_TYPES = listOf(
            "spelling", "anagram", "word_definition", "synonym"
        )
    }
}
