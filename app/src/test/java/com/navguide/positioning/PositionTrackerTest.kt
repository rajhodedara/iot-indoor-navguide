package com.navguide.positioning

import com.navguide.ble.IBeacon
import com.navguide.ble.IBeaconReading
import com.navguide.testutil.DemoMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PositionTrackerTest {

    private val doc = DemoMap.load()

    private fun reading(minor: Int, rssi: Int): IBeaconReading {
        val distance = IBeacon.distanceFromRssi(rssi, -59)
        return IBeaconReading(doc.uuid, 1, minor, -59, rssi, distance)
    }

    @Test
    fun `snaps after two consecutive strong readings`() {
        val tracker = PositionTracker(doc)
        assertNull(tracker.onReading(reading(2, -60))) // first hit: candidate only
        val snapped = tracker.onReading(reading(2, -60))
        assertEquals("ja", snapped)
        assertEquals("ja", tracker.currentNodeId)
    }

    @Test
    fun `weak or distant readings do not snap`() {
        val tracker = PositionTracker(doc)
        // rssi -90 is below snapRssiMin; also distance is far.
        repeat(5) { assertNull(tracker.onReading(reading(2, -90))) }
        assertNull(tracker.currentNodeId)
    }

    @Test
    fun `hysteresis prevents flapping between nodes`() {
        val tracker = PositionTracker(doc)
        // Settle on ja (minor 2).
        tracker.onReading(reading(2, -60)); tracker.onReading(reading(2, -60))
        assertEquals("ja", tracker.currentNodeId)
        // Now a slightly stronger reading for jb (minor 5) should NOT flip immediately
        // when the advantage is below the hysteresis threshold.
        repeat(4) { tracker.onReading(reading(5, -58)) }
        assertEquals("ja", tracker.currentNodeId)
        // A decisively stronger jb (rssi -50 vs ja smoothed around -60) flips it.
        repeat(3) { tracker.onReading(reading(5, -45)) }
        assertEquals("jb", tracker.currentNodeId)
    }

    @Test
    fun `unknown minor is ignored`() {
        val tracker = PositionTracker(doc)
        assertNull(tracker.onReading(reading(99, -50)))
        assertNull(tracker.currentNodeId)
    }

    @Test
    fun `force node switches immediately`() {
        val tracker = PositionTracker(doc)
        assertTrue(tracker.forceNode("jc"))
        assertEquals("jc", tracker.currentNodeId)
        assertTrue(!tracker.forceNode("jc"))
    }
}
