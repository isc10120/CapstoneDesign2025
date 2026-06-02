package com.example.zamgavocafront.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.StompManager
import com.example.zamgavocafront.api.dto.BattleResultResponse
import com.example.zamgavocafront.api.dto.BattleStatusResponse
import com.example.zamgavocafront.api.dto.StatusEffect
import com.example.zamgavocafront.api.dto.StompSkillMessage
import com.example.zamgavocafront.model.WordData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PvpViewModel(application: Application) : AndroidViewModel(application) {

    private val _pvpWords = MutableStateFlow<List<WordData>>(emptyList())
    val pvpWords: StateFlow<List<WordData>> = _pvpWords.asStateFlow()

    private val _battleStatus = MutableStateFlow<BattleStatusResponse?>(null)
    val battleStatus: StateFlow<BattleStatusResponse?> = _battleStatus.asStateFlow()

    // 상대방 누적 데미지 — 서버에서 초기값 로드 후 STOMP로 실시간 증가
    private val _enemyDamage = MutableStateFlow(0)
    val enemyDamage: StateFlow<Int> = _enemyDamage.asStateFlow()

    // 상태이상 — 초기값 서버 로드, STOMP로 실시간 갱신
    private val _myStatusEffects = MutableStateFlow<List<StatusEffect>>(emptyList())
    val myStatusEffects: StateFlow<List<StatusEffect>> = _myStatusEffects.asStateFlow()

    private val _enemyStatusEffects = MutableStateFlow<List<StatusEffect>>(emptyList())
    val enemyStatusEffects: StateFlow<List<StatusEffect>> = _enemyStatusEffects.asStateFlow()

    private val _latestResult = MutableStateFlow<BattleResultResponse?>(null)
    val latestResult: StateFlow<BattleResultResponse?> = _latestResult.asStateFlow()

    // 상대가 스킬을 사용했을 때 이펙트 색상 이벤트 (null = 색상 없음)
    private val _opponentSkillColor = MutableSharedFlow<String?>(extraBufferCapacity = 1)
    val opponentSkillColor: SharedFlow<String?> = _opponentSkillColor

    private var stompJob: Job? = null
    private var connectedBattleId: Long = -1L

    fun loadPvpWords() {
        viewModelScope.launch {
            _pvpWords.value = WordRepository.fetchPvpWords(getApplication())
        }
    }

    fun loadBattleStatus() {
        viewModelScope.launch {
            try {
                val resp = ApiClient.api.getPvpStatus()
                if (resp.success && resp.data != null) {
                    _battleStatus.value = resp.data
                    _enemyDamage.value = resp.data.enemy.totalDamage
                    _myStatusEffects.value = resp.data.my.statusEffects
                    _enemyStatusEffects.value = resp.data.enemy.statusEffects
                    connectStomp(resp.data.battleId)
                }
            } catch (_: Exception) {}
        }
    }

    fun checkLatestResult() {
        viewModelScope.launch {
            try {
                val resp = ApiClient.api.getPvpLatestResult()
                if (resp.success) _latestResult.value = resp.data
            } catch (_: Exception) {}
        }
    }

    fun confirmResult() {
        viewModelScope.launch {
            try { ApiClient.api.confirmPvpResult() } catch (_: Exception) {}
            _latestResult.value = null
        }
    }

    fun testMatch() {
        viewModelScope.launch {
            try {
                ApiClient.api.testMatch()
                loadBattleStatus()
            } catch (_: Exception) {}
        }
    }

    private fun connectStomp(battleId: Long) {
        if (battleId == connectedBattleId && stompJob?.isActive == true) return
        connectedBattleId = battleId
        stompJob?.cancel()

        val myUserId = getApplication<Application>()
            .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getLong("userId", 0L)

        stompJob = viewModelScope.launch {
            try {
                StompManager.subscribe(battleId).collect { body ->
                    try {
                        val msg = ApiClient.gson.fromJson(body, StompSkillMessage::class.java)
                        // senderId가 본인이면 무시 (서버 에코)
                        if (msg.senderId != myUserId) {
                            // 실패(문제 오답)가 아닐 때만 데미지 증가 및 이펙트 재생
                            if (!msg.isFailed) {
                                _enemyDamage.value += msg.damageDealt
                                _opponentSkillColor.tryEmit(msg.skillDominantColor)
                            }
                            // 상대가 나에게 적용한 디버프 추가 (POISON, PARALYZE)
                            msg.statusApplied?.let { applied ->
                                val effect = StatusEffect(
                                    id = applied.id,
                                    type = applied.type,
                                    remainingTurns = applied.turns
                                )
                                if (applied.type in DEBUFFS_ON_TARGET) {
                                    _myStatusEffects.value = _myStatusEffects.value + effect
                                } else {
                                    _enemyStatusEffects.value = _enemyStatusEffects.value + effect
                                }
                            }
                            // 상대가 자신의 디버프를 클렌즈한 경우 제거
                            msg.cleansedEffectId?.let { effectId ->
                                _enemyStatusEffects.value =
                                    _enemyStatusEffects.value.filter { it.id != effectId }
                            }
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {
                connectedBattleId = -1L // 재연결 허용
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stompJob?.cancel()
    }

    companion object {
        // 상대가 사용하면 나에게 적용되는 디버프 타입
        private val DEBUFFS_ON_TARGET = setOf("POISON", "PARALYZE")
    }
}
