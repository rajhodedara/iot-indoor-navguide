# Demo Script (3 scenes, zero budget)

Audience: evaluators / faculty. Goal: show a visually impaired user being guided **hands-free
and eyes-free** through a building — using only phones (no purchased hardware).

## Before the demo — prepare

- 3 Android phones (minimum 1, ideal 3):
  - **Phone A** and **Phone B** will act as beacons (Beacon Mode).
  - **Phone C** is the "user" phone running navigation.
- Install `app-debug.apk` on all phones. Grant permissions. Set an emergency contact on Phone C.
- Put phones A/B on a table/wall ~6–8 meters apart, like two corridor checkpoints.
- Optional: print the demo floor diagram (`tools/map-editor.html` export) to show the layout.

> ⚠️ Test everything once before the audience arrives. Confirm each phone shows
> "Broadcasting as …" and the user phone detects it (watch the "Near:" line).

## Scene 1 — "Beacon Mode gives us a building" (60s)

1. On Phone A: **Beacon Mode → Junction A → Start broadcasting.** Screen shows
   "Broadcasting as Junction A (minor 2)".
2. On Phone B: same, choose **Room 102 (minor 6)**.
3. Talking point: *"Each phone is now an iBeacon — the same signal cheap ₹300 beacons emit.
   In a real deployment these are coin-cell tags stuck to the ceiling; for this demo our phones
   are the beacons."*

## Scene 2 — "Guide me" (2–3 min, the main event)

1. On Phone C: pick a destination, e.g. **Staff Room** (or Room 102 for the shorter walk).
2. Stand near Phone A. The app speaks: *"Navigation started. Walk straight for N steps, then
   turn left/right."* — show the big on-screen instruction too.
3. Walk slowly toward Phone B. As you approach, the turn countdown fires
   (*"Turn right in 10 steps"*), then at the beacon: *"Turn right."* + the distinct vibration
   pattern (let an audience member hold Phone C to feel it).
4. Arrive: *"You have arrived at Room 102."* + long arrival vibration.
5. Press **Repeat last instruction** — it re-speaks. *"That's how a user asks again without
   looking at the screen."*

## Scene 3 — "And if technology fails?" (60s)

1. **Auto-walk fallback (one tap!)**: start ANY navigation, then tap **"Auto-walk route"** at the
   bottom of the navigation screen — the app walks the whole route by itself, giving real spoken
   guidance and vibration at every turn, from Main Entrance to your destination. Perfect for
   explaining the system when you can't walk around.
2. **Manual fallback**: pick a location in the dropdown → "Hop here" to jump to it;
   "Simulate 10 steps" to walk forward step by step.
   Talking point: *"Even with zero beacons, the navigation logic can be demonstrated and tested."*
2. **SOS**: long-press the big red SOS button (or triple-press volume-down). Alarm plays,
   SMS goes to the saved contact (receiver shows it). *"One gesture, no screen interaction."*

## Talking points for the Q&A

- **Why beacons?** GPS doesn't work indoors; beacons cost ~₹300 and last years on a coin cell.
- **Why steps, not meters?** Blind users can't gauge meters; step counts are actionable.
- **Why only two signal checks?** We use proximity snap + hysteresis (no fragile RSSI
  fingerprinting), so setup is minutes, not days — and it survives furniture changes.
- **Privacy:** everything runs on the phone. Nothing is uploaded. SOS = alarm + SMS only.
- **Cost table:** software ₹0; demo ₹0; realistic building ≈ ₹3–6k of beacons.
- **Honest limits:** single floor; no cloud features yet; needs a walk-test to tune step length.

## Failure fallbacks (if something misbehaves)

| Problem | Fallback line |
|---|---|
| A beacon phone won't broadcast (unsupported) | Swap in another phone; or say "this is why real beacons exist — and here's the simulator". |
| User phone doesn't detect the beacon | Move closer (within ~2–3 m), wait 2–3 s, keep the beacon phone's screen on. |
| No TTS voice | Show the big on-screen text + vibrations; check the phone's TTS engine afterwards. |
| Wi-Fi/BT interference in the hall | Use the inline demo controls (Scene 3) to finish the demo cleanly. |
