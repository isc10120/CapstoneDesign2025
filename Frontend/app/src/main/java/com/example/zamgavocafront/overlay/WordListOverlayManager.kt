package com.example.zamgavocafront.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.model.WordData
import kotlin.math.abs

/**
 * 오늘의 단어 목록 오버레이 – 헤더를 드래그해 이동 가능한 플로팅 카드.
 * 카드 영역을 좌우 스와이프하거나 ‹ › 버튼으로 단어를 넘길 수 있다.
 */
class WordListOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val words: List<WordData>,
    private val onClose: () -> Unit
) {
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var currentIndex = 0

    fun show() {
        if (overlayView != null) return

        val view = LayoutInflater.from(context).inflate(R.layout.overlay_word_list, null)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        params = lp

        updateWordDisplay(view)

        // 전체화면 스와이프 → 단어 이동
        val gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (abs(velocityX) > abs(velocityY)) {
                        if (velocityX < 0) navigateNext(view) else navigatePrev(view)
                    }
                    return true
                }
            }
        )

        view.findViewById<View>(R.id.word_card_container).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        view.findViewById<Button>(R.id.btn_prev_word).setOnClickListener { navigatePrev(view) }
        view.findViewById<Button>(R.id.btn_next_word).setOnClickListener { navigateNext(view) }
        view.findViewById<Button>(R.id.btn_close_word_list).setOnClickListener { onClose() }

        windowManager.addView(view, lp)
        overlayView = view

        // 팝인 애니메이션
        view.scaleX = 0.85f; view.scaleY = 0.85f; view.alpha = 0f
        view.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(220).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun navigatePrev(view: View) {
        if (currentIndex <= 0) return
        currentIndex--
        animateWordChange(view, fromRight = false)
    }

    private fun navigateNext(view: View) {
        if (currentIndex >= words.size - 1) return
        currentIndex++
        animateWordChange(view, fromRight = true)
    }

    private fun animateWordChange(view: View, fromRight: Boolean) {
        val container = view.findViewById<View>(R.id.word_card_container)
        val dir = if (fromRight) 1f else -1f
        container.animate().translationX(-dir * 60f).alpha(0f).setDuration(120).withEndAction {
            updateWordDisplay(view)
            container.translationX = dir * 60f
            container.alpha = 0f
            container.animate().translationX(0f).alpha(1f).setDuration(120).start()
        }.start()
    }

    private fun updateWordDisplay(view: View) {
        val word = words[currentIndex]
        view.findViewById<TextView>(R.id.tv_word).text = word.word
        view.findViewById<TextView>(R.id.tv_meaning).text = word.meaning
        view.findViewById<TextView>(R.id.tv_word_progress).text =
            "${currentIndex + 1}/${words.size}"
    }

    fun dismiss() {
        overlayView?.let { v ->
            try { windowManager.removeView(v) } catch (_: Exception) {}
            overlayView = null
        }
    }
}
