# Mapping Your College (class-to-class navigation for your building)

This guide is for the scenario you described: **a student goes from one class to another**
inside your college building. The app already supports this — it is map-agnostic. You only
need to design your building once with the map editor.

> **KJSIT Sion specifically:** a starter template is included (`app/src/main/assets/kjsit_template.json`)
> modelled as a G+8 building with Floor 1 + Floor 2 and working staircase links — cross-floor
> class-to-class routing works out of the box. See the last section.

## Step 1 — Draw your building (15–30 min, no coding)

Open `tools/map-editor.html` in any browser (works offline, e.g. double-click the file).

1. Set **Pixels per meter** — a rough guess is fine (default 20). Everything you measure will
   be converted to meters with this. If you have a floor-plan image, load it as background and
   pick pxPerM so a known corridor (say 30 m) matches the image (e.g. 600 px / 30 m = 20).
2. Decide your **beacon locations** — the rule is simple:
   - one beacon at every **classroom door** you want as a destination,
   - one at every **corridor junction / turn**,
   - one at the **main entrance**, one at each **staircase/elevator**.
   For a typical college floor: 10–20 locations. Each one becomes a node.
3. **Add Node** → click the spot → name it ("Class 101", "Junction by Library"…) and set its
   type (`room`, `intersection`, `entrance`, `stairs`, `exit`).
4. **Connect** → click two nodes to make a walkable path; type the real walking distance in
   meters in the right panel (or keep the auto-distance). Only connect where a student can
   actually walk — don't cut through walls.
5. **Validate** — fix anything it reports (missing connections, duplicate beacon numbers).
6. **Export JSON** → save as `demo_map.json`.

## Step 2 — Put the map into the app

Replace `app/src/main/assets/demo_map.json` with your exported file, then rebuild:

```powershell
.\gradlew.bat assembleDebug
```

That's it — the app's destination list will now show your real classroom names, and routing
works between any two of them (class → class, class → exit, entrance → class, etc.).

> 💡 Beacon minors: the editor numbers them for you if you set them; make sure each numbered
> location is unique. When you eventually stick real beacons, each beacon gets its location's
> number (configure with the nRF Connect app).

## Step 3 — How the class-to-class flow will feel

1. Student opens the app → sees **"Where do you want to go?"** with all classrooms.
2. Picks "Class 102" (or any destination) → spoken: *"Navigation started. Walk straight for
   24 steps, then turn left."*
3. As they walk: countdown whispers near each turn (*"Turn left in 10 steps"*), a distinct
   vibration for left/right at the turn, "Keep walking straight" if they overshoot.
4. Arrival: *"You have arrived at Class 102."* + long vibration.

This exact flow (Room 101 → Room 103) is covered by automated tests:
`RouterTest.class to class route room 101 to room 103` and
`NavigationSessionTest.class to class walk from room 101 to room 103`.

## Step 4 — Try it before you map a whole floor

Fastest sanity check: keep the demo map, build the app, and run **Simulator controls** inside
navigation: start a route, then "Hop here" along the corridor nodes. You'll see the whole
class-to-class experience in 60 seconds without any hardware.

## Notes / limitations to know (honesty for your report)

- **Multi-floor is supported**: floors cross via stairs/elevator nodes with the same name
  (e.g. "Staircase A" on every floor). The app announces "Take the stairs to floor 3." and the
  router prefers routes with fewer floor changes.
- **Corridor accuracy** (~2–4 m). Great for "which corridor / which turn"; not for cm-level.
- A room's beacon sits **at its door** — the app guides you TO the door, not inside the room.
- Distances are for **walking paths**: measure corridor route lengths, not straight lines.
- You can have up to **65,535 beacon numbers** (iBeacon minor field), so the building size
  is not a limit at all — 12 or 120 locations both work.

## KJSIT Sion — starter template (adjust on site)

Since public interior floor plans of the KJSIT Sion building aren't published online, the repo
ships a **template** modelled on the institute's real structure:

- KJSIT occupies a **G+8 building** at the Somaiya Ayurvihar complex, Sion (confirmed from the
  institute site + Wikipedia). Ground + floors 3/5 style layouts vary — adjust on your site walk.
- `app/src/main/assets/kjsit_template.json` models **Floor 1 + Floor 2** with two staircases and
  cross-floor routing that already works (it is covered by the multi-floor tests).
- To use it: in `tools/map-editor.html`, **Import JSON** → pick the template → edit names/positions
  to match your real floor → export as `demo_map.json` → replace in `app/src/main/assets/` → rebuild.
- Copy the same staircase nodes pattern to add floors 3–9 (duplicate a floor, rename rooms,
  keep staircase names identical across floors).

> This is a starting grid, not a surveyed plan — a 20-minute site walk with the editor open on a
> laptop/tablet fixes it. Correct building facts used: Sion campus, Ayurvihar complex, G+8 storeys,
> departments (AI&DS, Computer, IT, Electronics & Telecom), library is a "Knowledge Resource Centre",
> ground-floor canteen + sports ground exist (from the institute's public pages).

## Realistic effort estimate for your college

| Task | Time |
|---|---|
| Walk the floor, note junctions/door positions on a sketch | 1 hour |
| Enter nodes + edges in the editor | 1–2 hours |
| Replace map, rebuild, test with demo controls | 15 min |
| Walk-test with 2 phones (one in Beacon Mode) | 1 hour |
| Optional: install real beacons (or ESP32s) and walk-test again | 2–3 hours |
