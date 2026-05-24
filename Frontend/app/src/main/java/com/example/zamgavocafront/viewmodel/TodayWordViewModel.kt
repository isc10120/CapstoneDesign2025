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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayWordViewModel(application: Application) : AndroidViewModel(application) {

    private val _words = MutableStateFlow<List<WordData>>(WordRepository.allWords.toList())
    val words: StateFlow<List<WordData>> = _words.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var isLoaded = false

    companion object {
        private val sessionCache = mutableListOf<WordData>()
        val externalWordsFlow = MutableSharedFlow<List<WordData>>(replay = 1)

        fun clearSession() {
            sessionCache.clear()
        }

        /**
         * OverlayService 등 외부에서 새 단어 목록으로 갱신할 때 호출.
         * context를 넘기면 날짜별 캐시에도 저장해 앱 재시작 후에도 전체 목록을 복원한다.
         */
        fun updateSessionFromOutside(words: List<WordData>, context: android.content.Context? = null) {
            sessionCache.clear()
            sessionCache.addAll(words)
            context?.applicationContext?.let { ctx ->
                ctx.getSharedPreferences("daily_word_cache", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putString("words", ApiClient.gson.toJson(words))
                    .putString("date", today())
                    .apply()
            }
            externalWordsFlow.tryEmit(words)
        }

        private fun today(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    init {
        viewModelScope.launch {
            externalWordsFlow.collect { words ->
                _words.value = words
                isLoaded = true
            }
        }
    }

    fun loadDailyWords() {
        if (isLoaded) return
        isLoaded = true

        if (sessionCache.isNotEmpty()) {
            _words.value = sessionCache.toList()
            return
        }

        viewModelScope.launch {
            val existing = runCatching { ApiClient.api.getDailyWordList() }.getOrNull()?.data

            if (!existing.isNullOrEmpty()) {
                syncNudgeFromServer(existing)
                clearStalePendingNudges(existing.map { it.id }.toSet())

                val serverMapped = existing.map { WordRepository.mapWordResponse(it) }

                // 완료된 단어는 서버에서 삭제되므로 캐시로 보완해 항상 전체 목록을 표시한다
                val cached = loadDailyWordCache()
                val fullList = if (cached.isNotEmpty()) {
                    val serverById = serverMapped.associateBy { it.id }
                    cached.map { cachedWord -> serverById[cachedWord.id] ?: cachedWord }
                } else {
                    serverMapped
                }

                applyWordList(fullList)

            } else {
                // 서버에 남은 단어가 없으면 캐시 확인 (모두 완료된 경우)
                val cached = loadDailyWordCache()
                if (cached.isNotEmpty()) {
                    applyWordList(cached)
                    return@launch
                }

                // 캐시도 없으면 새 단어 목록 요청
                val newWords = runCatching {
                    ApiClient.api.getNewDailyWordList(Difficulty.MEDIUM.toApiLevel())
                }.getOrNull()?.data

                if (!newWords.isNullOrEmpty()) {
                    resetLocalProgressForNewWords(newWords)
                    val mapped = newWords.map { WordRepository.mapWordResponse(it) }
                    saveDailyWordCache(mapped)
                    applyWordList(mapped)
                } else {
                    isLoaded = false
                    _errorMessage.value = "단어를 불러오지 못했습니다. 네트워크 연결을 확인해주세요."
                }
            }
        }
    }

    private fun applyWordList(words: List<WordData>) {
        sessionCache.clear()
        sessionCache.addAll(words)
        WordRepository.allWords.clear()
        WordRepository.allWords.addAll(words)
        _words.value = words
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 서버의 nudge 횟수를 로컬에 동기화한다.
     * - pending nudge가 있으면 server/pending 중 큰 값을 유지
     * - pending이 없으면 서버 값을 권위값으로 사용
     */
    private fun syncNudgeFromServer(words: List<WordResponse>) {
        val ctx = getApplication<Application>()
        val progressPrefs = ctx.getSharedPreferences("word_progress", android.content.Context.MODE_PRIVATE)
        val pendingPrefs = ctx.getSharedPreferences("nudge_pending", android.content.Context.MODE_PRIVATE)
        val editor = progressPrefs.edit()
        var changed = false
        for (word in words) {
            val serverNudge = word.nudge ?: continue
            val localNudge = progressPrefs.getInt("word_${word.id}", 0)
            val pendingNudge = pendingPrefs.getInt("nudge_${word.id}", 0)
            val target = if (pendingNudge > 0) {
                maxOf(serverNudge.toInt(), pendingNudge).coerceAtMost(WordProgressManager.MAX_COUNT)
            } else {
                serverNudge.toInt().coerceAtMost(WordProgressManager.MAX_COUNT)
            }
            if (target != localNudge) {
                editor.putInt("word_${word.id}", target)
                changed = true
            }
        }
        if (changed) editor.apply()
    }

    /** 새 단어 목록이 배정될 때 기존 stale 로컬 진행도와 pending nudge를 초기화한다. */
    private fun resetLocalProgressForNewWords(words: List<WordResponse>) {
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences("word_progress", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (word in words) {
            editor.putInt("word_${word.id}", 0)
        }
        editor.apply()
        ctx.getSharedPreferences("nudge_pending", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    /** 현재 단어 목록에 없는 단어의 pending nudge를 제거해 배치 실패를 방지한다. */
    private fun clearStalePendingNudges(currentWordIds: Set<Long>) {
        if (currentWordIds.isEmpty()) return
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences("nudge_pending", android.content.Context.MODE_PRIVATE)
        val staleKeys = prefs.all.keys.filter { key ->
            if (!key.startsWith("nudge_")) return@filter false
            val id = key.removePrefix("nudge_").toLongOrNull() ?: return@filter true
            id !in currentWordIds
        }
        if (staleKeys.isNotEmpty()) {
            prefs.edit().apply { staleKeys.forEach { remove(it) } }.apply()
        }
    }

    /** 오늘의 단어 목록을 날짜 키와 함께 SharedPreferences에 저장한다. */
    private fun saveDailyWordCache(words: List<WordData>) {
        val ctx = getApplication<Application>()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        ctx.getSharedPreferences("daily_word_cache", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("words", ApiClient.gson.toJson(words))
            .putString("date", today)
            .apply()
    }

    /** 오늘 날짜의 캐시가 있으면 반환, 없거나 날짜가 다르면 빈 목록 반환. */
    private fun loadDailyWordCache(): List<WordData> {
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences("daily_word_cache", android.content.Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (prefs.getString("date", null) != today) return emptyList()
        val json = prefs.getString("words", null) ?: return emptyList()
        return runCatching {
            ApiClient.gson.fromJson(json, Array<WordData>::class.java)?.toList() ?: emptyList()
        }.getOrElse { emptyList() }
    }
}
