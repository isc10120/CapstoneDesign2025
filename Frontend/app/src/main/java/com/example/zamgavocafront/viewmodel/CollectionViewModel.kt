package com.example.zamgavocafront.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.pvp.CollectedCardManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CollectionUiState {
    object Loading : CollectionUiState()
    data class Success(val cards: List<CollectedCardManager.CollectedCard>) : CollectionUiState()
    object Empty : CollectionUiState()
    data class Error(val message: String) : CollectionUiState()
}

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    fun loadCards() {
        _uiState.value = CollectionUiState.Loading
        viewModelScope.launch {
            val cards = fetchCards()
            _uiState.value = if (cards.isEmpty()) CollectionUiState.Empty
                             else CollectionUiState.Success(cards)
        }
    }

    private suspend fun fetchCards(): List<CollectedCardManager.CollectedCard> {
        // 로컬 수집 카드가 있으면 그대로 사용 (PVP 수집 결과가 즉시 반영됨)
        val local = CollectedCardManager.getCards(getApplication())
        if (local.isNotEmpty()) return local

        // 로컬이 비어있을 때만 서버 데이터로 초기화
        return try {
            val resp = ApiClient.api.getCollectedSkillList()
            if (resp.success && resp.data != null) {
                resp.data.map { skill ->
                    val localWord = WordRepository.allWords.find { it.id.toLong() == skill.wordId }
                    val grade = when (localWord?.difficulty) {
                        Difficulty.HARD -> "금급"
                        Difficulty.MEDIUM -> "은급"
                        else -> "동급"
                    }
                    CollectedCardManager.CollectedCard(
                        wordId = skill.wordId.toInt(),
                        word = localWord?.word ?: skill.name,
                        skillName = skill.name,
                        skillDescription = skill.explain,
                        damage = skill.damage,
                        imageBase64 = null,
                        grade = grade,
                        imageUrl = skill.imageURL.takeIf { it.isNotBlank() }
                    )
                }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
