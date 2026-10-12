# Optional Hardware (real beacons & DIY ESP32 beacons)

You do **not** need any hardware for the demo (use Beacon Mode / Simulator). When you want the
real thing, two cheap paths:

## Option A — buy iBeacon tags (easiest)

| Item | Notes | Approx. price (India) |
|---|---|---|
| Generic iBeacon tag (nRF51822, CR2032) | iBeacon-compatible, config via app | ₹150–350 each (online; IndiaMART/AliExpress) |
| Generic nRF52832 beacon (AA/AAA) | longer battery life (2–4 yrs) | ₹500–1,200 each |
| CR2032 batteries | spares | ₹10–20 each |
| Mounting tape (3M VHB) | ceiling/wall mounting at 2–2.5 m | ₹300 for a roll |

For a 12-beacon floor: **~₹2,500–5,000** (CR2032 tags) — more with double-AA models.
Note: at Lamington Road / Grant Road markets, ready-made beacon tags are usually *order-in*;
ESP32 boards, batteries and tools are available on the spot.

**Configure each tag** (with the nRF Connect app, for example):

| Setting | Value |
|---|---|
| UUID | `E2C56DB5-DFFB-48D2-B060-D0F5A71096E0` (same as demo map) |
| Major | `1` |
| Minor | node's `beaconMinor` from your map (1…12 in the demo map) |
| Tx Power | `-4 dBm` (≈15–20 m range) |
| Advertising interval | `300 ms` |
| Battery-friendly mode | anything ≥ 300 ms is fine for this app |

Mounting tips: 2–2.5 m high, not on metal, antenna not hidden behind concrete columns.

## Option B — DIY ESP32 beacons (cheapest, most "IoT")

`hardware/esp32-beacon/esp32-beacon.ino` makes an ESP32 board broadcast as an iBeacon.

- Board: any ESP32 dev board (₹300–500).
- Power: USB power bank, or 18650 cell with holder (~₹300 total).
- Cost per beacon: ~₹350–600 vs ₹150–350 for tags — *tags usually win on cost*; ESP32 wins if
  you want to also blink LEDs, read sensors, or learn the BLE stack (or if you must buy locally).

Flash it with Arduino IDE (ESP32 board package). Edit the constants at the top of the sketch
(UUID / MAJOR / MINOR / TX_POWER) per node and reflash (or build 12 and label them).

> This sketch is provided for completeness and has not been compiled in this repository's
> environment. Verify in Arduino IDE before relying on it for your submission.

## Which to choose for the final submission?

| Goal | Recommendation |
|---|---|
| Just demo it | Phones in Beacon Mode (₹0) |
| "Real IoT" credibility, minimal budget | 2–3 iBeacon tags (₹500–1,000 total) |
| Learn + tinker + local shopping | ESP32 boards |
| Full building deployment | 12 tags, spread one weekend afternoon to install |

## Signal sanity check (5 minutes)

1. Stand under a beacon: RSSI ≈ −50 to −60 dBm (check with nRF Connect).
2. Walk 10 m away: RSSI ≈ −75 to −85.
3. If the app snaps too early/late, adjust in code:
   `PositionTracker(snapDistanceM = 3.0, snapRssiMin = -75, hysteresisDb = 6)`.
