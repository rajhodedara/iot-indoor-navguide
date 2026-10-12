# Setup & Build Guide

## 1. Requirements (verified working set)

| Tool | Version used | Notes |
|---|---|---|
| JDK | Oracle JDK 24 (or any 17–24) | Gradle needs a JDK ≥ 17 |
| Gradle | 9.7.1 (wrapped in the repo) | Use `gradlew.bat`, no install needed |
| Android Gradle Plugin | 9.3.1 | Configured in the build files |
| Kotlin | built into AGP 9 (no separate plugin) | `compileOptions` target 17 |
| Android SDK | platform android-36 (+ build-tools 36.x) | install via Android Studio or sdkmanager |
| Android device | Android 8.0+ (API 26+) with BLE | BLE tests do NOT work on emulators |

Set the SDK path in `local.properties` (repo root, not committed):

```
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

## 2. Build

From the repository root (Windows PowerShell):

```powershell
.\gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Run the unit tests (no device needed):

```powershell
.\gradlew.bat testDebugUnitTest
```

## 3. Install on a phone

Via USB (enable Developer Options → USB debugging first):

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Or copy the APK to the phone and open it (allow "install from unknown sources").

## 4. First run

1. Grant the permissions the app asks for (Bluetooth + location + notifications).
   SMS is **optional**: Settings → "Enable silent SOS" — without it, SOS opens the Messages app
   with the emergency text ready to send.
2. **Settings** → set your emergency contact and check "Test voice" / "Test vibration".
3. Back on the main screen, pick a destination. Inside navigation, the bottom **demo controls**
   (screenshot-free simulator) let you test without any beacons: pick a location → "Hop here".

## 5. Troubleshooting

| Problem | Fix |
|---|---|
| `SDK location not found` | Create/fix `local.properties` (see step 1). |
| Gradle complains about Java version | Ensure `java -version` prints 17–24; set `JAVA_HOME` to the JDK if needed. |
| App installs but scanning finds nothing | Make sure Bluetooth is ON, permissions granted, and the OTHER phone is broadcasting (Beacon Mode). Some phones cannot broadcast BLE — try a different demo phone for Beacon Mode. |
| No voice output | Install/enable a TTS engine (Speech Services by Google is preinstalled on most phones). Settings → Text-to-speech → check engine. |
| "This phone cannot broadcast BLE beacons" | That phone lacks BLE peripheral support. Use another phone, or a real beacon / ESP32. |
| Voice speed sounds odd | Android 12 quirk on some engines: change the speed in NavGuide Settings (Slow/Normal/Fast) and test. |
| Navigation doesn't start moving | Keep walking; beacons are detected within a few meters. Or use the in-navigation demo controls ("Hop here") to check the logic. |

## 6. Project structure recap (where to change what)

- **Add/change locations** → edit `app/src/main/assets/demo_map.json` or use `tools/map-editor.html`.
- **Tune snapping** → `PositionTracker` constants (`snapDistanceM`, `snapRssiMin`, `hysteresisDb`).
- **Tune instructions** → `InstructionGenerator` (phrasing) and `NavigationSession` (timing).
- **Vibration patterns** → `HapticsImpl`.
- **SOS behavior** → `SosController`.
