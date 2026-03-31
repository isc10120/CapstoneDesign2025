package com.example.zamgavocafront.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.model.WordData

/**
 * 넛지 ② – 연타 해제
 * 카드를 6회 연속으로 탭하면 오버레이가 사라진다.
 * 2초 이상 탭이 없으면 카운터가 초기화된다.
 */
class NudgeTapOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val word: WordData,
    private val onDismissed: () -> Unit
) {
    private var overlayView: View? = null
    private var tapCount = 0
    private val REQUIRED_TAPS = 6
    private val TAP_RESET_MS = 2000L
    private val handler = Handler(Looper.getMainLooper())
    private val dotViews = mutableListOf<View>()

    private val resetRunnable = Runnable {
        if (tapCount in 1 until REQUIRED_TAPS) {
            tapCount = 0
            updateDots()
            overlayView?.findViewById<TextView>(R.id.tv_tap_count)?.text = "0 / $REQUIRED_TAPS"
        }
    }

    fun show() {
        if (overlayView != null) return

        val view = LayoutInflater.from(context).inflate(R.layout.overlay_nudge_tap, null)
        view.findViewById<TextView>(R.id.tv_nudge_tap_word).text = word.word
        view.findViewById<TextView>(R.id.tv_nudge_tap_meaning).text = word.meaning

        buildDots(view)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        view.setOnClickListener { handleTap(view) }

        windowManager.addView(view, params)
        overlayView = view

        view.scaleX = 0.85f; view.scaleY = 0.85f; view.alpha = 0f
        view.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(220).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun handleTap(view: View) {
        handler.removeCallbacks(resetRunnable)
        tapCount++
        updateDots()

        val countTv = view.findViewById<TextView>(R.id.tv_tap_count)
        countTv.text = "$tapCount / $REQUIRED_TAPS"

        // 카운터 펄스 애니메이션
        countTv.animate().scaleX(1.35f).scaleY(1.35f).setDuration(70).withEndAction {
            countTv.animate().scaleX(1f).scaleY(1f).setDuration(70).start()
        }.start()

        if (tapCount >= REQUIRED_TAPS) {
            view.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(280).withEndAction {
                try { windowManager.removeView(view) } catch (_: Exception) {}
                overlayView = null
                dotViews.clear()
                onDismissed()
            }.start()
        } else {
            handler.postDelayed(resetRunnable, TAP_RESET_MS)
        }
    }

    private fun buildDots(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.tap_dots_container)
        val density = context.resources.displayMetrics.density
        val sizePx = (20 * density).toInt()
        val marginPx = (5 * density).toInt()

        repeat(REQUIRED_TAPS) {
            val dot = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins(marginPx, 0, marginPx, 0)
                }
                background = makeDotDrawable(false)
            }
            container.addView(dot)
            dotViews.add(dot)
        }
    }

    private fun updateDots() {
        dotViews.forEachIndexed { i, dot ->
            dot.background = makeDotDrawable(i < tapCount)
        }
    }

    private fun makeDotDrawable(filled: Boolean): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (filled) {
                setColor(0xFFF4A700.toInt())
            } else {
                setColor(Color.TRANSPARENT)
                setStroke((2 * density).toInt(), 0xFFF4A700.toInt())
            }
        }
    }

    fun dismiss() {
        handler.removeCallbacks(resetRunnable)
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
        dotViews.clear()
    }
}
