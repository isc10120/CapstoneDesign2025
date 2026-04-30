package com.example.zamgavocafront.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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

    private val nudgeHandler = Handler(Looper.getMainLooper())
    private val nudgeRunnable = object : Runnable {
        override fun run() {
            showRandomNudge()
            scheduleNextNudgeInternal()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        // 서비스 시작 시 넛지 스케줄 복원 (START_STICKY 재시작 포함)
        if (AlarmScheduler.isNudgeEnabled(this)) {
            scheduleNextNudgeInternal()
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
                // AlarmManager 경유 시: 즉시 표시 후 Handler 타이머 리셋
                showRandomNudge()
                scheduleNextNudgeInternal()
            }
            ACTION_START_NUDGE_SCHEDULE -> {
                // 넛지 활성화 시 Handler 기반 스케줄 시작
                scheduleNextNudgeInternal()
            }
            ACTION_STOP_NUDGE_SCHEDULE -> {
                nudgeHandler.removeCallbacks(nudgeRunnable)
            }
            ACTION_STOP -> {
                nudgeHandler.removeCallbacks(nudgeRunnable)
                dismissAll()
                stopSelf()
            }
        }
        return START_STICKY
    }

    /** 설정된 간격(min~max 분) 후에 넛지를 표시하도록 예약한다. */
    private fun scheduleNextNudgeInternal() {
        nudgeHandler.removeCallbacks(nudgeRunnable)
        if (!AlarmScheduler.isNudgeEnabled(this)) return
        val (min, max) = AlarmScheduler.getNudgeIntervalMinutes(this)
        val range = if (min >= max) min..min else min..max
        val delayMs = range.random() * 60 * 1000L
        nudgeHandler.postDelayed(nudgeRunnable, delayMs)
    }

    /** 완료(카운트 3) 되지 않은 단어 중 랜덤 1개를 반환한다. */
    private fun pickAvailableWord(): WordData? {
        val available = WordProgressManager.getAvailableWords(this, WordRepository.allWords)
        return if (available.isNotEmpty()) available.random() else null
    }

    private fun showMorningOverlay() {
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
        val word = pickAvailableWord() ?: return
        when ((0..2).random()) {
            0 -> showNudgeDrag(word)
            1 -> showNudgeTap(word)
            else -> showNudgeBounce(word)
        }
    }

    private fun showNudgeDrag(word: WordData) {
        nudgeDragOverlay?.dismiss()
        nudgeDragOverlay = NudgeDragOverlayManager(this, windowManager, word) {
            nudgeDragOverlay = null
            val newCount = WordProgressManager.incrementCount(this, word.id)
            if (newCount >= WordProgressManager.MAX_COUNT) {
                PvpWordManager.addUnlockedWord(this, word.id)
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
            }
            syncNudge(word.id, newCount)
        }
        nudgeBounceOverlay?.show()
    }

    // ── 넛지 백엔드 동기화 ──────────────────────────────────────────────────

    /** 넛지 1회 해제 후 호출. 오프라인이면 pending에 저장하고 다음 기회에 재전송. */
    private fun syncNudge(wordId: Int, count: Int) {
        savePendingNudge(wordId, count)
        serviceScope.launch {
            val pending = getPendingNudges()
            try {
                ApiClient.api.updateNudge(pending)
                clearPendingNudges()
            } catch (_: Exception) {
                // 전송 실패 → pending에 남겨두고 다음 nudge 시점에 재시도
            }
        }
    }

    private fun savePendingNudge(wordId: Int, count: Int) {
        getSharedPreferences("nudge_pending", MODE_PRIVATE)
            .edit().putInt("nudge_$wordId", count).apply()
    }

    private fun getPendingNudges(): List<NudgeUpdateRequest> =
        getSharedPreferences("nudge_pending", MODE_PRIVATE).all
            .mapNotNull { (key, value) ->
                if (key.startsWith("nudge_") && value is Int)
                    NudgeUpdateRequest(
                        id = key.removePrefix("nudge_").toLongOrNull() ?: return@mapNotNull null,
                        nudge = value
                    )
                else null
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
        nudgeHandler.removeCallbacks(nudgeRunnable)
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
