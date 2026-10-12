# Roadmap — Not Implemented (yet)

This build is deliberately a focused core: offline beacon navigation + audio/haptic guidance.
The original project idea mentioned more features; they are future work, listed honestly here so
nobody mistakes them for implemented functionality.

## Near-term (fits a student team)

1. **Voice commands** — start navigation by speaking ("navigate to Room 102") via SpeechRecognizer,
   with a wake gesture. (Design carefully: blind users dislike false triggers while walking.)
2. **Step-length calibration flow** — "walk 10 steps, we'll learn your stride" wizard.
4. **Heading-assisted PDR** — gyro heading for mid-corridor progress estimates (this build uses
   step counting only; heading drift indoors is real and needs care).
5. **Haptic pattern user study** — validate/refine the left/right/arrival patterns with users.

## Medium-term

6. **Cloud map sync** (Firebase Firestore free tier) + web admin map editor with live updates.
   The v1 prototype had a sketch of this; it is NOT in the current app.
7. **Class-schedule integration** — auto-suggest "leave now for Physics in Room 103".
8. **SOS escalation** — staff dashboard + push notifications to campus security.
9. **Check-in / social features** (see original concept) — needs careful privacy design.

## Long-term / research

10. **Camera obstacle detection** — on-device TFLite model (wet-floor signs, chairs). Opt-in.
11. **Better positioning** — sensor fusion, outlier rejection, learned signal maps.
12. **iOS app** — feature parity (CoreLocation beacons).
13. **Battery-optimized background mode** — always-on guidance without battery drain.

## Known limitations of the current build

- Corridor-scale demo accuracy (~2–4 m); not room-level precision.
- Multi-floor routing works via named staircase/elevator nodes; the stairs themselves must be
  walked with user's own judgement (step counting pauses mid-staircase).
- Snapping tuned for walking pace; slow strollers may want higher `confirmationsNeeded`.
- Phone-as-beacon depends on a phone model with BLE peripheral support (many mid-range ones work).
- No automated UI tests (unit tests cover the algorithm core).
