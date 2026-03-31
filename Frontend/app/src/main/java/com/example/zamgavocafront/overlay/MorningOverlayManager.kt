package com.example.zamgavocafront.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import com.example.zamgavocafront.R
import com.example.zamgavocafront.model.Difficulty

/**
 * 아침 오버레이 – 전체화면 반투명 오버레이로 오늘의 학습 난이도를 선택한다.
 * 난이도 선택 시 [onDifficultySelected] 콜백을 호출한 뒤 자동으로 닫힌다.
 */
class MorningOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onDifficultySelected: (Difficulty) -> Unit
) {
    private var overlayView: View? = null

    fun show() {
        if (overlayView != null) return

        val view = LayoutInflater.from(context).inflate(R.layout.overlay_morning, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        view.findViewById<Button>(R.id.btn_difficulty_easy).setOnClickListener {
            onDifficultySelected(Difficulty.EASY)
        }
        view.findViewById<Button>(R.id.btn_difficulty_medium).setOnClickListener {
            onDifficultySelected(Difficulty.MEDIUM)
        }
        view.findViewById<Button>(R.id.btn_difficulty_hard).setOnClickListener {
            onDifficultySelected(Difficulty.HARD)
        }

        windowManager.addView(view, params)
        overlayView = view

        // 팝인 애니메이션
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(250).setInterpolator(DecelerateInterpolator()).start()
    }

    fun dismiss() {
        overlayView?.let { v ->
            v.animate().alpha(0f).setDuration(200).withEndAction {
                try { windowManager.removeView(v) } catch (_: Exception) {}
            }.start()
            overlayView = null
        }
    }
}
