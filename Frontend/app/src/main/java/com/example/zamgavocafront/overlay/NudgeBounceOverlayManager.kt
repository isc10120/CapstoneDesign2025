package com.example.zamgavocafront.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.model.WordData
import kotlin.math.abs
import kotlin.math.sqrt

class NudgeBounceOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager,
    private val word: WordData,
    private val onDismissed: () -> Unit
) {
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())

    private val REQUIRED_HITS = 4
    private val FRAME_DELAY_MS = 16L
    private val BASE_SPEED = 9f

    private var posX = 0f
    private var posY = 0f
    private var velX = BASE_SPEED
    private var velY = BASE_SPEED * 0.78f
    private var hitCount = 0

    private var widgetW = 0
    private var widgetH = 0

    private var screenW = 0
    private var screenH = 0

    private var flickStartX = 0f
    private var flickStartY = 0f
    private var flickStartTime = 0L
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var wasHittingWall = false

    private val frameRunnable = object : Runnable {
        override fun run() {
            val view = overlayView ?: return
            val lp = params ?: return

            // 이동
            posX += velX
            posY += velY

            velX *= 0.98f
            velY *= 0.98f

            if (abs(velX) < 1f) velX = if (velX >= 0) 1f else -1f
            if (abs(velY) < 1f) velY = if (velY >= 0) 1f else -1f

            val maxX = (screenW - widgetW).toFloat()
            val maxY = (screenH - widgetH).toFloat()

            // 벽 반사
            if (posX <= 0f) {
                posX = 0f
                velX = abs(velX) * 0.85f
            } else if (posX >= maxX) {
                posX = maxX
                velX = -abs(velX) * 0.85f
            }

            if (posY <= 0f) {
                posY = 0f
                velY = abs(velY) * 0.85f
            } else if (posY >= maxY) {
                posY = maxY
                velY = -abs(velY) * 0.85f
            }

            // 벽 충돌 감지
            val isHittingWall =
                posX <= 0f || posX >= maxX ||
                        posY <= 0f || posY >= maxY

            if (isHittingWall && !wasHittingWall) {
                onWallHit(view)
            }
            wasHittingWall = isHittingWall

            lp.x = posX.toInt()
            lp.y = posY.toInt()

            try {
                windowManager.updateViewLayout(view, lp)
            } catch (_: Exception) {
                return
            }

            handler.postDelayed(this, FRAME_DELAY_MS)
        }
    }

    fun show() {
        if (overlayView != null) return
        measureScreen()

        val density = context.resources.displayMetrics.density

        val widthPx = (120 * density).toInt()
        val heightPx = (140 * density).toInt()

        widgetW = widthPx
        widgetH = heightPx

        posX = ((screenW - widgetW) / 2).toFloat()
        posY = ((screenH - widgetH) / 2).toFloat()

        val view = LayoutInflater.from(context)
            .inflate(R.layout.overlay_nudge_bounce, null)

        view.findViewById<TextView>(R.id.tv_bounce_word).text = word.word
        view.findViewById<TextView>(R.id.tv_bounce_meaning).text = word.meaning

        val lp = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = posX.toInt()
            y = posY.toInt()
        }

        params = lp

        setupTouchFlick(view, lp)

        windowManager.addView(view, lp)
        overlayView = view

        handler.post(frameRunnable)

        view.scaleX = 0.6f
        view.scaleY = 0.6f
        view.alpha = 0f

        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun setupTouchFlick(view: View, lp: WindowManager.LayoutParams) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    handler.removeCallbacks(frameRunnable)
                    flickStartX = event.rawX
                    flickStartY = event.rawY
                    lastTouchX = flickStartX
                    lastTouchY = flickStartY
                    flickStartTime = System.currentTimeMillis()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastTouchX
                    val dy = event.rawY - lastTouchY

                    posX = (posX + dx).coerceIn(0f, (screenW - widgetW).toFloat())
                    posY = (posY + dy).coerceIn(0f, (screenH - widgetH).toFloat())

                    lp.x = posX.toInt()
                    lp.y = posY.toInt()

                    try {
                        windowManager.updateViewLayout(view, lp)
                    } catch (_: Exception) {}

                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val dt = (System.currentTimeMillis() - flickStartTime).coerceAtLeast(1L)

                    val rawVx = (event.rawX - flickStartX) / dt * FRAME_DELAY_MS
                    val rawVy = (event.rawY - flickStartY) / dt * FRAME_DELAY_MS

                    val speed = sqrt(rawVx * rawVx + rawVy * rawVy)

                    if (speed > 1f) {
                        val clamped = speed.coerceIn(BASE_SPEED * 0.7f, BASE_SPEED * 2.2f)
                        velX = rawVx / speed * clamped
                        velY = rawVy / speed * clamped
                    }

                    handler.post(frameRunnable)
                    true
                }

                else -> false
            }
        }
    }

    private fun onWallHit(view: View) {
        if (hitCount >= REQUIRED_HITS) return

        hitCount++

        // 충돌 애니메이션
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
            }.start()

        val progress = hitCount.toFloat() / REQUIRED_HITS
        view.alpha = 1f - (progress * 0.3f)

        if (hitCount == REQUIRED_HITS - 1) {
            view.animate().rotation(5f).setDuration(50).withEndAction {
                view.animate().rotation(-5f).setDuration(50).withEndAction {
                    view.animate().rotation(0f).setDuration(50).start()
                }.start()
            }.start()
        }

        if (hitCount >= REQUIRED_HITS) {
            handler.postDelayed({
                dismissWithSuccess()
            }, 200L)
        }
    }

    private fun dismissWithSuccess() {
        handler.removeCallbacks(frameRunnable)

        overlayView?.animate()
            ?.alpha(0f)
            ?.scaleX(0.5f)
            ?.scaleY(0.5f)
            ?.setDuration(300)
            ?.withEndAction {
                try {
                    overlayView?.let { windowManager.removeView(it) }
                } catch (_: Exception) {}

                overlayView = null
                onDismissed()
            }
            ?.start()
    }

    private fun measureScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            screenW = bounds.width()
            screenH = bounds.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            screenW = dm.widthPixels
            screenH = dm.heightPixels
        }
    }

    fun dismiss() {
        handler.removeCallbacks(frameRunnable)
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }
}