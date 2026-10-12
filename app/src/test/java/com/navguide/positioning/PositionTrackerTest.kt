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
        assertEquals("stair1", snapped)
        assertEquals("stair1", tracker.currentNodeId)
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
        // Settle on stair1 (minor 2).
        tracker.onReading(reading(2, -60)); tracker.onReading(reading(2, -60))
        assertEquals("stair1", tracker.currentNodeId)
        // A slightly stronger reading for corridor1 (minor 3) should NOT flip immediately
        // when the advantage is below the hysteresis threshold.
        repeat(4) { tracker.onReading(reading(3, -56)) }
        assertEquals("stair1", tracker.currentNodeId)
        // A decisively stronger corridor1 (rssi -45 vs stair1 around -60) flips it.
        repeat(4) { tracker.onReading(reading(3, -45)) }
        assertEquals("corridor1", tracker.currentNodeId)
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
        assertTrue(tracker.forceNode("s28"))
        assertEquals("s28", tracker.currentNodeId)
        assertTrue(!tracker.forceNode("s28"))
    }
}
