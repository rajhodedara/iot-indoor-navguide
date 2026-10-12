# Hardware Setup Guide

This document outlines the hardware specifications, configuration, and placement strategy for the BLE Beacons used in the IoT Indoor Navigation System.

## 1. BLE Beacon Specifications & Recommendations

For a single department building requiring approximately 20 beacons, we recommend standard iBeacon-compatible devices. 

| Beacon Type | Pros | Cons | Est. Price / Unit |
|-------------|------|------|-------------------|
| **Generic nRF51822 (CR2032)** | Very cheap, small form factor | Shorter battery life (6-12 mo) | $5 - $10 |
| **Generic nRF52832 (AA / AAA)** | Longer battery life (2-4 yrs), strong signal | Bulky, harder to hide | $12 - $18 |
| **Brand (Kontakt.io / Estimote)** | Excellent SDKs, highly reliable | Expensive, overkill for PoC | $25 - $40 |

**Recommendation**: Generic nRF52832 beacons powered by AA batteries. They fit within the ~$300 budget and reduce maintenance overhead.

## 2. Configuration Scheme

All beacons must be configured prior to mounting. Use the manufacturer's provided mobile app (e.g., nRF Connect) to set the following parameters:

- **UUID**: `E2C56DB5-DFFB-48D2-B060-D0F5A71096E0` (Use one uniform UUID for the entire university/project)
- **Major**: `1` (Represents the Building or Department ID)
- **Minor**: `1` to `20` (Represents the specific location/node in the building)
- **Tx Power**: `-4 dBm` (Good balance between range and battery life, ~15-20 meters line-of-sight)
- **Advertising Interval**: `300 ms` (Ensures the smartphone picks up the signal quickly during walking)

## 3. Placement Strategy

Beacons are used for proximity location and node-to-node routing. Place them at key decision points.

**Key Placement Rules:**
1. Intersections and hallways.
2. Entrances to important rooms (e.g., lecture halls, restrooms).
3. Stairs or elevators (Note: label these clearly in the admin portal to trigger floor-change events).

### Sample Floor Layout

```text
    [Room 101]       [Room 102]       [Restrooms]
       |                |                |
-------(B1)------------(B2)-------------(B3)--------- [Exit]
       |                                 |
    [Room 103]                       [Stairs B4]
```
*`B1`, `B2`, `B3`, `B4` represent Beacon locations.*

## 4. Mounting Instructions

- **Height**: Mount beacons approximately 2 to 2.5 meters above the floor. This minimizes signal absorption by human bodies.
- **Material**: Do not mount directly on metal surfaces or behind thick concrete pillars, as this will severely attenuate the BLE signal. Use plastic enclosures.
- **Adhesive**: Use heavy-duty double-sided foam tape (e.g., 3M VHB) to secure the beacons to drywall or drop-ceiling grids.

## 5. Battery Life Estimates

- **CR2032 Coin Cell**: ~8-10 months at 300ms interval / -4 dBm.
- **2x AA Batteries**: ~3-4 years at 300ms interval / -4 dBm.
*(Note: Always document the installation date to schedule proactive battery replacements.)*

## 6. Testing Beacon Range and Signal

1. Use an app like **nRF Connect** or **Beacon Scanner** on Android.
2. Stand directly under the beacon. Expected RSSI: `-50` to `-60` dBm.
3. Walk 10 meters away. Expected RSSI: `-75` to `-85` dBm.
4. Ensure there is minimal overlapping interference between adjacent beacons (adjust Tx power if necessary).

## 7. Troubleshooting

- **No Signal Detected**: Check battery. Ensure the beacon is not in deep sleep mode.
- **Fluctuating Signal (Jumping Location)**: The Tx power might be too high, causing the device to hear distant beacons as strongly as nearby ones. Lower Tx power or move beacons further apart.
- **Signal Drops in Crowds**: Normal BLE behavior. The app's PDR (Pedestrian Dead Reckoning) engine should bridge the gap between beacon signals.
