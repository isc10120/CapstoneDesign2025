package com.example.zamgavocafront.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.model.WordData

/**
 * 넛지 ① – 드래그 해제
 * 손가락을 위아래로 계속 움직이는 동안만 타이머가 진행되고,
 * 멈추거나 손가락을 떼면 타이머가 초기화된다.
 * 3초 연속 드래그를 달성하면 오버레이가 사라진다.
 */
class NudgeDragOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val word: WordData,
    private val onDismissed: () -> Unit
) {
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val REQUIRED_MS = 3000L

    // 드래그 타이머 상태
    private var isDraggingActive = false
    private var totalActiveDragMs = 0L
    private var lastCheckTime = 0L
    private var lastMovementTime = 0L

    private val tickRunnable = object : Runnable {
        override fun run() {
            val view = overlayView ?: return
            val now = SystemClock.elapsedRealtime()
            val timeSinceMove = now - lastMovementTime

            if (isDraggingActive && timeSinceMove < 150L) {
                // 150ms 이내에 움직임이 있었으면 진행
                totalActiveDragMs += (now - lastCheckTime).coerceAtMost(100L)
            } else if (timeSinceMove >= 250L) {
                // 250ms 이상 멈추면 리셋
                totalActiveDragMs = 0L
                view.findViewById<ProgressBar>(R.id.progress_drag).progress = 0
            }
            lastCheckTime = now

            val progress = ((totalActiveDragMs.toFloat() / REQUIRED_MS) * 100)
                .toInt().coerceIn(0, 100)
            view.findViewById<ProgressBar>(R.id.progress_drag).progress = progress

            if (totalActiveDragMs >= REQUIRED_MS) {
                dismissWithSuccess()
                return
            }
            handler.postDelayed(this, 30L)
        }
    }

    fun show() {
        if (overlayView != null) return

        val view = LayoutInflater.from(context).inflate(R.layout.overlay_nudge_drag, null)
        view.findViewById<TextView>(R.id.tv_nudge_drag_word).text = word.word
        view.findViewById<TextView>(R.id.tv_nudge_drag_meaning).text = word.meaning

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDraggingActive = true
                    lastMovementTime = SystemClock.elapsedRealtime()
                    lastCheckTime = lastMovementTime
                    totalActiveDragMs = 0L
                    handler.removeCallbacks(tickRunnable)
                    handler.post(tickRunnable)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lastMovementTime = SystemClock.elapsedRealtime()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDraggingActive = false
                    handler.removeCallbacks(tickRunnable)
                    totalActiveDragMs = 0L
                    view.findViewById<ProgressBar>(R.id.progress_drag).progress = 0
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
        overlayView = view

        view.scaleX = 0.85f; view.scaleY = 0.85f; view.alpha = 0f
        view.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(220).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun dismissWithSuccess() {
        handler.removeCallbacks(tickRunnable)
        overlayView?.animate()?.alpha(0f)?.scaleX(1.1f)?.scaleY(1.1f)
            ?.setDuration(250)?.withEndAction {
                try { overlayView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
                overlayView = null
                onDismissed()
            }?.start()
    }

    fun dismiss() {
        handler.removeCallbacks(tickRunnable)
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
    }
}
