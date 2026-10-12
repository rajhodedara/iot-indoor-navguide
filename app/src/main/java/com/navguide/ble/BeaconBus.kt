package com.navguide.ble

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Process-wide event bus. Real BLE scans (BleScannerService) and the demo Simulator both
 * publish here; the navigation activity subscribes.
 */
object BeaconBus {
    /** iBeacon readings from scans or the simulator. */
    val readings = MutableSharedFlow<IBeaconReading>(extraBufferCapacity = 64)

    /** Simulated step pulses (each value = number of steps to add). */
    val stepPulses = MutableSharedFlow<Int>(extraBufferCapacity = 64)

    /** Simulated "user is now at node X" hops. */
    val nodeHops = MutableSharedFlow<String>(extraBufferCapacity = 16)
}
