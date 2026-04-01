package com.example.zamgavocafront.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.model.WordData
import kotlin.math.abs

class NudgeTapOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val word: WordData,
    private val onDismissed: () -> Unit
) {
    private var overlayView: View? = null
    private lateinit var params: WindowManager.LayoutParams

    private var tapCount = 0
    private val REQUIRED_TAPS = 6
    private val TAP_RESET_MS = 2000L
    private val handler = Handler(Looper.getMainLooper())

    private val resetRunnable = Runnable {
        if (tapCount in 1 until REQUIRED_TAPS) {
            tapCount = 0
            //updateText()
        }
    }

    fun show() {
        if (overlayView != null) return

        val view = LayoutInflater.from(context)
            .inflate(R.layout.overlay_nudge_tap, null)

        view.findViewById<TextView>(R.id.tv_nudge_tap_word).text = word.word
        view.findViewById<TextView>(R.id.tv_nudge_tap_meaning).text = word.meaning

        val density = context.resources.displayMetrics.density
        val widthPx = (230 * density).toInt()
        val heightPx = (220 * density).toInt()

        params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = 300
        params.y = 500

        // 🔥 드래그 + 탭 통합
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        var isDragging = false
        val DRAG_THRESHOLD = 15

        view.setOnTouchListener { v, event ->
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (abs(dx) > DRAG_THRESHOLD || abs(dy) > DRAG_THRESHOLD) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(v, params)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        handleTap(v)
                    }
                    true
                }

                else -> false
            }
        }

        windowManager.addView(view, params)
        overlayView = view

        // 등장 애니메이션
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.alpha = 0f

        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(180)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun handleTap(view: View) {
        handler.removeCallbacks(resetRunnable)
        tapCount++
        //updateText()

        // 🔥 펌핑 애니메이션
        view.animate()
            .scaleX(1.25f)
            .scaleY(1.25f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(80)
                    .start()
            }
            .start()

        if (tapCount >= REQUIRED_TAPS) {
            view.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .withEndAction {
                    try {
                        windowManager.removeView(view)
                    } catch (_: Exception) {}
                    overlayView = null
                    onDismissed()
                }
                .start()
        } else {
            handler.postDelayed(resetRunnable, TAP_RESET_MS)
        }
    }

    /*
    private fun updateText() {
        overlayView?.findViewById<TextView>(R.id.tv_tap_count)
            ?.text = "$tapCount / $REQUIRED_TAPS"
    }
    */

    fun dismiss() {
        handler.removeCallbacks(resetRunnable)
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }
}