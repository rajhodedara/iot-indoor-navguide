package com.navguide.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    @Test
    fun `distance basic`() {
        assertEquals(5.0, GeoMath.distance(0.0, 0.0, 3.0, 4.0), 1e-9)
    }

    @Test
    fun `bearing east is 0 and south is 90 with y-down coordinates`() {
        assertEquals(0.0, GeoMath.bearing(0.0, 0.0, 10.0, 0.0), 1e-9)
        assertEquals(90.0, GeoMath.bearing(0.0, 0.0, 0.0, 10.0), 1e-9)
        assertEquals(180.0, GeoMath.bearing(0.0, 0.0, -10.0, 0.0), 1e-9)
        assertEquals(270.0, GeoMath.bearing(0.0, 0.0, 0.0, -10.0), 1e-9)
    }

    @Test
    fun `angle diff wraps correctly`() {
        assertEquals(20.0, GeoMath.angleDiffDeg(350.0, 10.0), 1e-9)
        assertEquals(-20.0, GeoMath.angleDiffDeg(10.0, 350.0), 1e-9)
        assertEquals(180.0, Math.abs(GeoMath.angleDiffDeg(0.0, 180.0)), 1e-9)
        assertEquals(-90.0, GeoMath.angleDiffDeg(90.0, 0.0), 1e-9)
    }

    @Test
    fun `steps for distance rounds up with minimum one`() {
        assertEquals(9, GeoMath.stepsFor(6.0, 0.7))
        assertEquals(1, GeoMath.stepsFor(0.1, 0.7))
        assertEquals(12, GeoMath.stepsFor(8.0, 0.7))
        assertTrue(GeoMath.stepsFor(0.0, 0.7) >= 1)
    }
}
