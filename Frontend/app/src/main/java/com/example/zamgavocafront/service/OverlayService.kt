package com.example.zamgavocafront.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import com.example.zamgavocafront.MainActivity
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
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY"
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIF_ID = 1001
    }

    private lateinit var windowManager: WindowManager

    private var morningOverlay: MorningOverlayManager? = null
    private var wordListOverlay: WordListOverlayManager? = null
    private var nudgeDragOverlay: NudgeDragOverlayManager? = null
    private var nudgeTapOverlay: NudgeTapOverlayManager? = null
    private var nudgeBounceOverlay: NudgeBounceOverlayManager? = null

    // Placeholder words – replace with API call
    val sampleWords = listOf(
        WordData(1, "ephemeral", "일시적인, 단명하는", Difficulty.MEDIUM),
        WordData(2, "serendipity", "뜻밖의 행운", Difficulty.HARD),
        WordData(3, "eloquent", "유창한, 설득력 있는", Difficulty.MEDIUM),
        WordData(4, "benevolent", "자비로운, 친절한", Difficulty.EASY),
        WordData(5, "melancholy", "우울, 침울함", Difficulty.MEDIUM),
        WordData(6, "perseverance", "인내, 불굴의 정신", Difficulty.EASY),
        WordData(7, "ambiguous", "애매한, 불명확한", Difficulty.MEDIUM),
        WordData(8, "tenacious", "끈질긴, 완강한", Difficulty.HARD),
        WordData(9, "profound", "심오한, 깊은", Difficulty.HARD),
        WordData(10, "candid", "솔직한, 숨김없는", Difficulty.EASY),
    )

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_MORNING -> showMorningOverlay()
            ACTION_SHOW_WORD_LIST -> {
                val difficulty = intent.getStringExtra(EXTRA_DIFFICULTY)
                    ?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() }
                    ?: Difficulty.MEDIUM
                showWordListOverlay(sampleWords, difficulty)
            }
            ACTION_SHOW_NUDGE_DRAG -> showNudgeDrag(sampleWords.random())
            ACTION_SHOW_NUDGE_TAP -> showNudgeTap(sampleWords.random())
            ACTION_SHOW_NUDGE_BOUNCE -> showNudgeBounce(sampleWords.random())
            ACTION_SHOW_NUDGE_RANDOM -> showRandomNudge()
            ACTION_STOP -> {
                dismissAll()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun showMorningOverlay() {
        morningOverlay?.dismiss()
        morningOverlay = MorningOverlayManager(this, windowManager) { difficulty ->
            morningOverlay?.dismiss()
            morningOverlay = null
            showWordListOverlay(sampleWords, difficulty)
        }
        morningOverlay?.show()
    }

    private fun showWordListOverlay(words: List<WordData>, difficulty: Difficulty) {
        wordListOverlay?.dismiss()
        wordListOverlay = WordListOverlayManager(this, windowManager, words) {
            wordListOverlay?.dismiss()
            wordListOverlay = null
        }
        wordListOverlay?.show()
    }

    private fun showRandomNudge() {
        val word = sampleWords.random()
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
        }
        nudgeDragOverlay?.show()
    }

    private fun showNudgeTap(word: WordData) {
        nudgeTapOverlay?.dismiss()
        nudgeTapOverlay = NudgeTapOverlayManager(this, windowManager, word) {
            nudgeTapOverlay = null
        }
        nudgeTapOverlay?.show()
    }

    private fun showNudgeBounce(word: WordData) {
        nudgeBounceOverlay?.dismiss()
        nudgeBounceOverlay = NudgeBounceOverlayManager(this, windowManager, word) {
            nudgeBounceOverlay = null
        }
        nudgeBounceOverlay?.show()
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
