package com.miolonixc.vibro17.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.miolonixc.vibro17.model.VibroEffect

/**
 * Thin wrapper around the Android vibration API that gracefully degrades on
 * older devices (pre-API 26 have no amplitude control).
 */
class VibrationEngine(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /** Global intensity multiplier applied to every effect (0.0–1.0). */
    var intensity: Float = 1.0f

    val hasAmplitudeControl: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()

    @SuppressLint("MissingPermission")
    fun play(effect: VibroEffect, ampScale: Float = 1f) {
        val scale = (intensity * ampScale).coerceIn(0f, 1f)
        val base = if (effect.amplitudes.size == effect.timings.size) {
            effect.amplitudes
        } else {
            // Defensive: never let a malformed effect crash the app.
            IntArray(effect.timings.size) { i -> effect.amplitudes.getOrElse(i) { 0 } }
        }
        val amplitudes = if (scale < 1.0f) {
            IntArray(base.size) { i -> (base[i] * scale).toInt().coerceAtMost(255) }
        } else {
            base
        }
        val vibration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(effect.timings, amplitudes, effect.repeat)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createWaveform(effect.timings, effect.repeat)
        }
        vibrator.vibrate(vibration)
    }

    @SuppressLint("MissingPermission")
    fun oneShot(millis: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(millis)
        }
    }

    fun stop() {
        vibrator.cancel()
    }
}
