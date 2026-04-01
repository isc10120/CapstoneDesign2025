package com.example.zamgavocafront.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.model.WordData

class WordListOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val words: List<WordData>,
    private val onClose: () -> Unit
) {
    private var overlayView: View? = null
    private var currentIndex = 0

    fun show() {
        if (overlayView != null) return

        val view = LayoutInflater.from(context)
            .inflate(R.layout.overlay_word_list, null)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = view

        // 🔥 순서 중요 (dot 초기화 버그 해결)
        setupDots(view)
        updateWordDisplay(view)
        setupSwipe(view)

        view.findViewById<View>(R.id.btn_close).setOnClickListener { onClose() }
        view.findViewById<View>(R.id.overlay_root).setOnClickListener { onClose() }
        view.findViewById<View>(R.id.word_card).setOnClickListener { }

        windowManager.addView(view, lp)

        // 등장 애니메이션
        view.scaleX = 0.85f
        view.scaleY = 0.85f
        view.alpha = 0f

        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // ✅ 단어 하이라이트 (형태 변형 포함: s, ed, ing 등)
    private fun highlightWord(text: String, word: String): SpannableString {
        val spannable = SpannableString(text)

        val regex = Regex("\\b${Regex.escape(word)}\\w*\\b", RegexOption.IGNORE_CASE)

        regex.findAll(text).forEach {
            spannable.setSpan(
                ForegroundColorSpan(
                    context.getColor(R.color.main_yellow) // 노란색
                ),
                it.range.first,
                it.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannable
    }

    private fun setupSwipe(view: View) {
        val content = view.findViewById<View>(R.id.card_content)

        var downX = 0f

        content.setOnTouchListener { v, event ->
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    v.translationX = dx
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - downX

                    if (dx < -150) navigateNext(view)
                    else if (dx > 150) navigatePrev(view)

                    v.animate()
                        .translationX(0f)
                        .setDuration(150)
                        .start()
                    true
                }

                else -> false
            }
        }
    }

    // ✅ 무한 순환
    private fun navigatePrev(view: View) {
        currentIndex =
            if (currentIndex == 0) words.lastIndex
            else currentIndex - 1

        animateWordChange(view, fromRight = false)
    }

    private fun navigateNext(view: View) {
        currentIndex =
            if (currentIndex == words.lastIndex) 0
            else currentIndex + 1

        animateWordChange(view, fromRight = true)
    }

    private fun animateWordChange(view: View, fromRight: Boolean) {
        val content = view.findViewById<View>(R.id.card_content)
        val dir = if (fromRight) 1f else -1f

        content.animate()
            .translationX(-dir * 80f)
            .alpha(0f)
            .setDuration(120)
            .withEndAction {

                updateWordDisplay(view)

                content.translationX = dir * 80f
                content.alpha = 0f

                content.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun updateWordDisplay(view: View) {
        val word = words[currentIndex]

        view.findViewById<TextView>(R.id.tv_word).text = word.word
        view.findViewById<TextView>(R.id.tv_meaning).text = word.meaning

        val exampleEn = view.findViewById<TextView>(R.id.tv_example_en)
        exampleEn.text = highlightWord(word.exampleEn, word.word)

        view.findViewById<TextView>(R.id.tv_example_kr).text = word.exampleKr

        view.findViewById<TextView>(R.id.tv_header).text =
            "오늘의 단어 (${currentIndex + 1}/${words.size})"

        updateDots(view)
    }

    private fun setupDots(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.dot_container)
        container.removeAllViews()

        repeat(words.size) {
            val dot = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(24, 24).apply {
                    marginEnd = 6
                }
                setBackgroundResource(android.R.drawable.presence_invisible)
                alpha = 0.3f
            }
            container.addView(dot)
        }
    }

    private fun updateDots(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.dot_container)

        for (i in 0 until container.childCount) {
            container.getChildAt(i).alpha =
                if (i == currentIndex) 1f else 0.3f
        }
    }

    fun dismiss() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }
}