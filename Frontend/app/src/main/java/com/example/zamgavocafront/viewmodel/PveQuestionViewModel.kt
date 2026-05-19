package com.example.zamgavocafront.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.dto.EvaluateNewRequest
import com.example.zamgavocafront.api.dto.QuestionRequest
import com.example.zamgavocafront.api.dto.QuestionResponse
import com.example.zamgavocafront.pvp.CollectedCardManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PveQuestionUiState {
    object Idle : PveQuestionUiState()
    object LoadingQuestion : PveQuestionUiState()
    data class QuestionReady(
        val question: String,
        val hint: String,
        val questionType: String,
        val options: List<String>? = null
    ) : PveQuestionUiState()
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

    var wordId: Int = 0
    var wordText: String = ""

    private var currentQuestion: QuestionResponse? = null

    fun loadQuestion() {
        val current = _uiState.value
        if (current !is PveQuestionUiState.Idle && current !is PveQuestionUiState.Error) return
        _uiState.value = PveQuestionUiState.LoadingQuestion
        viewModelScope.launch {
            try {
                val questionType = QUESTION_TYPES.random()
                val response = ApiClient.api.generateQuestion(questionType, QuestionRequest(wordId.toLong())).data
                    ?: throw Exception("서버 응답 데이터가 없습니다")
                currentQuestion = response
                _uiState.value = PveQuestionUiState.QuestionReady(
                    question = buildQuestionText(response),
                    hint = response.hint?.takeIf { it.isNotBlank() }?.let { "힌트: $it" } ?: "",
                    questionType = response.questionType,
                    options = response.options
                )
            } catch (e: Exception) {
                _uiState.value = PveQuestionUiState.Error("문제 생성 실패: ${e.message}")
            }
        }
    }

    private fun buildQuestionText(q: QuestionResponse): String = buildString {
        append(q.question)
        if (!q.blankedWord.isNullOrBlank()) append("\n\n${q.blankedWord}")
        else if (!q.shuffledLetters.isNullOrBlank()) append("\n\n${q.shuffledLetters}")
    }

    fun submitAnswer(answer: String) {
        if (answer.isBlank()) return
        if (_uiState.value is PveQuestionUiState.Evaluating) return
        val question = currentQuestion ?: return

        _uiState.value = PveQuestionUiState.Evaluating
        viewModelScope.launch {
            try {
                val evalResp = ApiClient.api.evaluateNewAnswer(
                    EvaluateNewRequest(
                        wordId = wordId.toLong(),
                        questionType = question.questionType,
                        userAnswer = answer,
                        modelAnswer = question.answer
                    )
                ).data ?: throw Exception("채점 응답 데이터가 없습니다")
                val score = evalResp.score
                if (evalResp.correct) {
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
                        correction = evalResp.correctAnswer
                    )
                }
            } catch (e: Exception) {
                _uiState.value = PveQuestionUiState.Error("채점 오류: ${e.message}")
            }
        }
    }

    fun onCorrectConsumed() {
        if (_uiState.value is PveQuestionUiState.Correct) {
            _uiState.value = PveQuestionUiState.Idle
        }
    }

    companion object {
        private val QUESTION_TYPES = listOf(
            "spelling", "anagram", "word_definition", "synonym", "sentence_writing", "translation"
        )
    }
}
