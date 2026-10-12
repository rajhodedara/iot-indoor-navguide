package com.navguide.positioning

import com.navguide.ble.IBeaconReading
import com.navguide.core.MapDoc

/**
 * Beacon-proximity position tracking with hysteresis. Pure Kotlin.
 *
 * Rules:
 *  - A node is a snap candidate when the reading's estimated distance < snapDistanceM
 *    and rssi >= snapRssiMin.
 *  - Switching to a new node requires [confirmationsNeeded] consecutive candidate readings
 *    (~0.5s) and, when a node is already held, a signal advantage of [hysteresisDb]
 *    over the currently held node's smoothed rssi.
 */
class PositionTracker(
    private val doc: MapDoc,
    private val snapDistanceM: Double = 3.0,
    private val snapRssiMin: Int = -75,
    private val hysteresisDb: Int = 6,
    private val confirmationsNeeded: Int = 2
) {
    var currentNodeId: String? = null
        private set

    private val smoothedRssi = mutableMapOf<Int, Int>()
    private var candidateMinor: Int? = null
    private var candidateHits = 0

    /** Force the position (used by the simulator). Returns true if the node changed. */
    fun forceNode(nodeId: String?): Boolean {
        val changed = nodeId != currentNodeId
        currentNodeId = nodeId
        candidateMinor = null
        candidateHits = 0
        return changed
    }

    /**
     * Process a beacon reading. Returns the NEW node id when a snap change occurred,
     * otherwise null.
     */
    fun onReading(reading: IBeaconReading): String? {
        val node = doc.nodeByMinor[reading.minor] ?: return null

        // Smooth rssi per beacon (simple EMA).
        val prev = smoothedRssi[reading.minor]
        smoothedRssi[reading.minor] = if (prev == null) reading.rssi else (prev * 2 + reading.rssi) / 3

        if (currentNodeId == node.id) {
            candidateMinor = null
            candidateHits = 0
            return null
        }

        val good = reading.distance < snapDistanceM && reading.rssi >= snapRssiMin
        if (!good) {
            if (candidateMinor == reading.minor) {
                candidateMinor = null
                candidateHits = 0
            }
            return null
        }

        if (candidateMinor == reading.minor) {
            candidateHits++
        } else {
            candidateMinor = reading.minor
            candidateHits = 1
        }

        if (candidateHits >= confirmationsNeeded) {
            val currentMinor = currentNodeId?.let { id -> doc.nodeById[id]?.beaconMinor }
            val currentSmoothed = currentMinor?.let { smoothedRssi[it] } ?: Int.MIN_VALUE
            val candidateSmoothed = smoothedRssi[reading.minor] ?: reading.rssi
            if (currentNodeId == null || candidateSmoothed - currentSmoothed >= hysteresisDb) {
                currentNodeId = node.id
                candidateMinor = null
                candidateHits = 0
                return node.id
            }
        }
        return null
    }

    fun reset() {
        currentNodeId = null
        candidateMinor = null
        candidateHits = 0
        smoothedRssi.clear()
    }
}
