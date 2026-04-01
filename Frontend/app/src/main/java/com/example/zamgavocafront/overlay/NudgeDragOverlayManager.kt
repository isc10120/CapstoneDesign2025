package com.example.zamgavocafront.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.*
import android.widget.TextView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.model.WordData
import kotlin.math.abs

class NudgeDragOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val word: WordData,
    private val onDismissed: () -> Unit
) {
    private var overlayView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    private val handler = Handler(Looper.getMainLooper())
    private val REQUIRED_MS = 3000L

    // 드래그 상태
    private var isDraggingActive = false
    private var totalActiveDragMs = 0L
    private var lastCheckTime = 0L
    private var lastMovementTime = 0L

    // 위치 이동용
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // 🔥 드래그 시간 체크 루프
    private val tickRunnable = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            val timeSinceMove = now - lastMovementTime

            if (isDraggingActive && timeSinceMove < 150L) {
                totalActiveDragMs += (now - lastCheckTime).coerceAtMost(100L)
            } else if (timeSinceMove >= 250L) {
                totalActiveDragMs = 0L
            }

            lastCheckTime = now

            if (totalActiveDragMs >= REQUIRED_MS) {
                dismiss()
                onDismissed()
                return
            }

            handler.postDelayed(this, 30L)
        }
    }

    fun show() {
        if (overlayView != null) return

        val view = LayoutInflater.from(context)
            .inflate(R.layout.overlay_nudge_drag, null)

        // 텍스트 세팅
        view.findViewById<TextView>(R.id.tv_nudge_drag_word).text = word.word
        view.findViewById<TextView>(R.id.tv_nudge_drag_meaning).text = word.meaning

        // 🔥 크기 강제 (dp → px)
        val density = context.resources.displayMetrics.density
        val widthPx = (120 * density).toInt()
        val heightPx = (140 * density).toInt()

        params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = 200
        params.y = 400

        view.setOnTouchListener { v, event ->
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    isDraggingActive = true

                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    lastMovementTime = SystemClock.elapsedRealtime()
                    lastCheckTime = lastMovementTime
                    totalActiveDragMs = 0L

                    handler.removeCallbacks(tickRunnable)
                    handler.post(tickRunnable)

                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY)

                    params.x = initialX + dx
                    params.y = initialY + dy.toInt()

                    windowManager.updateViewLayout(v, params)

                    // 🔥 운동 느낌 (펌핑 애니메이션)
                    val scale = 1f + (abs(dy) / 800f).coerceAtMost(0.12f)
                    v.scaleX = scale
                    v.scaleY = scale

                    lastMovementTime = SystemClock.elapsedRealtime()
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    isDraggingActive = false
                    handler.removeCallbacks(tickRunnable)
                    totalActiveDragMs = 0L

                    // 🔥 원래 크기로 복귀
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()

                    true
                }

                else -> false
            }
        }

        windowManager.addView(view, params)
        overlayView = view
    }

    fun dismiss() {
        handler.removeCallbacks(tickRunnable)

        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }
}