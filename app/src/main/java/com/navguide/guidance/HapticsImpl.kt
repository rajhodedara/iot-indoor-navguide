package com.navguide.guidance

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Vibration patterns for left / right / straight / arrival / off-route / SOS. */
class HapticsImpl(context: Context) : Haptics {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun play(pattern: HapticPattern) {
        if (!vibrator.hasVibrator()) return
        val effect = when (pattern) {
            HapticPattern.STRAIGHT ->
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)

            HapticPattern.LEFT ->
                VibrationEffect.createWaveform(longArrayOf(0, 120, 100, 120), -1)

            HapticPattern.RIGHT ->
                VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)

            HapticPattern.ARRIVAL ->
                VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)

            HapticPattern.OFF_ROUTE ->
                VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 600), -1)

            HapticPattern.SOS_ALARM ->
                VibrationEffect.createWaveform(longArrayOf(0, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60), -1)

            HapticPattern.TRANSITION ->
                // Three short pulses: "steps up/down"
                VibrationEffect.createWaveform(longArrayOf(0, 120, 120, 120, 120, 360), -1)
        }
        vibrator.vibrate(effect)
    }

    /** Plays each pattern in sequence with spoken gaps handled by caller UI. */
    fun playSequence(patterns: List<HapticPattern>, gapMs: Long = 900) {
        val handler = Handler(Looper.getMainLooper())
        patterns.forEachIndexed { index, p ->
            handler.postDelayed({ play(p) }, gapMs * index)
        }
    }
}
