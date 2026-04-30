package com.example.zamgavocafront.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.WordProgressManager
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.dto.WordResponse
import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.model.WordData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodayWordViewModel(application: Application) : AndroidViewModel(application) {

    private val _words = MutableStateFlow<List<WordData>>(WordRepository.allWords.toList())
    val words: StateFlow<List<WordData>> = _words.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var isLoaded = false

    fun loadDailyWords() {
        if (isLoaded) return
        isLoaded = true
        viewModelScope.launch {
            val existing = runCatching { ApiClient.api.getDailyWordList() }.getOrNull()?.data
            val wordList: List<WordResponse>? = if (!existing.isNullOrEmpty()) {
                syncNudgeFromServer(existing)
                existing
            } else {
                runCatching {
                    ApiClient.api.getNewDailyWordList(Difficulty.MEDIUM.toApiLevel())
                }.getOrNull()?.data
            }

            if (!wordList.isNullOrEmpty()) {
                val mapped = wordList.map { WordRepository.mapWordResponse(it) }
                WordRepository.allWords.clear()
                WordRepository.allWords.addAll(mapped)
                _words.value = mapped
                _errorMessage.value = null
            } else {
                isLoaded = false
                _errorMessage.value = "단어를 불러오지 못했습니다. 네트워크 연결을 확인해주세요."
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /** 서버의 nudge 횟수가 로컬보다 크면 로컬을 서버 값으로 덮어쓴다. */
    private fun syncNudgeFromServer(words: List<WordResponse>) {
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences("word_progress", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        var changed = false
        for (word in words) {
            val serverNudge = word.nudge ?: continue
            val localNudge = prefs.getInt("word_${word.id}", 0)
            if (serverNudge > localNudge) {
                editor.putInt("word_${word.id}", serverNudge.coerceAtMost(WordProgressManager.MAX_COUNT))
                changed = true
            }
        }
        if (changed) editor.apply()
    }
}
