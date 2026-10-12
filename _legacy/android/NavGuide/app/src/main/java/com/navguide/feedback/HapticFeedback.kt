package com.navguide.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedback(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    enum class Pattern {
        SHORT_PULSE, // Left
        LONG_PULSE, // Right
        DOUBLE_PULSE, // Straight
        SMOOTH_LONG, // Arrival
        RAPID_STACCATO, // Danger
        ALTERNATING // Off-route
    }

    fun playPattern(pattern: Pattern) {
        if (!vibrator.hasVibrator()) return

        val effect = when (pattern) {
            Pattern.SHORT_PULSE -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            Pattern.LONG_PULSE -> VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            Pattern.DOUBLE_PULSE -> VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1)
            Pattern.SMOOTH_LONG -> VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
            Pattern.RAPID_STACCATO -> VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50, 50, 50, 50), -1)
            Pattern.ALTERNATING -> VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 400, 100, 200), -1)
        }

        vibrator.vibrate(effect)
    }
}
