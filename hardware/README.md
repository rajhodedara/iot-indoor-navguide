# Hardware Notes

Two ways to add real beacons: buy iBeacon tags, or DIY with ESP32 boards.

## ESP32 DIY beacons

Firmware: [`esp32-beacon/esp32-beacon.ino`](esp32-beacon/esp32-beacon.ino)

Flash steps (Arduino IDE):
1. Install the **ESP32 board package** (Boards Manager → "esp32 by Espressif").
2. Open `esp32-beacon.ino`; set `BEACON_UUID`, `BEACON_MAJOR`, `BEACON_MINOR` for the node
   this board will represent (see `docs/MAP_FORMAT.md` for the demo map's minors).
3. Select the ESP32 board + port → Upload.
4. Verify with the **nRF Connect** app on a phone: the advertiser should be visible and the
   NavGuide app's "Near:" indicator should snap when you stand close.

Power: USB power bank (demo) or a protected 18650 cell (real install).

> Status: sketch provided for reference; not compile-verified in this repo. Check it in
> Arduino IDE before your submission demo.

## Real iBeacon tags

Buy generic nRF51822/nRF52832 iBeacon tags (iBeacon-compatible). Configure each with
nRF Connect (or the vendor app):

| Setting | Value |
|---|---|
| UUID | `E2C56DB5-DFFB-48D2-B060-D0F5A71096E0` (same as the app map) |
| Major | `1` |
| Minor | the node's `beaconMinor` |
| Tx Power | `-4 dBm` |
| Advertising interval | `300 ms` |

Prices & where to buy: see [`../docs/HARDWARE_OPTIONAL.md`](../docs/HARDWARE_OPTIONAL.md)
(~₹150–350/tag online; ₹2.5–5k for a 12-beacon floor).
