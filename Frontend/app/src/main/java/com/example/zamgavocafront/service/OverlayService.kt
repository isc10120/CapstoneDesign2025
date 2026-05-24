package com.example.zamgavocafront.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import com.example.zamgavocafront.AlarmScheduler
import com.example.zamgavocafront.MainActivity
import com.example.zamgavocafront.WordProgressManager
import com.example.zamgavocafront.WordRepository
import com.example.zamgavocafront.api.ApiClient
import com.example.zamgavocafront.api.dto.NudgeUpdateRequest
import com.example.zamgavocafront.pvp.PvpWordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.zamgavocafront.model.Difficulty
import com.example.zamgavocafront.model.WordData
import com.example.zamgavocafront.overlay.MorningOverlayManager
import com.example.zamgavocafront.viewmodel.TodayWordViewModel
import com.example.zamgavocafront.overlay.NudgeBounceOverlayManager
import com.example.zamgavocafront.overlay.NudgeDragOverlayManager
import com.example.zamgavocafront.overlay.NudgeTapOverlayManager
import com.example.zamgavocafront.overlay.WordListOverlayManager

class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW_MORNING = "ACTION_SHOW_MORNING"
        const val ACTION_SHOW_WORD_LIST = "ACTION_SHOW_WORD_LIST"
        const val ACTION_SHOW_NUDGE_DRAG = "ACTION_SHOW_NUDGE_DRAG"
        const val ACTION_SHOW_NUDGE_TAP = "ACTION_SHOW_NUDGE_TAP"
        const val ACTION_SHOW_NUDGE_BOUNCE = "ACTION_SHOW_NUDGE_BOUNCE"
        const val ACTION_SHOW_NUDGE_RANDOM = "ACTION_SHOW_NUDGE_RANDOM"
        const val ACTION_START_NUDGE_SCHEDULE = "ACTION_START_NUDGE_SCHEDULE"
        const val ACTION_STOP_NUDGE_SCHEDULE = "ACTION_STOP_NUDGE_SCHEDULE"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIF_ID = 1001
    }

    private lateinit var windowManager: WindowManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var morningOverlay: MorningOverlayManager? = null
    private var wordListOverlay: WordListOverlayManager? = null
    private var nudgeDragOverlay: NudgeDragOverlayManager? = null
    private var nudgeTapOverlay: NudgeTapOverlayManager? = null
    private var nudgeBounceOverlay: NudgeBounceOverlayManager? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        // START_STICKY 재시작 시 AlarmManager 알람 복원
        if (AlarmScheduler.isNudgeEnabled(this)) {
            AlarmScheduler.scheduleNextNudgeAlarm(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_MORNING -> showMorningOverlay()
            ACTION_SHOW_WORD_LIST -> {
                val difficulty = intent.getStringExtra(EXTRA_DIFFICULTY)
                    ?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() }
                    ?: Difficulty.MEDIUM
                serviceScope.launch {
                    val level = difficulty.toApiLevel()
                    try {
                        val resp = ApiClient.api.getNewDailyWordList(level)
                        if (resp.success && resp.data != null) {
                            val words = resp.data.map { dto ->
                                WordData(
                                    id = dto.id.toInt(),
                                    word = dto.word,
                                    meaning = dto.definition,
                                    exampleEn = dto.example,
                                    exampleKr = dto.exampleKor,
                                    difficulty = difficulty,
                                    skillId = dto.skillId
                                )
                            }
                            withContext(Dispatchers.Main) {
                                WordRepository.allWords.clear()
                                WordRepository.allWords.addAll(words)
                                TodayWordViewModel.updateSessionFromOutside(words, this@OverlayService)
                                showWordListOverlay(WordRepository.allWords, difficulty)
                            }
                            return@launch
                        }
                    } catch (_: Exception) { }
                    withContext(Dispatchers.Main) {
                        showWordListOverlay(WordRepository.allWords, difficulty)
                    }
                }
            }
            ACTION_SHOW_NUDGE_DRAG -> pickAvailableWord()?.let { showNudgeDrag(it) }
            ACTION_SHOW_NUDGE_TAP -> pickAvailableWord()?.let { showNudgeTap(it) }
            ACTION_SHOW_NUDGE_BOUNCE -> pickAvailableWord()?.let { showNudgeBounce(it) }
            ACTION_SHOW_NUDGE_RANDOM -> {
                showRandomNudge()
            }
            ACTION_START_NUDGE_SCHEDULE -> { /* 서비스 실행 유지용. AlarmManager가 알람 발동 시 이미 실행 중인 서비스에 전달 가능 */ }
            ACTION_STOP_NUDGE_SCHEDULE -> { /* AlarmScheduler.stopNudgeSchedule()가 AlarmManager 취소 처리 */ }
            ACTION_STOP -> {
                dismissAll()
                stopSelf()
            }
        }
        return START_STICKY
    }

    /** 완료(카운트 3) 되지 않은 단어 중 랜덤 1개를 반환한다. */
    private fun pickAvailableWord(): WordData? {
        val available = WordProgressManager.getAvailableWords(this, WordRepository.allWords)
        return if (available.isNotEmpty()) available.random() else null
    }

    private fun showMorningOverlay() {
        if (!Settings.canDrawOverlays(this)) return
        AlarmScheduler.recordMorningShownToday(this)
        morningOverlay?.dismiss()
        morningOverlay = MorningOverlayManager(this, windowManager) { difficulty ->
            morningOverlay?.dismiss()
            morningOverlay = null
            // 난이도 선택 시 백엔드에서 해당 레벨 단어 목록을 새로 받아온다
            serviceScope.launch {
                val level = difficulty.toApiLevel()
                try {
                    val resp = ApiClient.api.getNewDailyWordList(level)
                    if (resp.success && resp.data != null) {
                        val words = resp.data.map { dto ->
                            WordData(
                                id = dto.id.toInt(),
                                word = dto.word,
                                meaning = dto.definition,
                                exampleEn = dto.example,
                                exampleKr = dto.exampleKor,
                                difficulty = difficulty,
                                skillId = dto.skillId
                            )
                        }
                        withContext(Dispatchers.Main) {
                            WordRepository.allWords.clear()
                            WordRepository.allWords.addAll(words)
                            TodayWordViewModel.updateSessionFromOutside(words, this@OverlayService)
                            showWordListOverlay(WordRepository.allWords, difficulty)
                        }
                        return@launch
                    }
                } catch (_: Exception) { }
                // API 실패 시 기존 단어 목록 그대로 사용
                withContext(Dispatchers.Main) {
                    showWordListOverlay(WordRepository.allWords, difficulty)
                }
            }
        }
        morningOverlay?.show()
    }

    private fun showWordListOverlay(words: List<WordData>, difficulty: Difficulty) {
        // 난이도 선택 시점에 스킬 카드 백그라운드 미리 생성

        wordListOverlay?.dismiss()
        wordListOverlay = WordListOverlayManager(this, windowManager, words) {
            wordListOverlay?.dismiss()
            wordListOverlay = null
        }
        wordListOverlay?.show()
    }

    private fun showRandomNudge() {
        if (!Settings.canDrawOverlays(this)) return
        val word = pickAvailableWord()
        if (word != null) {
            when ((0..2).random()) {
                0 -> showNudgeDrag(word)
                1 -> showNudgeTap(word)
                else -> showNudgeBounce(word)
            }
            return
        }
        // allWords가 비어있거나 모든 단어가 완료된 경우 API에서 재로드 후 시도
        serviceScope.launch {
            try {
                val resp = ApiClient.api.getDailyWordList()
                if (resp.success && !resp.data.isNullOrEmpty()) {
                    val words = resp.data.map { dto ->
                        WordData(
                            id = dto.id.toInt(),
                            word = dto.word,
                            meaning = dto.definition,
                            exampleEn = dto.example,
                            exampleKr = dto.exampleKor,
                            difficulty = com.example.zamgavocafront.model.Difficulty.MEDIUM,
                            skillId = dto.skillId
                        )
                    }
                    withContext(Dispatchers.Main) {
                        WordRepository.allWords.clear()
                        WordRepository.allWords.addAll(words)
                        TodayWordViewModel.updateSessionFromOutside(words, this@OverlayService)
                        pickAvailableWord()?.let { reloaded ->
                            when ((0..2).random()) {
                                0 -> showNudgeDrag(reloaded)
                                1 -> showNudgeTap(reloaded)
                                else -> showNudgeBounce(reloaded)
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun showNudgeDrag(word: WordData) {
        nudgeDragOverlay?.dismiss()
        nudgeDragOverlay = NudgeDragOverlayManager(this, windowManager, word) {
            nudgeDragOverlay = null
            val newCount = WordProgressManager.incrementCount(this, word.id)
            if (newCount >= WordProgressManager.MAX_COUNT) {
                PvpWordManager.addUnlockedWord(this, word.id)
                cacheUnlockedWord(word)
            }
            syncNudge(word.id, newCount)
        }
        nudgeDragOverlay?.show()
    }

    private fun showNudgeTap(word: WordData) {
        nudgeTapOverlay?.dismiss()
        nudgeTapOverlay = NudgeTapOverlayManager(this, windowManager, word) {
            nudgeTapOverlay = null
            val newCount = WordProgressManager.incrementCount(this, word.id)
            if (newCount >= WordProgressManager.MAX_COUNT) {
                PvpWordManager.addUnlockedWord(this, word.id)
                cacheUnlockedWord(word)
            }
            syncNudge(word.id, newCount)
        }
        nudgeTapOverlay?.show()
    }

    private fun showNudgeBounce(word: WordData) {
        nudgeBounceOverlay?.dismiss()
        nudgeBounceOverlay = NudgeBounceOverlayManager(this, windowManager, word) {
            nudgeBounceOverlay = null
            val newCount = WordProgressManager.incrementCount(this, word.id)
            if (newCount >= WordProgressManager.MAX_COUNT) {
                PvpWordManager.addUnlockedWord(this, word.id)
                cacheUnlockedWord(word)
            }
            syncNudge(word.id, newCount)
        }
        nudgeBounceOverlay?.show()
    }

    private fun cacheUnlockedWord(word: WordData) {
        if (WordRepository.pvpWordCache.none { it.id == word.id }) {
            WordRepository.pvpWordCache.add(word)
        }
        // Persist so the word survives process restart before server sync completes
        getSharedPreferences("pvp_completed_words", MODE_PRIVATE)
            .edit()
            .putString("word_${word.id}", ApiClient.gson.toJson(word))
            .apply()
    }

    // ── 넛지 백엔드 동기화 ──────────────────────────────────────────────────

    /** 넛지 1회 해제 후 호출. 오프라인이면 pending에 저장하고 다음 기회에 재전송. */
    private fun syncNudge(wordId: Int, count: Int) {
        savePendingNudge(wordId, count)
        serviceScope.launch {
            val pending = getPendingNudges()
            try {
                val resp = ApiClient.api.updateNudge(pending)
                if (resp.success) clearPendingNudges()
            } catch (_: Exception) {
                // 전송 실패 → pending에 남겨두고 다음 nudge 시점에 재시도
            }
        }
    }

    private fun savePendingNudge(wordId: Int, count: Int) {
        getSharedPreferences("nudge_pending", MODE_PRIVATE)
            .edit().putInt("nudge_$wordId", count).apply()
    }

    private fun getPendingNudges(): List<NudgeUpdateRequest> {
        // Only send nudges for words in the current session to avoid stale entries
        // causing DAILY_NUDGE_WORD_NOT_FOUND errors that fail the whole batch
        val currentIds = WordRepository.allWords.map { it.id.toLong() }.toSet()
        return getSharedPreferences("nudge_pending", MODE_PRIVATE).all
            .mapNotNull { (key, value) ->
                if (key.startsWith("nudge_") && value is Int) {
                    val id = key.removePrefix("nudge_").toLongOrNull() ?: return@mapNotNull null
                    if (currentIds.isNotEmpty() && id !in currentIds) return@mapNotNull null
                    NudgeUpdateRequest(id = id, nudge = value)
                } else null
            }
    }

    private fun clearPendingNudges() {
        getSharedPreferences("nudge_pending", MODE_PRIVATE).edit().clear().apply()
    }

    private fun dismissAll() {
        morningOverlay?.dismiss()
        wordListOverlay?.dismiss()
        nudgeDragOverlay?.dismiss()
        nudgeTapOverlay?.dismiss()
        nudgeBounceOverlay?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissAll()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "단어 학습 서비스",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "ZamgaVoca 단어 오버레이 서비스가 실행 중입니다"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ZamgaVoca 학습 중")
            .setContentText("단어 오버레이가 활성화되어 있습니다")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(contentIntent)
            .addAction(Notification.Action.Builder(null, "중단", stopIntent).build())
            .setOngoing(true)
            .build()
    }
}
