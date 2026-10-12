# Map Format (schema v1)

The app reads a single JSON map from `app/src/main/assets/demo_map.json`. Design your own in
`tools/map-editor.html` (offline, single file) and export → replace the asset → rebuild.

## Schema

```json
{
  "schemaVersion": 1,
  "name": "Demo Floor - Department Building",
  "uuid": "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
  "floor": { "widthM": 40, "heightM": 25, "pxPerM": 20, "image": null },
  "nodes": [
    { "id": "entrance", "x": 2, "y": 12, "name": "Main Entrance", "type": "entrance", "beaconMinor": 1 }
  ],
  "edges": [
    { "from": "entrance", "to": "ja", "m": 8 }
  ]
}
```

### Fields

| Field | Meaning |
|---|---|
| `uuid` | One shared iBeacon UUID for the whole building. Keep it identical on every beacon. |
| `x`, `y` | Node position in **meters** (origin top-left, y grows downward). |
| `type` | One of `entrance`, `corridor`, `intersection`, `room`, `stairs`, `elevator`, `exit`. Destinations shown to users = room / entrance / stairs / exit. |
| `beaconMinor` | The iBeacon **minor ID** of the beacon at this node (number or omit). This is how a detected signal maps to a place. Must be unique. |
| `floor` | Floor number (1 = ground, 2, 3…). Optional, defaults to 1. Multi-floor maps route across floors via stairs/elevator nodes that share the same name. |
| `edges[].m` | Walkable distance in meters. Edges are bidirectional. |

### Rules (enforced by the app + editor validator)

1. Every node id unique; every edge references existing nodes.
2. `beaconMinor` values unique across the map (also unique per iBeacon major — we use major=1 always).
3. Graph must be **fully connected** (every location reachable; stairs/elevator links across floors count).
4. Distances should reflect the real walking path (beacon-to-beacon), not straight-line shortcuts
   through walls.
5. **Multi-floor**: give stair/elevator nodes the same name on each floor (e.g. "Staircase A" on
   floors 1–9); the app links them automatically for routing and announces
   "Take the stairs to floor N" when the route changes floors.

## Using the editor (tools/map-editor.html)

1. Open the file in any browser (works offline).
2. Optional: load a floor-plan image as the background; set **Pixels per meter** to match it.
3. **Add Node** → click where a beacon will be placed. Click a node with **Select** to edit:
   name, type, beacon minor. Set `pxPerM` before placing so exported meters are right.
4. **Connect** → click two nodes to create a walkable edge; edit the length (meters) in the
   right-hand list if needed.
5. **Validate** → fix any reported problems.
6. **Export JSON** → you get `demo_map.json`. Replace `app/src/main/assets/demo_map.json`
   and rebuild the app.

> Note: the editor exports `image: null` (background images are a visual aid only; the app never
> needs the image to navigate).

## Beacon configuration (real hardware)

For each physical beacon (configure once with e.g. nRF Connect):

| Setting | Value |
|---|---|
| UUID | same as `uuid` in the map |
| Major | `1` |
| Minor | the node's `beaconMinor` |
| Tx Power @ 1m | `-59` (default in app; adjust per beacon if ranging looks off) |
| Advertising interval | 300 ms (battery vs latency sweet spot) |
