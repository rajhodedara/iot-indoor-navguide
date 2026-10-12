# Accessibility Design Notes

NavGuide is built for eyes-free use. This document records the design choices and a manual
test checklist. It also lists what still needs **testing with real visually impaired users**.

## Feedback channels

| Channel | Used for | Why |
|---|---|---|
| **Spoken instructions** (TTS) | Every turn, route start, arrival, errors | Primary channel; works with one hand, no screen. |
| **Vibration patterns** | Same events as speech, confirmed tactilely | Works in noisy spaces and for deaf-blind users (patterns are distinct). |
| **Screen text** | Confirmation for sighted helpers / demo | Large, high-contrast (black background, yellow/white text). |

## Instruction phrasing

- **Steps, not meters.** Research and practitioner guidance for blind navigation agree that
  "turn left in 10 steps" is actionable while "in 7 meters" is not — users cannot judge meters.
- Instructions name the action first for urgency: *"Turn left."* then context when relevant.
- Straight-through junctions are silent (no chatter); only turns, start, arrival and problems speak.
- Countdowns fire ~10 steps before a turn; the final command fires at the turn beacon.
- People walk to a "beat" — instructing near landmarks (beacons) beats continuous nagging.

## TTS behavior (TalkBack coexistence)

- The app uses a **short-prompt** model: messages are < 8 words where possible, spoken with
  `QUEUE_FLUSH` so new guidance interrupts stale guidance (never a pile-up).
- If TalkBack is on, both engines can speak. Guidance prompts are brief and infrequent to
  minimize overlap; users are expected to run NavGuide as the primary navigation audio.
- TTS init delay: prompts issued before the engine is ready are queued (last 3) and played
  on init, so early instructions are not lost.
- Voice speed is adjustable (Slow 0.85 / Normal 1.0 / Fast 1.15) because Android 12+ engines
  vary in perceived speed.

## Vibration pattern table (initial tuning; refine in user testing)

| Event | Pattern | Rationale |
|---|---|---|
| Straight / continue | single 100 ms | minimal, frequent — must be unobtrusive |
| **Left** | two 120 ms pulses (100 ms gap) | "two ticks = two syllables (left)" |
| **Right** | single 400 ms pulse | longer, distinct from both above |
| Arrival | single 1000 ms | unmistakable "done" |
| Off-route | 200/100/200/100/600 ms | uneasy rhythm → calls attention |
| SOS triggered | rapid staccato 60 ms ×8 | urgent, matches alarm |

Patterns deliberately differ in **length and rhythm**, not intensity only, so they survive
glove/fabric and are distinguishable with a cane in the other hand.

## SOS design

- Two triggers: big red **SOS button** (long-press 1.5 s) and **triple-press volume-down**
  (fast, no screen interaction). 
- Action: loud alarm + SMS with current location name to the saved contact. No confirmation
  step (simplicity beats safety theater in an emergency), but the alarm is stoppable.
- Note: holding volume keys varies by OEM (some toggle accessibility shortcuts). The triple-press
  is short taps, chosen to avoid that collision. **Verify on your demo phones.**

## Screen-reader / layout rules in the app

- Every interactive element is a real Button/EditText with a `contentDescription`.
- Touch targets ≥ 64–120 dp (far above the 48 dp minimum).
- High-contrast dark theme hardcoded (not following system theme) for predictable visibility.
- Focus order follows reading order; no custom gestures required (simulator/beacon mode are for
  demo operators, not end users).

## Manual test checklist (before demo/final submission)

- [ ] TalkBack ON: swipe through all screens; every control announces something sensible.
- [ ] Navigation: instructions audible at normal walking pace; no overlapping speech.
- [ ] Haptics: can you tell left vs right vs arrival with eyes closed? (5 trials each)
- [ ] Arrival vibration fires exactly once.
- [ ] Repeat button re-speaks the last instruction.
- [ ] SOS: long-press triggers alarm + SMS; volume triple-press works on the demo phone.
- [ ] Phone in pocket: vibration still perceivable (test walking).
- [ ] Battery: 30 min navigation drain is acceptable (screen on = main cost).

## To validate with real users (future work)

- Step-length calibration per user (settings exist; needs UX study).
- Whether left/right patterns above are learned as intended (haptics literature suggests
  rhythm-based tactons work, but patterns should be co-designed with users).
- Prompt timing: 10-step countdowns may need adjusting per walking speed.
