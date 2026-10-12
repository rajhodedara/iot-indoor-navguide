/*
 * NavGuide ESP32 iBeacon emitter
 * ---------------------------------
 * Turns an ESP32 dev board into a BLE iBeacon matching the NavGuide app's map format.
 * Edit the constants below for each beacon, flash via Arduino IDE (ESP32 board support).
 *
 * NOTE: This sketch has not been compile-verified in the NavGuide repository environment;
 * verify in Arduino IDE before relying on it.
 *
 * Wiring: none (uses the ESP32's built-in radio). Power: USB power bank or 3.7V Li-ion.
 */

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEBeacon.h>

// ============ CONFIGURE PER BEACON ============
// Must match demo_map.json "uuid" and the node's "beaconMinor".
#define BEACON_UUID   "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"
#define BEACON_MAJOR  1        // building id (1 = the demo building)
#define BEACON_MINOR  2        // <-- change per node (1..12 in the demo map)
#define TX_POWER      -4       // dBm (typical range 15-20 m)
// ==============================================

// Advertising interval in units of 0.625 ms. 300 ms = 480 units (battery/latency balance).
#define ADV_INTERVAL_UNITS 480

void setup() {
  Serial.begin(115200);
  Serial.println("NavGuide beacon starting...");

  BLEDevice::init("NavGuide");

  BLEAdvertising *adv = BLEDevice::getAdvertising();
  BLEBeacon beacon;
  beacon.setManufacturerId(0x004C);  // Apple Inc. — iBeacon manufacturer id
  beacon.setProximityUUID(BLEUUID(BEACON_UUID));
  beacon.setMajor((uint16_t)BEACON_MAJOR);
  beacon.setMinor((uint16_t)BEACON_MINOR);

  BLEAdvertisementData advData;
  advData.setFlags(0x1A);
  advData.setManufacturerData(beacon.getData());

  adv->setAdvertisementData(advData);
  adv->setMinInterval(ADV_INTERVAL_UNITS);
  adv->setMaxInterval(ADV_INTERVAL_UNITS);
  adv->setScanResponse(false);
  adv->start();

  Serial.printf("Broadcasting UUID=%s major=%d minor=%d txPower=%d\n",
                BEACON_UUID, BEACON_MAJOR, BEACON_MINOR, TX_POWER);
}

void loop() {
  delay(2000);
  // Nothing to do; advertising runs in the background.
}
