package com.navguide.ble

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Process-wide event bus. Real BLE scans (BleScannerService) publish here;
 * the navigation activity subscribes.
 *
 * NOTE: nodeHops/stepPulses are retained only because SimulatorActivity still references
 * them pending its removal (deletion requires user approval). NavigationActivity now hosts
 * its own inline demo controls instead.
 */
object BeaconBus {
    /** iBeacon readings from scans. */
    val readings = MutableSharedFlow<IBeaconReading>(extraBufferCapacity = 64)

    /** Legacy simulator hooks (currently unused by live code). */
    val stepPulses = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    val nodeHops = MutableSharedFlow<String>(extraBufferCapacity = 16)
}
