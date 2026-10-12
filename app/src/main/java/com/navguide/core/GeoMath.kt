package com.navguide.core

import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max

/**
 * Coordinate math helpers. All angles in degrees. Screen coordinates:
 * x grows right, y grows DOWN (so north on a floor plan = negative y direction).
 */
object GeoMath {

    fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double = hypot(x2 - x1, y2 - y1)

    /** Bearing of the vector p1 -> p2, degrees in [0, 360). 0 = +x direction. */
    fun bearing(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val deg = Math.toDegrees(atan2(y2 - y1, x2 - x1))
        return (deg + 360.0) % 360.0
    }

    /** Signed smallest difference b - a, in (-180, 180]. */
    fun angleDiffDeg(a: Double, b: Double): Double {
        var d = (b - a) % 360.0
        if (d > 180.0) d -= 360.0
        if (d <= -180.0) d += 360.0
        return d
    }

    /** Steps needed for a distance, at least 1. */
    fun stepsFor(meters: Double, strideM: Double): Int = max(1, ceil(meters / strideM).toInt())
}
