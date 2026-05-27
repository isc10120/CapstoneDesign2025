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
        // 서버에서 최신 카드를 가져와 로컬에 병합 (서버 우선)
        // 서버 실패 시 로컬 캐시로 폴백
        try {
            val resp = ApiClient.api.getCollectedSkillList()
            if (resp.success && resp.data != null) {
                val existingCards = CollectedCardManager.getCards(getApplication()).associateBy { it.wordId }
                resp.data.forEach { skill ->
                    val localWord = WordRepository.allWords.find { it.id.toLong() == skill.wordId }
                    val grade = when (localWord?.difficulty) {
                        Difficulty.HARD -> "금급"
                        Difficulty.MEDIUM -> "은급"
                        else -> "동급"
                    }
                    // 서버가 빈 URL을 반환해도 기존 저장된 URL 유지
                    val imageUrl = skill.imageURL.takeIf { it.isNotBlank() }
                        ?: existingCards[skill.wordId.toInt()]?.imageUrl
                    CollectedCardManager.addCard(
                        getApplication(),
                        CollectedCardManager.CollectedCard(
                            wordId = skill.wordId.toInt(),
                            word = localWord?.word ?: skill.name,
                            skillName = skill.name,
                            skillDescription = skill.explain,
                            damage = skill.damage,
                            imageBase64 = null,
                            grade = grade,
                            imageUrl = imageUrl,
                            wordMeaning = localWord?.meaning ?: "",
                            partOfSpeech = localWord?.partOfSpeech ?: ""
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // 서버 실패 → 로컬 카드로 폴백
        }
        return CollectedCardManager.getCards(getApplication())
    }
}
