package com.navguide.positioning

import kotlin.math.sqrt

/**
 * Step detection from accelerometer samples using smoothed magnitude peaks.
 * Pure Kotlin — feed it samples, it returns whether a step was detected.
 */
class StepDetector(
    private val threshold: Double = 1.5,
    private val minIntervalMs: Long = 250
) {
    private var smoothedMag = GRAVITY
    private var armed = true
    private var lastStepAtMs = 0L

    var stepCount: Int = 0
        private set

    fun reset() {
        stepCount = 0
        smoothedMag = GRAVITY
        armed = true
        lastStepAtMs = 0L
    }

    /** Feed one accelerometer sample; returns true when this sample completed a step. */
    fun onSample(x: Double, y: Double, z: Double, timestampMs: Long): Boolean {
        val mag = sqrt(x * x + y * y + z * z)
        smoothedMag += SMOOTHING_ALPHA * (mag - smoothedMag)
        val spike = smoothedMag - GRAVITY

        if (spike > threshold) {
            if (armed && timestampMs - lastStepAtMs >= minIntervalMs) {
                armed = false
                lastStepAtMs = timestampMs
                stepCount++
                return true
            }
        } else if (spike < threshold * 0.3) {
            armed = true
        }
        return false
    }

    companion object {
        private const val GRAVITY = 9.81
        private const val SMOOTHING_ALPHA = 0.25
    }
}
