# NavGuide — IoT-Guided Indoor Navigation for Visually Impaired Students

An Android app that guides visually impaired students through a building using **BLE beacon
proximity + step counting**, with **spoken turn-by-turn instructions and distinct vibration
patterns**. Designed for a university department building (single floor, corridor-scale).

> **Status: student proof-of-concept.** The app builds, its core logic is unit-tested (43 tests),
> and it runs fully offline. It was rebuilt from scratch as a clean, minimal, dependency-light
> implementation. See `docs/ROADMAP.md` for what is intentionally not implemented yet.

---

## What works today

| Feature | Status |
|---|---|
| Navigate between locations on a floor map (spoken steps + haptics) | ✅ |
| BLE iBeacon scanning (real beacons) | ✅ |
| **Beacon Mode** — turn a demo phone INTO a beacon (no hardware needed) | ✅ |
| **In-navigation demo controls** — hop between locations / simulate steps on one phone | ✅ |
| Offline-first: bundled map, no internet, no cloud account | ✅ |
| Emergency SOS: loud alarm + SMS to a saved contact | ✅ |
| Map editor (single HTML file) to design your building | ✅ |
| Unit tests for routing, positioning, instruction generation | ✅ (43 tests) |

## Quick start (students — zero budget)

1. **Build the app** (see `docs/SETUP.md` for the full toolchain notes):
   ```
   .\gradlew.bat assembleDebug
   ```
   Install `app/build/outputs/apk/debug/app-debug.apk` on Android phones (min. Android 8 / 8.0, API 26).

2. **Demo with phones-as-beacons** (no hardware):
   - Phone A: open the app → **Beacon Mode** → pick a location (e.g., "Junction A") → Start broadcasting.
   - Phone B: open the app → pick a destination → walk toward Phone A; instructions speak as the
     beacons are detected. See `docs/DEMO_SCRIPT.md` for a full 3-scene demo script.
   - No second phone? Inside any navigation, use the **demo controls** at the bottom of the
     navigation screen (pick a location → "Hop here", or "Simulate 10 steps") to replay the walk.

3. **Optional — real beacons or ESP32 DIY beacons**: see `docs/HARDWARE_OPTIONAL.md`.

## Repository layout

```
app/                          Android app (Kotlin)
  src/main/java/com/navguide/
    core/                     map model + geometry (pure Kotlin, unit-tested)
    ble/                      iBeacon format, scanner service, emitter, permissions
    positioning/              step detector + beacon-proximity tracker
    routing/                  graph + A* route finder
    guidance/                 navigation session + instruction generation + TTS/haptics
    sos/                      emergency alarm + SMS
    ui/                       screens (main, navigation, settings, beacon mode, simulator)
  src/main/assets/demo_map.json     bundled demo floor (12 beacon locations)
  src/test/                   unit tests (JVM, no device needed)
tools/map-editor.html         offline map editor (export demo_map.json)
hardware/esp32-beacon/        optional DIY ESP32 beacon firmware + notes
docs/                         setup, demo script, map format, accessibility, roadmap
```

## How it works (short version)

1. **Position**: the phone scans iBeacon advertisements. When a known beacon (by its minor ID)
   is close and strong enough, the position snaps to that beacon's map node — with hysteresis so
   it doesn't flicker between beacons. Between beacons, step counting estimates progress.
2. **Route**: A* over the building graph (nodes = locations/beacons, edges = walkable paths in meters).
3. **Guidance**: instructions are counted in **steps, not meters** ("Turn left in 10 steps"), spoken
   via TTS, paired with distinct vibration patterns (left = two short pulses, right = one long, etc.).
4. **Privacy**: everything runs on-device. No location tracking, no cloud. SOS uses only the phone
   (alarm + SMS) — nothing leaves the device except that message.

## Cost

Software: **₹0**. Demo: **₹0** (phones-as-beacons or simulator). Optional real hardware: BLE iBeacon
tags from ~₹250–500 each in India (~₹3,000–6,000 for a 12-beacon building), or ESP32 DIY beacons
(~₹300–500 each). See `docs/HARDWARE_OPTIONAL.md`.

## Docs

- [docs/SETUP.md](docs/SETUP.md) — build, install, troubleshoot
- [docs/DEMO_SCRIPT.md](docs/DEMO_SCRIPT.md) — how to demo it (and what to say)
- [docs/MAP_FORMAT.md](docs/MAP_FORMAT.md) — map JSON schema + editor usage
- [docs/ACCESSIBILITY.md](docs/ACCESSIBILITY.md) — audio/haptic design + testing checklist
- [docs/HARDWARE_OPTIONAL.md](docs/HARDWARE_OPTIONAL.md) — real beacons / ESP32 beacons
- [docs/ROADMAP.md](docs/ROADMAP.md) — future features (cloud, schedule, obstacle detection…)

## License

MIT (academic project).
