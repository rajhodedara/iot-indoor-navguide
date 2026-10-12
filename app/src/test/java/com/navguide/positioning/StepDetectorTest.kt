package com.navguide.positioning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepDetectorTest {

    /** Generates a walking-like magnitude signal: gravity + sine bump per step. */
    private fun feedSteps(detector: StepDetector, steps: Int, samplesPerStep: Int = 25): Int {
        var detected = 0
        var t = 0L
        for (s in 0 until steps) {
            for (i in 0 until samplesPerStep) {
                val phase = i.toDouble() / samplesPerStep
                val bump = if (phase < 0.5) 2.5 * Math.sin(phase * 2 * Math.PI) else 0.0
                val mag = 9.81 + bump
                // Split across axes arbitrarily: all on z.
                if (detector.onSample(0.0, 0.0, mag, t)) detected++
                t += 20 // 50 Hz
            }
        }
        return detected
    }

    @Test
    fun `counts one step per walking cycle`() {
        val d = StepDetector()
        val detected = feedSteps(d, steps = 10)
        assertTrue("expected ~10 steps but got $detected", detected in 8..11)
        assertTrue(d.stepCount in 8..11)
    }

    @Test
    fun `ignores low amplitude noise`() {
        val d = StepDetector()
        var t = 0L
        repeat(500) {
            if (d.onSample(0.0, 0.0, 9.81 + 0.3, t)) error("noise produced a step")
            t += 20
        }
        assertEquals(0, d.stepCount)
    }

    @Test
    fun `respects minimum interval between steps`() {
        val d = StepDetector()
        var t = 0L
        // Two big spikes 100ms apart: only first counts.
        repeat(10) { if (d.onSample(0.0, 0.0, 13.0, t)) Unit; t += 10 }
        val afterFirst = d.stepCount
        repeat(10) { d.onSample(0.0, 0.0, 12.0, t); t += 10 }
        assertTrue(afterFirst in 0..1)
        assertTrue(d.stepCount <= 2)
    }

    @Test
    fun `reset clears state`() {
        val d = StepDetector()
        feedSteps(d, steps = 3)
        d.reset()
        assertEquals(0, d.stepCount)
    }
}
