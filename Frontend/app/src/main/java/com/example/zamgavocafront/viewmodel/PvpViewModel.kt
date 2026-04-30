package com.example.zamgavocafront.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.model.WordData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PvpViewModel(application: Application) : AndroidViewModel(application) {

    private val _pvpWords = MutableStateFlow<List<WordData>>(emptyList())
    val pvpWords: StateFlow<List<WordData>> = _pvpWords.asStateFlow()

    /**
     * 이번 주 PVP 단어 목록을 로드한다.
     * @param filterUsed true이면 이미 정답 처리된 단어를 제외한다.
     */
    fun loadPvpWords(filterUsed: Boolean = false) {
        viewModelScope.launch {
            _pvpWords.value = WordRepository.fetchPvpWords(getApplication(), filterUsed)
        }
    }
}
