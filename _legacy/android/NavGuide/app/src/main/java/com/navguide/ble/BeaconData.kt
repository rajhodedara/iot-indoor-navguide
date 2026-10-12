package com.navguide.ble

data class BeaconData(
    val uuid: String,
    val major: Int,
    val minor: Int,
    val rssi: Int,
    val distance: Double, // Estimated distance
    val timestamp: Long = System.currentTimeMillis()
)
