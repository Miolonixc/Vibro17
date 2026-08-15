package com.miolonixc.vibro17.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.miolonixc.vibro17.ui.Theme

/**
 * Lightweight waveform visualizer. While playing it draws an animated sine wave
 * whose amplitude is scaled by the active effect's strength; when idle it shows
 * a flat cyan line.
 */
class VisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
        color = Theme.accent(context)
    }
    private val dimPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
        color = Theme.accentDim(context)
    }

    private var amplitude = 0f
    private var playing = false
    private var phase = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val waves = 6

    private val tick = object : Runnable {
        override fun run() {
            if (!playing) return
            phase += 0.25f
            invalidate()
            handler.postDelayed(this, 16)
        }
    }

    fun setActive(maxAmplitude: Int, isPlaying: Boolean) {
        amplitude = (maxAmplitude / 255f).coerceIn(0f, 1f)
        playing = isPlaying
        if (isPlaying) {
            handler.removeCallbacks(tick)
            handler.post(tick)
        } else {
            handler.removeCallbacks(tick)
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(tick)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val mid = h / 2f
        val amp = if (playing) amplitude * (h / 2f - 6f) else 2f

        val path = android.graphics.Path()
        val step = 6f
        var first = true
        var x = 0f
        while (x <= w) {
            val norm = x / w
            val y = mid + kotlin.math.sin(norm * Math.PI.toFloat() * 2f * waves + phase) * amp *
                (0.6f + 0.4f * kotlin.math.sin(norm * Math.PI.toFloat()))
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
            x += step
        }
        canvas.drawPath(path, if (playing) paint else dimPaint)
    }
}
