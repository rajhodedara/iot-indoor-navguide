package com.navguide.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IBeaconTest {

    private val uuid = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"

    @Test
    fun `payload is 23 bytes and starts with iBeacon magic`() {
        val p = IBeacon.buildPayload(uuid, 1, 3, -59)
        assertEquals(23, p.size)
        assertEquals(0x02, p[0].toInt() and 0xFF)
        assertEquals(0x15, p[1].toInt() and 0xFF)
    }

    @Test
    fun `round trip uuid major minor txPower`() {
        val p = IBeacon.buildPayload(uuid, 1, 3, -59)
        val r = IBeacon.parseManufacturerData(p, rssi = -59)!!
        assertEquals(uuid, r.uuid)
        assertEquals(1, r.major)
        assertEquals(3, r.minor)
        assertEquals(-59, r.txPower)
        assertEquals(-59, r.rssi)
        assertEquals(1.0, r.distance, 0.5)
    }

    @Test
    fun `boundary minors 0 and 65535 survive round trip`() {
        for (m in listOf(0, 65535, 256, 255)) {
            val p = IBeacon.buildPayload(uuid, 0, m, -59)
            val r = IBeacon.parseManufacturerData(p, -70)!!
            assertEquals(m, r.minor)
        }
    }

    @Test
    fun `non iBeacon payload returns null`() {
        val junk = ByteArray(23) { 0x11 }
        assertNull(IBeacon.parseManufacturerData(junk, -60))
        assertNull(IBeacon.parseManufacturerData(ByteArray(10), -60))
    }

    @Test
    fun `distance model is monotonic and sane`() {
        val near = IBeacon.distanceFromRssi(-59, -59)
        val mid = IBeacon.distanceFromRssi(-75, -59)
        val far = IBeacon.distanceFromRssi(-90, -59)
        assertTrue(near < mid)
        assertTrue(mid < far)
        assertEquals(1.0, near, 0.01)
        assertTrue(mid > 3.0 && mid < 10.0)
    }

    @Test
    fun `uuid bytes round trip`() {
        val bytes = IBeacon.uuidToBytes(uuid)
        assertEquals(16, bytes.size)
        assertEquals(uuid, IBeacon.bytesToUuid(bytes))
    }
}
