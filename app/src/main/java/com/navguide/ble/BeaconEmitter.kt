package com.navguide.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context

/**
 * Emits iBeacon advertisements (phone-as-beacon demo mode).
 */
class BeaconEmitter(private val context: Context) {

    private var activeCallback: AdvertiseCallback? = null

    private fun adapter() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    fun isSupported(): Boolean = adapter()?.isMultipleAdvertisementSupported == true

    fun start(uuid: String, major: Int, minor: Int, txPower: Int, onResult: (Boolean, String?) -> Unit) {
        val advertiser = try { adapter()?.bluetoothLeAdvertiser } catch (_: SecurityException) { null }
        if (advertiser == null) {
            onResult(false, "BLE advertising is not available on this phone.")
            return
        }
        val payload = IBeacon.buildPayload(uuid, major, minor, txPower)
        val data = AdvertiseData.Builder()
            .addManufacturerData(IBeacon.COMPANY_ID, payload)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()
        val cb = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                onResult(true, null)
            }

            override fun onStartFailure(errorCode: Int) {
                val msg = when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> "Beacon payload too large for this phone."
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many active advertisers on this phone."
                    ADVERTISE_FAILED_ALREADY_STARTED -> "Already broadcasting."
                    ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal Bluetooth error."
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "BLE advertising is unsupported on this phone."
                    else -> "Advertising failed (code $errorCode)."
                }
                onResult(false, msg)
            }
        }
        activeCallback = cb
        try {
            advertiser.startAdvertising(settings, data, cb)
        } catch (e: SecurityException) {
            onResult(false, "Bluetooth permission is missing.")
        }
    }

    fun stop() {
        val cb = activeCallback ?: return
        try {
            adapter()?.bluetoothLeAdvertiser?.stopAdvertising(cb)
        } catch (_: Exception) {
        } finally {
            activeCallback = null
        }
    }
}
