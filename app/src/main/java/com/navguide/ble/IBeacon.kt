package com.navguide.ble

import kotlin.math.pow

/**
 * iBeacon reading parsed from a BLE advertisement. Pure Kotlin (JVM-testable).
 */
data class IBeaconReading(
    val uuid: String,
    val major: Int,
    val minor: Int,
    val txPower: Int,
    val rssi: Int,
    val distance: Double
)

/**
 * iBeacon wire format helper.
 *
 * Manufacturer-specific data payload (what BluetoothLeAdvertiser.addManufacturerData carries,
 * WITHOUT the 2-byte company ID):
 *
 *   [0]=0x02 [1]=0x15 | UUID (16 bytes) | major (2, big-endian) | minor (2, big-endian) | txPower (1, signed)
 *
 * Total 23 bytes; with company id 0x004C the full manufacturer data is 25 bytes.
 */
object IBeacon {

    const val COMPANY_ID = 0x004C
    const val DEFAULT_TX_POWER = -59

    fun uuidToBytes(uuid: String): ByteArray {
        val hex = uuid.replace("-", "")
        require(hex.length == 32) { "UUID must be 32 hex chars: $uuid" }
        val out = ByteArray(16)
        for (i in 0 until 16) {
            val hi = Character.digit(hex[i * 2], 16)
            val lo = Character.digit(hex[i * 2 + 1], 16)
            require(hi >= 0 && lo >= 0) { "Invalid hex in UUID: $uuid" }
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    fun bytesToUuid(bytes: ByteArray, offset: Int = 0): String {
        val sb = StringBuilder(36)
        for (i in 0 until 16) {
            sb.append(String.format("%02X", bytes[offset + i]))
            if (i == 3 || i == 5 || i == 7 || i == 9) sb.append('-')
        }
        return sb.toString()
    }

    /** Builds the 23-byte manufacturer payload (company ID is added by the advertising API). */
    fun buildPayload(uuid: String, major: Int, minor: Int, txPower: Int): ByteArray {
        val out = ByteArray(23)
        out[0] = 0x02
        out[1] = 0x15
        uuidToBytes(uuid).copyInto(out, 2)
        out[18] = ((major ushr 8) and 0xFF).toByte()
        out[19] = (major and 0xFF).toByte()
        out[20] = ((minor ushr 8) and 0xFF).toByte()
        out[21] = (minor and 0xFF).toByte()
        out[22] = txPower.toByte()
        return out
    }

    /**
     * Parses iBeacon manufacturer data (WITHOUT company ID). Returns null when the payload
     * does not look like an iBeacon advertisement.
     */
    fun parseManufacturerData(payload: ByteArray, rssi: Int): IBeaconReading? {
        if (payload.size < 23) return null
        if ((payload[0].toInt() and 0xFF) != 0x02 || (payload[1].toInt() and 0xFF) != 0x15) return null
        val uuid = bytesToUuid(payload, 2)
        val major = ((payload[18].toInt() and 0xFF) shl 8) or (payload[19].toInt() and 0xFF)
        val minor = ((payload[20].toInt() and 0xFF) shl 8) or (payload[21].toInt() and 0xFF)
        val txRaw = payload[22].toInt() // signed
        val txPower = if (txRaw in -100..0) txRaw else DEFAULT_TX_POWER
        return IBeaconReading(uuid, major, minor, txPower, rssi, distanceFromRssi(rssi, txPower))
    }

    /** Log-distance path-loss approximation. Weaker rssi -> larger distance. */
    fun distanceFromRssi(rssi: Int, txPower: Int = DEFAULT_TX_POWER): Double {
        val exp = (txPower - rssi) / 20.0
        return 10.0.pow(exp)
    }
}
