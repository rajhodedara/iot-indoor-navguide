# 🧭 NavGuide — Complete Project Context
## IoT-Guided Indoor Navigation System for Visually Impaired Students in Educational Institutions

> **This is the single-source-of-truth document for the entire project.**
> Everything — problem statement, architecture, algorithms, features, data models, code structure, design decisions, and review results — is captured here.

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Project Objectives](#2-project-objectives)
3. [Proposed Solution](#3-proposed-solution)
4. [System Architecture](#4-system-architecture)
5. [Hardware Layer — BLE Beacons](#5-hardware-layer--ble-beacons)
6. [Indoor Positioning Algorithm](#6-indoor-positioning-algorithm)
7. [Navigation — A* Pathfinding](#7-navigation--a-pathfinding)
8. [Accessibility-First Feedback Design](#8-accessibility-first-feedback-design)
9. [Feature Specifications (7 Features)](#9-feature-specifications)
10. [Cloud Architecture — Firebase](#10-cloud-architecture--firebase)
11. [Data Models (Firestore)](#11-data-models-firestore)
12. [API Design (Cloud Functions)](#12-api-design-cloud-functions)
13. [Admin Portal](#13-admin-portal)
14. [Security & Privacy](#14-security--privacy)
15. [Technology Stack](#15-technology-stack)
16. [Cost Analysis](#16-cost-analysis)
17. [Project Structure & Codebase](#17-project-structure--codebase)
18. [Android App — Module Guide](#18-android-app--module-guide)
19. [Design Decision Log](#19-design-decision-log)
20. [Multi-Agent Design Review Results](#20-multi-agent-design-review-results)
21. [Deployment Strategy](#21-deployment-strategy)
22. [Future Scope](#22-future-scope)
23. [References](#23-references)

---

## 1. Problem Statement

### The Global Challenge
- **285 million** people worldwide are visually impaired (WHO)
- **36 million** are completely blind
- **90%** of visually impaired people live in developing countries
- Educational inclusion is a fundamental right, yet physical infrastructure remains a major barrier

### The Specific Problem
Visually impaired students in educational institutions face critical daily challenges:

| Challenge | Impact |
|---|---|
| **Finding classrooms** | Late to classes, anxiety, dependence on others |
| **Navigating hallways** | Disorientation in unfamiliar or crowded areas |
| **Avoiding obstacles** | Physical safety risk (furniture, wet floors, people) |
| **Emergency evacuation** | Life-threatening inability to find exits |
| **Social isolation** | Cannot locate friends or staff independently |
| **Schedule changes** | Room changes require re-learning routes |

### Why Existing Solutions Fail

| Solution | Limitation |
|---|---|
| **GPS navigation** | Does NOT work indoors — signals blocked by walls/ceilings |
| **Human escorts** | Not scalable, creates dependence, affects dignity |
| **Tactile floor markings** | Static, expensive to install, limited to fixed routes |
| **Guide dogs** | Expensive, not available to all, cannot navigate to specific rooms |
| **Braille signage** | Only useful when you can find and reach the sign |

### Gap Identified
**There is no affordable, scalable, real-time indoor navigation solution designed specifically for visually impaired students in educational institutions.**

---

## 2. Project Objectives

### Primary Objectives
1. Design and implement an **IoT-based indoor navigation system** using BLE beacons for a single educational building
2. Provide **real-time turn-by-turn guidance** using audio (Text-to-Speech) and haptic (vibration) feedback
3. Implement **obstacle detection** using the smartphone camera and on-device AI
4. Create an **Emergency SOS system** with local fallback for network dead zones
5. Integrate **classroom schedules** for automated navigation prompts
6. Build an **admin web portal** for building map management

### Secondary Objectives
7. Implement **voice-controlled commands** for hands-free operation
8. Create **simplified social features** (manual check-in, not tracking)
9. Ensure **privacy-first, local-first architecture** — no continuous location uploads
10. Maintain **zero operational cost** using Firebase free tier

### Project Scope & Constraints

| Aspect | Scope |
|---|---|
| **Scale** | Single department building (~20 beacons) |
| **Platform** | Android (Kotlin) |
| **Cloud** | Firebase Spark Plan (free tier) |
| **Users** | 10-20 students for prototype testing |
| **Type** | Academic proof-of-concept prototype |
| **Budget** | ~$200-300 (hardware only) |

---

## 3. Proposed Solution

### NavGuide — Overview
NavGuide is an Android smartphone application paired with BLE (Bluetooth Low Energy) beacons deployed throughout a building. The system:

1. **Detects** the user's indoor position using BLE beacon proximity + smartphone sensors
2. **Calculates** the optimal route using A* pathfinding on a pre-defined building graph
3. **Guides** the user with step-count-based audio instructions and distinct haptic vibration patterns
4. **Detects** obstacles using the smartphone camera with TensorFlow Lite (opt-in mode)
5. **Alerts** staff in emergencies via Firebase Cloud Messaging, with a local alarm fallback

### Core Design Principles

| Principle | Implementation |
|---|---|
| **Local-First** | Navigation works fully offline after initial map download |
| **Privacy-First** | No real-time location stored in cloud; location uploaded only during SOS |
| **Accessibility-First** | Full TalkBack support, distinct haptic patterns, Audio Focus management |
| **Safety-First** | Local alarm SOS fallback, battery kill detection, orientation validation |
| **Cost-First** | Free Firebase tier, affordable BLE beacons, no extra hardware for users |

---

## 4. System Architecture

### Three-Tier Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                    HARDWARE TIER (IoT)                            │
│                                                                  │
│   [BLE Beacon 1]  [BLE Beacon 2]  [BLE Beacon 3]  ...  [N]     │
│       ↓               ↓               ↓                         │
│   Broadcasting UUID/Major/Minor identifiers via BLE              │
│                                                                  │
│   [Smartphone Sensors]                                           │
│   Gyroscope · Accelerometer · Camera                             │
└──────────────────────┬───────────────────────────────────────────┘
                       │ BLE Advertising Packets
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│                    CLIENT TIER (Android App)                      │
│                                                                  │
│   ┌─────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│   │ BLE Scanner  │→│ Positioning      │→│ Routing Engine    │   │
│   │ (Foreground  │  │ Engine           │  │ (A* Pathfinding)  │   │
│   │  Service)    │  │ (Proximity+PDR)  │  │ (Local Graph)     │   │
│   └─────────────┘  └──────────────────┘  └────────┬─────────┘   │
│                                                    │             │
│   ┌─────────────┐  ┌──────────────────┐  ┌────────▼─────────┐   │
│   │ Obstacle     │  │ Voice Command    │  │ Feedback Manager │   │
│   │ Detector     │  │ Manager          │  │ (Haptic + Audio) │   │
│   │ (Opt-in,     │  │ (Volume Button)  │  │ (TTS + Earcons)  │   │
│   │  1 FPS)      │  │                  │  │                  │   │
│   └─────────────┘  └──────────────────┘  └──────────────────┘   │
│                                                                  │
│   ┌─────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│   │ SOS Manager  │  │ Schedule Manager │  │ Map Repository   │   │
│   │ (Local+Cloud)│  │ (Auto-Navigate)  │  │ (Cached + Sync)  │   │
│   └─────────────┘  └──────────────────┘  └──────────────────┘   │
└──────────────────────┬───────────────────────────────────────────┘
                       │ SOS Only / Map Sync / Auth
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│                    CLOUD TIER (Firebase)                          │
│                                                                  │
│   ┌──────────┐ ┌─────────────┐ ┌──────────┐ ┌──────────────┐   │
│   │ Firebase  │ │ Cloud       │ │ Cloud    │ │ Firebase     │   │
│   │ Auth      │ │ Firestore   │ │ Functions│ │ Cloud        │   │
│   │           │ │ (Maps, SOS, │ │ (SOS     │ │ Messaging    │   │
│   │           │ │  Schedules) │ │  Routing)│ │ (FCM Alerts) │   │
│   └──────────┘ └─────────────┘ └──────────┘ └──────────────┘   │
│                                                                  │
│   ┌──────────────────────────────────────────────────────────┐   │
│   │            Firebase Hosting (Admin Portal)                │   │
│   └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

### Data Flow Summary

```
OFFLINE FLOW (Normal Navigation):
  Beacons → BLE Scanner → Positioning Engine → Route Engine → Feedback → User
  (All on-device, no internet needed)

ONLINE FLOW (SOS Only):
  Panic Button → SOS Manager → Firestore → Cloud Function → FCM → Staff Phone
  Fallback: → Local Alarm + SMS (if no internet)

SYNC FLOW (Occasional):
  App Launch → Check Map Version → Download if Changed → Cache Locally
```

---

## 5. Hardware Layer — BLE Beacons

### What Are BLE Beacons?
Small, battery-powered devices that continuously broadcast a Bluetooth Low Energy signal. They don't connect to phones — they just announce their identity. The phone listens for these announcements to determine which beacons are nearby.

### Beacon Specifications

| Property | Value | Purpose |
|---|---|---|
| **Protocol** | iBeacon or Eddystone | Industry standard, universally supported |
| **UUID** | Single per institution | e.g., `f7826da6-4fa2-4e98-8024-bc5b71e0893e` |
| **Major ID** | Building + Floor | e.g., `0x0101` = Building 1, Floor 1 |
| **Minor ID** | Specific location | e.g., `0x0042` = Room 102 entrance |
| **Tx Power** | ~10 meter range | Balanced cell size |
| **Advertising Interval** | 300ms | Responsive yet battery-efficient |
| **Battery** | CR2032 coin cell | 1-2 year lifespan |
| **Quantity** | ~20 per building | At key decision points |
| **Unit Cost** | $10-15 | nRF51822 or nRF52832 based |

### Placement Strategy

```
SAMPLE FLOOR PLAN — Department Building, Floor 1

    [B1]────────[B2]────────[B3]────────[B4]
     │           │           │           │
   Room 101   Room 102   Room 103   Room 104
     │           │           │           │
    [B5]────────[B6]────────[B7]────────[B8]
                 │                       │
              Stairs [B9]          Elevator [B10]
                 │                       │
    [B11]───────[B12]──────[B13]───────[B14]
     │           │           │           │
   Lab 201    Lab 202    Library    Cafeteria
                                    [B15]

  [Bx] = BLE Beacon location (placed at ceiling or wall, 2.5m height)
  ──── = Walkable path (edge in navigation graph)
```

**Placement Rules:**
- Every intersection must have a beacon
- Every staircase entrance/exit must have a beacon
- Every elevator must have a beacon
- Every room entrance must have a beacon
- Maximum 8 meters between any two adjacent beacons
- Mount at 2.5m height on walls or ceiling (away from metal objects)

### Smartphone Sensors Used (No Extra Hardware)

| Sensor | Purpose | Why |
|---|---|---|
| **Bluetooth** | Detect BLE beacon signals | Primary positioning input |
| **Accelerometer** | Count steps for PDR | Distance estimation between beacons |
| **Gyroscope** | Track heading changes | Turn detection (replaces compass) |
| **Camera** | Obstacle detection (opt-in) | TFLite object recognition |

> **Why NOT magnetometer (compass)?** Indoor buildings have structural steel, HVAC ducts, and electrical wiring that create severe magnetic distortion. The compass can be off by 90°+ indoors, which would steer a blind user into walls. The gyroscope measures *relative* heading changes and is immune to magnetic interference.

---

## 6. Indoor Positioning Algorithm

### Approach: Proximity + Pedestrian Dead Reckoning (PDR)

> **Why NOT RSSI Fingerprinting?**
> Fingerprinting requires recording the exact signal strength (RSSI) from every beacon at hundreds of points across the building to build a "radio map." This map degrades rapidly when:
> - Furniture is moved
> - Crowds fill hallways (human bodies absorb 2.4GHz signals)
> - New Wi-Fi access points are installed
> - Seasons change (humidity affects signal propagation)
> 
> Re-surveying is labor-intensive and impractical for a student project. Proximity-based routing is simpler, more robust, and perfectly adequate for structured hallways.

### How It Works

```
STEP 1: BEACON DETECTION
  Phone scans for BLE beacons
  Identifies the NEAREST beacon by signal strength (RSSI)
  Snaps user position to that beacon's known graph node
  
STEP 2: PEDESTRIAN DEAD RECKONING (Between Beacons)
  When no new beacon is detected:
    - Accelerometer counts steps (peak detection algorithm)
    - Gyroscope tracks heading changes (relative, not absolute)
    - Estimated position = last beacon + (step_count × stride_length × heading)
  
STEP 3: CORRECTION
  When a new beacon IS detected:
    - PDR position is corrected/snapped to the beacon's known location
    - Accumulated drift is reset
    - Next route segment is confirmed or recalculated

STEP 4: CONTINUOUS LOOP
  Repeat Steps 1-3 as user walks
```

### Step Detection Algorithm

```
Raw Accelerometer Data → Low-Pass Filter → Peak Detection → Step Count

Peak Detection Parameters:
  - Minimum peak amplitude: 1.2g (filters out noise)
  - Minimum peak interval: 300ms (max walking speed ~3.3 steps/sec)
  - Stride length: Calibrated per user (default 0.65m)
```

### Positioning Accuracy
- **At beacon locations**: ~1-2 meters (beacon proximity)
- **Between beacons**: ~2-4 meters (PDR drift)
- **Acceptable for**: Turn-by-turn navigation in structured hallways (nodes are at intersections)

---

## 7. Navigation — A* Pathfinding

### Building Graph Model

The building is represented as a **directed weighted graph**:

```
Graph G = (V, E) where:
  V = Set of nodes (beacon locations + waypoints)
  E = Set of edges (walkable paths between nodes)

Each Node has:
  - id: Minor ID of the beacon
  - x, y: Physical coordinates on floor plan
  - floor: Floor number
  - type: "intersection" | "room" | "stairs" | "elevator"
  - description: "Main hallway, near Room 102"

Each Edge has:
  - node_a → node_b: Direction
  - distance: Weight in meters
  - accessible: Boolean (wheelchair/mobility accessible)
  - instructions: Human-readable guidance text
```

### A* Algorithm

```
A*(start, goal):
  openSet = {start}
  gScore[start] = 0
  fScore[start] = heuristic(start, goal)  // Euclidean distance
  
  while openSet is not empty:
    current = node in openSet with lowest fScore
    if current == goal: return reconstruct_path()
    
    openSet.remove(current)
    for each neighbor of current:
      tentative_g = gScore[current] + edge_weight(current, neighbor)
      if tentative_g < gScore[neighbor]:
        cameFrom[neighbor] = current
        gScore[neighbor] = tentative_g
        fScore[neighbor] = tentative_g + heuristic(neighbor, goal)
        openSet.add(neighbor)
  
  return failure  // No path exists
```

### Turn-by-Turn Instruction Generation

```
For each consecutive pair of edges in the path:
  1. Calculate angle between incoming and outgoing edge vectors
  2. Determine turn direction:
     - -45° to +45°  → "Go straight"
     - +45° to +135° → "Turn right"
     - -45° to -135° → "Turn left"
     - else          → "Turn around"
  3. Calculate distance to next turn in STEPS (not meters)
  4. Generate instruction: "In {step_count} steps, {turn_direction}"
```

---

## 8. Accessibility-First Feedback Design

### Haptic Vibration Patterns

| Event | Pattern | Feel | Rationale |
|---|---|---|---|
| **Turn Left** | `·` | Single short pulse (100ms) | Quick, directional tap |
| **Turn Right** | `——` | Single long pulse (400ms) | Distinct from left |
| **Go Straight** | `· ·` | Two short pulses | Confirms direction |
| **Approaching Turn** | Increasing intensity | Gradual ramp-up | Prepares user |
| **✅ Arrival** | `———` | One smooth, long pulse (800ms) | Calm, reassuring |
| **⚠️ Danger/Obstacle** | `···///···` | Rapid staccato bursts | Urgent, alarming |
| **⛔ Off Route** | `—·—·—` | Alternating long-short | Distinct correction |

> **CRITICAL**: Arrival and Danger patterns are DELIBERATELY different. The original design used the same continuous vibration for both — a blind user couldn't tell if they should relax (arrived!) or freeze (danger!). This was identified as a critical safety flaw in the design review.

### Audio Design

**Audio Priority System (Audio Focus Management):**
```
Priority 1 (HIGHEST): Navigation TTS instructions
Priority 2: Obstacle danger alerts (earcon sounds, NOT speech)
Priority 3: SOS confirmation
Priority 4: Schedule reminders
Priority 5 (LOWEST): Social check-in notifications

Rules:
- Higher priority DUCKS lower priority audio
- Obstacle alerts use SHORT earcon sounds to avoid speech collision
- TalkBack is temporarily suppressed during navigation instructions
- Non-critical notifications QUEUED and delivered during pauses
```

**Distance Communication:**
- ❌ ~~"In 5 meters, turn left"~~ (blind users can't gauge meters)
- ✅ **"In about 6 steps, turn left"** (uses real-time step counter)
- ✅ Haptic intensity increases as user approaches the turn point

### Voice Command Interface

| Trigger | Action |
|---|---|
| **Volume button long-press (3s)** | Activate voice listening |
| *Confirmation beep* | Ready for command |
| "Take me to the cafeteria" | Start navigation |
| "Where is Room 102?" | Route information |
| "What's my next class?" | Schedule query |
| "Emergency help" | Trigger SOS |
| "Turn on obstacle detection" | Toggle camera mode |
| "Stop navigation" | Cancel current route |

> **Why NOT shake-to-activate?** Blind navigation involves stumbling, bumping, and rapid cane movements. A shake gesture would trigger constantly during normal walking, interrupting navigation audio at the worst possible moments.

---

## 9. Feature Specifications

### Feature 1: Indoor Turn-by-Turn Navigation
**Core feature. Always available.**
- User selects destination (voice command or TalkBack menu)
- A* calculates shortest accessible path on local graph
- TTS provides step-count-based instructions
- Haptic feedback confirms direction and proximity to turns
- Gyroscope monitors heading; alerts and reroutes if user drifts
- Works entirely offline (graph cached locally)

### Feature 2: Obstacle Detection (Opt-In)
**Safety feature. User-activated.**
- Activated via voice command or toggle button
- Smartphone camera runs TFLite MobileNet SSD at **1 FPS** (power-efficient)
- Detects: chairs, trash bins, people, wet floor signs, doors
- Accelerometer validates phone orientation — alerts user if camera is blocked or angled wrong
- Alerts via short earcon sounds (not TTS) to avoid audio collision
- User can deactivate anytime

### Feature 3: Emergency SOS
**Life-safety feature. Always available.**
- **Trigger**: Volume Up + Volume Down pressed simultaneously for 3 seconds
- **Primary path** (with network):
  1. Write SOS event to Firestore (user ID, last beacon, floor, timestamp)
  2. Cloud Function triggers FCM push to all staff-role users
  3. Staff sees: student name, estimated location, floor number
  4. Staff resolves SOS via admin portal or staff app
- **Fallback path** (without network):
  1. Play maximum volume alarm siren from device speaker
  2. Attempt direct SMS to pre-configured emergency contacts
  3. Display high-contrast "SOS ACTIVE" screen
- SOS events logged for institutional safety records

### Feature 4: Classroom Schedule Integration
**Convenience feature. Automatic.**
- Student's schedule stored in Firestore (entered by admin or CSV import)
- App checks schedule periodically
- 10 minutes before class: TTS announcement
  - *"Your next class, Mathematics, in Room 102, starts in 10 minutes. Say 'navigate' to go there."*
- On confirmation: auto-starts navigation to classroom
- Eliminates need to memorize room locations or check schedules

### Feature 5: Voice-Controlled Commands
**Interaction feature. On-demand.**
- Activated via volume button long-press (3 seconds)
- Uses Android SpeechRecognizer (works offline for common commands)
- Natural language command parsing
- Supports: navigate, query schedule, trigger SOS, toggle obstacle detection, check status

### Feature 6: Admin Map Editor (Web Portal)
**Administrative feature. Staff/admin only.**
- React web application hosted on Firebase
- Interactive canvas-based floor plan editor
- Admin can:
  - Upload floor plan image as background
  - Click to place beacon nodes
  - Draw walkable edges between nodes
  - Set node properties (name, type, floor, accessibility)
  - Set edge properties (distance, direction, instructions)
  - Manage student schedules
  - Monitor SOS events in real-time
  - Publish map updates (versioned with hash)

### Feature 7: Social Check-ins (Simplified)
**Social feature. Opt-in, privacy-respecting.**
- ❌ ~~Continuous background location broadcasting~~ (removed — YAGNI, battery drain, privacy risk)
- ✅ **Manual "I'm here" check-in**: Student taps/speaks to share current beacon zone
- ✅ **"Is anyone nearby?"**: Queries recent check-ins in same zone
- All check-in data auto-expires after 30 minutes (Firestore TTL)
- Completely optional

---

## 10. Cloud Architecture — Firebase

### Firebase Spark Plan (Free Tier)

| Service | Purpose | Free Limit | Our Usage |
|---|---|---|---|
| **Authentication** | User login | 10K phone auths/month | ~20 users ✅ |
| **Cloud Firestore** | Data storage | 50K reads, 20K writes/day | <100/day ✅ |
| **Cloud Functions** | SOS routing | 125K invocations/month | <100/month ✅ |
| **Cloud Messaging** | Push notifications | Unlimited | ✅ |
| **Hosting** | Admin portal | 10GB bandwidth/month | <1GB ✅ |

### Why We Stay Within Free Limits

The key architectural decision: **NO continuous location uploads.**

```
WITHOUT our design (WOULD FAIL):
  10 users × updating location every 5 seconds × 3 hours
  = 10 × 720 × 3 = 21,600 writes/day
  → EXCEEDS 20K limit → App breaks for everyone

WITH our design (LOCAL-FIRST):
  Map sync: ~10 reads per app launch
  Schedule fetch: ~10 reads per day
  SOS: ~1 write (emergency only)
  → Total: <100 operations/day → WELL within limits ✅
```

---

## 11. Data Models (Firestore)

### Collection: `users`
```json
{
  "uid": "firebase-auth-uid",
  "role": "student",           // "student" | "staff" | "admin"
  "name": "Sarah Ahmed",
  "email": "sarah@university.edu",
  "phone": "+91XXXXXXXXXX",
  "emergency_contacts": ["+91YYYYYYYYYY", "+91ZZZZZZZZZZ"],
  "preferences": {
    "haptic_intensity": "medium",  // "low" | "medium" | "high"
    "tts_speed": 1.2,              // 0.5 to 2.0
    "tts_language": "en-IN",
    "stride_length": 0.65          // meters, calibrated
  },
  "schedule_ref": "schedules/schedule-001",
  "created_at": "2026-08-15T10:00:00Z"
}
```

### Collection: `buildings/{building_id}/beacons`
```json
{
  "minor_id": 42,
  "major_id": 257,          // 0x0101 = Building 1, Floor 1
  "floor": 1,
  "x": 12.5,                // meters from origin
  "y": 8.3,
  "type": "room",           // "intersection" | "room" | "stairs" | "elevator"
  "description": "Room 102 entrance - Mathematics classroom",
  "accessible": true
}
```

### Collection: `buildings/{building_id}/edges`
```json
{
  "node_a": 41,              // minor_id of start node
  "node_b": 42,              // minor_id of end node
  "distance": 6.2,           // meters
  "direction": 90,           // degrees (0=North, 90=East)
  "accessible": true,
  "instructions": "Go straight through the double doors"
}
```

### Collection: `schedules`
```json
{
  "student_uid": "firebase-auth-uid",
  "course_name": "Mathematics",
  "room_node": 42,           // beacon minor_id
  "start_time": "09:00",
  "end_time": "10:00",
  "days": ["Mon", "Wed", "Fri"]
}
```

### Collection: `sos_events`
```json
{
  "user_uid": "firebase-auth-uid",
  "user_name": "Sarah Ahmed",
  "last_known_node": 42,
  "floor": 1,
  "timestamp": "2026-08-15T09:45:00Z",
  "status": "active",         // "active" | "resolved"
  "responder_uid": null,
  "resolved_at": null
}
```

### Collection: `checkins` (TTL: 30 minutes)
```json
{
  "user_uid": "firebase-auth-uid",
  "user_name": "John",
  "beacon_zone": 42,          // current beacon minor_id
  "timestamp": "2026-08-15T09:30:00Z"
}
```

---

## 12. API Design (Cloud Functions)

### `triggerSos` — HTTPS Callable
```
Request:  { uid: string, lastNode: number, floor: number }
Response: { eventId: string, status: "active" }

Flow:
  1. Create document in sos_events collection
  2. Query all users with role == "staff"
  3. Send FCM push notification to each staff device:
     Title: "🚨 SOS Alert"
     Body: "Sarah Ahmed needs help near Room 102, Floor 1"
  4. Return event ID for tracking
```

### `resolveSos` — HTTPS Callable
```
Request:  { eventId: string, responderUid: string }
Response: { status: "resolved" }

Flow:
  1. Verify caller has staff role
  2. Update sos_events document: status = "resolved", responder_uid, resolved_at
  3. Send FCM to original user: "Help is on the way"
```

### `syncMap` — HTTPS Callable
```
Request:  { buildingId: string, clientVersion: string }
Response: { data: {...}, version: string } OR { upToDate: true }

Flow:
  1. Compute MD5 hash of current beacons + edges collections
  2. Compare with clientVersion
  3. If different: return full map data (beacons + edges + metadata)
  4. If same: return upToDate flag (no data transfer)
```

### `cleanupCheckins` — Scheduled (every hour)
```
Flow:
  1. Query checkins where timestamp < (now - 30 minutes)
  2. Batch delete all expired documents
  3. Log cleanup count
```

---

## 13. Admin Portal

### Technology
- **Framework**: React 18 with functional components and hooks
- **Build Tool**: Vite
- **Styling**: Custom CSS with dark mode
- **Backend**: Firebase SDK (Firestore, Auth)
- **Hosting**: Firebase Hosting

### Pages/Components

| Component | Purpose |
|---|---|
| `Login.jsx` | Admin authentication (email/password) |
| `Dashboard.jsx` | Overview: beacon count, active SOS, schedule stats |
| `MapEditor.jsx` | Canvas-based floor plan editor: place beacons, draw edges |
| `BeaconManager.jsx` | CRUD management for all beacons |
| `ScheduleManager.jsx` | Manage student class schedules |
| `SOSMonitor.jsx` | Real-time SOS event viewer with resolve actions |

---

## 14. Security & Privacy

| Concern | Mitigation |
|---|---|
| **Authentication** | Firebase Auth: phone OTP for students, email/password for staff |
| **Authorization** | Firestore Security Rules: role-based access (student/staff/admin) |
| **Location Privacy** | ALL positioning computed locally on device. No real-time location in cloud. |
| **SOS Privacy** | Location uploaded ONLY during emergency. Visible only to staff. |
| **Social Privacy** | Check-ins are opt-in, manual, auto-expire in 30 min |
| **Data Minimization** | No location history. No movement traces. No behavioral tracking. |
| **GDPR/Privacy Compliance** | Explicit consent for location sharing. Right to delete account. |

### Firestore Security Rules Summary
```
students → can read: maps, own profile, own schedule
         → can write: own check-ins, SOS events
         → CANNOT read: other students' data, staff data

staff    → can read: maps, SOS events, own profile
         → can write: SOS resolution
         → CANNOT read: student profiles (except during SOS)

admin    → full read/write on all collections
```

---

## 15. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| **Mobile App** | Kotlin | 1.9+ |
| | Android SDK | API 26+ (Android 8.0+) |
| | Android BLE API | Standard |
| | TensorFlow Lite | 2.x |
| | MobileNet SSD v2 | TFLite format |
| | Android SpeechRecognizer | Standard |
| | TalkBack | Standard |
| **Positioning** | Proximity (nearest beacon) | Custom |
| | Pedestrian Dead Reckoning | Custom (step counter + gyro) |
| | A* Pathfinding | Custom |
| **Cloud** | Firebase Authentication | Latest |
| | Cloud Firestore | Latest |
| | Cloud Functions | Node.js 18+ |
| | Firebase Cloud Messaging | Latest |
| | Firebase Hosting | Latest |
| **Admin Portal** | React | 18+ |
| | Vite | 5+ |
| | Firebase SDK | Latest |
| | HTML5 Canvas | Standard |
| **Hardware** | BLE Beacons | iBeacon / Eddystone |
| | Chipset | nRF51822 or nRF52832 |
| | Battery | CR2032 coin cell |

---

## 16. Cost Analysis

### Hardware Costs

| Item | Qty | Unit Cost | Total |
|---|---|---|---|
| BLE Beacons (generic nRF51822) | 20 | $10-15 | $200-300 |
| Beacon mounting supplies | 1 set | $10 | $10 |
| CR2032 batteries (spare) | 20 | $0.50 | $10 |
| **Hardware Subtotal** | | | **$220-320** |

### Software & Cloud Costs

| Item | Cost | Notes |
|---|---|---|
| Firebase Spark Plan | $0 | Free tier — sufficient for PoC |
| Android Studio | $0 | Free IDE |
| TensorFlow Lite | $0 | Open source |
| React + Vite | $0 | Open source |
| Google Fonts | $0 | Free |
| **Software Subtotal** | **$0** | |

### Total Project Cost: **~$220-320**

> **Budget Tip**: ESP32-based DIY beacons cost ~$3-5 each, reducing hardware cost to ~$60-100.

---

## 17. Project Structure & Codebase

```
IOT Project/
├── README.md                            # Project overview
├── PROJECT_CONTEXT.md                   # This file (comprehensive context)
├── .gitignore                           # Version control ignores
│
├── android/NavGuide/                    # 🤖 Android App
│   ├── build.gradle.kts                 #    Project Gradle
│   ├── settings.gradle.kts              #    Module settings
│   ├── gradle.properties                #    Build properties
│   └── app/
│       ├── build.gradle.kts             #    App dependencies
│       └── src/main/
│           ├── AndroidManifest.xml      #    Permissions & services
│           ├── java/com/navguide/
│           │   ├── ble/                 #    BLE scanning & beacon parsing
│           │   ├── positioning/         #    Proximity + PDR engine
│           │   ├── navigation/          #    Graph + A* + route manager
│           │   ├── feedback/            #    Haptic + Audio + Voice
│           │   ├── features/            #    SOS, Schedule, Obstacle, Check-in
│           │   ├── data/                #    Map & User repositories
│           │   └── ui/                  #    Activities (accessible)
│           └── res/                     #    Layouts, strings, colors, themes
│
├── firebase/                            # 🔥 Firebase Backend
│   ├── firebase.json                    #    Project config
│   ├── firestore.rules                  #    Security rules
│   ├── firestore.indexes.json           #    Database indexes
│   └── functions/src/                   #    Cloud Functions (TypeScript)
│       ├── triggerSos.ts                #    SOS event + FCM
│       ├── resolveSos.ts                #    Staff resolution
│       ├── syncMap.ts                   #    Versioned map sync
│       └── cleanupCheckins.ts           #    TTL cleanup
│
├── admin-portal/                        # 🖥️ Admin Web Portal
│   ├── src/
│   │   ├── components/                  #    React components
│   │   │   ├── Dashboard.jsx            #    Stats overview
│   │   │   ├── MapEditor.jsx            #    Canvas floor plan editor
│   │   │   ├── BeaconManager.jsx        #    Beacon CRUD
│   │   │   ├── ScheduleManager.jsx      #    Schedule management
│   │   │   └── SOSMonitor.jsx           #    Real-time SOS viewer
│   │   └── styles/index.css             #    Dark mode UI
│   └── package.json
│
├── docs/                                # 📚 Documentation
│   ├── HARDWARE_SETUP.md
│   ├── DEPLOYMENT_GUIDE.md
│   ├── USER_GUIDE.md
│   └── API_REFERENCE.md
│
└── presentation/                        # 🎯 Presentation
    └── NavGuide_Presentation.html       #    20-slide HTML presentation
```

---

## 18. Android App — Module Guide

### Module: `ble/` — Bluetooth Scanning

| File | Purpose | Key Classes |
|---|---|---|
| `BleScanner.kt` | Foreground service for continuous BLE scanning. Parses iBeacon packets. Requests battery optimization exemption. Alerts user via TTS if service is killed by OS. | `BleScanner : Service` |
| `BeaconData.kt` | Data class for parsed beacon information (UUID, Major, Minor, RSSI, timestamp) | `BeaconData` |

### Module: `positioning/` — Indoor Positioning

| File | Purpose |
|---|---|
| `PositioningEngine.kt` | Combines beacon proximity (nearest beacon snap) with PDR (step + heading) for continuous position estimation. Corrects PDR drift on each beacon detection. |
| `StepCounter.kt` | Accelerometer-based step detection using peak detection algorithm. Configurable stride length. |
| `GyroHeading.kt` | Relative heading tracking from gyroscope sensor. Drift-compensated. Does NOT use magnetometer. |

### Module: `navigation/` — Pathfinding & Routing

| File | Purpose |
|---|---|
| `NavigationGraph.kt` | Graph data structure (nodes + edges). Implements A* pathfinding. Supports local caching and version-based sync. |
| `RouteManager.kt` | Generates turn-by-turn instructions using step counts. Detects drift from planned route. Triggers re-routing. |

### Module: `feedback/` — User Feedback

| File | Purpose |
|---|---|
| `HapticFeedback.kt` | Custom vibration patterns — distinct for each event type (left/right/straight/arrival/danger/off-route). |
| `AudioManager.kt` | TTS engine with Audio Focus management. Navigation TTS has highest priority. Queues non-critical alerts. Earcon sounds for obstacles. |
| `VoiceCommandManager.kt` | Volume button long-press (3s) activation. SpeechRecognizer integration. Command parsing. |

### Module: `features/` — Application Features

| File | Purpose |
|---|---|
| `SosManager.kt` | Emergency SOS with dual path: Firestore+FCM (primary) and local alarm+SMS (fallback). |
| `ScheduleManager.kt` | Fetches schedule from Firebase. 10-min pre-class TTS reminder. Auto-navigate prompt. |
| `ObstacleDetector.kt` | Opt-in camera mode. TFLite MobileNet SSD at 1 FPS. Accelerometer orientation validation. |
| `CheckinManager.kt` | Manual "I'm here" check-in. Query nearby check-ins. 30-min TTL. |

### Module: `data/` — Data Layer

| File | Purpose |
|---|---|
| `MapRepository.kt` | Local map caching with version hash checking. Downloads from Firebase only when map has changed. |
| `UserRepository.kt` | User profile management. Preferences (haptic intensity, TTS speed, emergency contacts). |

### Module: `ui/` — User Interface

| File | Purpose |
|---|---|
| `MainActivity.kt` | Main screen with TalkBack-compatible layout. Large touch targets. High contrast. |
| `NavigationActivity.kt` | Active navigation screen with real-time status and feedback controls. |
| `SettingsActivity.kt` | Preferences: haptic intensity, TTS speed/language, emergency contacts, stride calibration. |

---

## 19. Design Decision Log

| # | Decision | Alternatives Considered | Rationale |
|---|---|---|---|
| 1 | **BLE Beacons** for positioning | UWB, Wi-Fi RTT, Visual SLAM | Cost-effective ($10-15/beacon), universally supported on Android, sufficient accuracy for indoor wayfinding. UWB too expensive, Wi-Fi RTT lacks infrastructure, Visual SLAM drains battery. |
| 2 | **Camera-based Obstacle Detection** (opt-in) | Ultrasonic sensors on cane | No extra hardware; but made opt-in due to battery/ergonomic concerns from review. |
| 3 | **Graph-based A* routing** | NavMesh, Grid-based | Structured hallways map naturally to a topological graph; easy turn-by-turn generation at nodes. |
| 4 | **Firebase (Spark Plan)** | AWS, Custom VPS | Real-time sync out-of-box, generous free tier, zero ops overhead for students. |
| 5 | **Thick Client / Local-First** | Thin Client (cloud routes) | Offline safety: navigation MUST work without internet. Privacy: no location in cloud. Cost: stays in free tier. |
| 6 | **Proximity + PDR** (no fingerprinting) | RSSI Fingerprinting, Trilateration | Fingerprinting is too brittle (crowds degrade it, requires constant re-surveying). Proximity is robust and zero-maintenance. |
| 7 | **Opt-in camera at 1 FPS** | Always-on high FPS | Saves battery (camera is the biggest drain). Improves ergonomics (don't need to hold phone up constantly). |
| 8 | **Local-only location processing** | Constant Firebase syncing | Stays within free tier limits. Preserves user privacy. Eliminates network dependency. |
| 9 | **Volume button long-press** | Shake-to-activate | Prevents false positives from walking, stumbling, or cane movements. Physical button is reliable. |
| 10 | **Relative gyro heading** | Absolute compass heading | Avoids dangerous magnetometer distortion from structural steel in buildings. Gyroscope is reliable indoors. |
| 11 | **Distinct haptic patterns** per event | Shared continuous vibration | Critical safety: user MUST distinguish arrival from danger instantly by feel. |
| 12 | **Step-count distances** | Meter-based distances | Blind users gauge distance by steps, not abstract visual measurements. |
| 13 | **Audio Focus management** | Unmanaged concurrent audio | Prevents sensory overload; navigation TTS takes priority over all other audio. |
| 14 | **Local alarm SOS fallback** | Cloud-only SOS | Ensures emergency alerts work even in network dead zones (stairwells, basements). |

---

## 20. Multi-Agent Design Review Results

### Review Process
The design was validated through a **structured 5-agent peer review** before any code was written:

| Phase | Agent | Role | Findings |
|---|---|---|---|
| Phase 1 | 🏗️ Primary Designer | Created initial architecture | Comprehensive 12-section design |
| Phase 2a | 🔴 Skeptic/Challenger | "How will this fail?" | 6 failure modes identified |
| Phase 2b | 🟡 Constraint Guardian | "What limits are violated?" | 5 constraint violations found |
| Phase 2c | 🟢 User Advocate | "Can a blind student use this?" | 5 accessibility concerns + "Day in the Life" |
| Phase 3 | ⚖️ Integrator/Arbiter | Resolve all objections | All 16 accepted → APPROVED |

### All 16 Objections (Accepted & Resolved)

| # | Objection | Source | Severity | Resolution |
|---|---|---|---|---|
| S1 | RSSI attenuation in crowds breaks fingerprinting | Skeptic | Critical | Abandoned fingerprinting → Proximity + PDR |
| S2 | Blind user can't aim camera reliably | Skeptic | Critical | Added accelerometer orientation validation |
| S3 | Indoor steel distorts compass heading | Skeptic | High | Replaced compass with gyroscope |
| S4 | SOS fails in network dead zones | Skeptic | High | Added local alarm + SMS fallback |
| S5 | OEM battery managers kill BLE scanner | Skeptic | High | Battery exemption + TTS alert on kill |
| S6 | Social features are unnecessary for PoC | Skeptic | Medium | Removed background sync → manual check-ins |
| CG1 | Firebase free tier would be exhausted | Constraint | Critical | Local-only positioning, no cloud uploads |
| CG2 | Camera + BLE drains battery in <2 hours | Constraint | High | Opt-in camera at 1 FPS |
| CG3 | SOS reliability insufficient | Constraint | High | Local alarm fallback (duplicate of S4) |
| CG4 | Fingerprinting requires constant maintenance | Constraint | High | Proximity + PDR (no radio map needed) |
| CG5 | Centralized location tracking = privacy risk | Constraint | High | Local-only processing, SOS-only upload |
| UA1 | Same haptic for arrival AND danger | User Advocate | Critical | Distinct patterns per event |
| UA2 | Holding phone up for camera = fatiguing | User Advocate | High | Camera is opt-in mode only |
| UA3 | TTS + TalkBack + alerts = sensory overload | User Advocate | High | Audio Focus priority management |
| UA4 | "In 5 meters" is meaningless to blind users | User Advocate | Medium | Step counts instead of meters |
| UA5 | Shake gesture triggers during normal walking | User Advocate | Medium | Volume button long-press instead |

---

## 21. Deployment Strategy

### Phase 1: Site Survey & Hardware (Week 1)
1. Walk the building, identify all decision points
2. Install ~20 BLE beacons at intersections, stairs, elevators, room entrances
3. Record beacon Minor IDs and physical locations
4. Test beacon range and signal strength

### Phase 2: Digital Map Creation (Week 1-2)
1. Set up Firebase project
2. Deploy admin portal
3. Upload floor plan image
4. Place beacon nodes on floor plan
5. Draw walkable edges between nodes
6. Set room names, descriptions, accessibility flags

### Phase 3: App Development & Testing (Week 2-4)
1. Build Android app in Android Studio
2. Alpha testing: Developer walks routes, calibrates step counter
3. Tune RSSI thresholds for beacon proximity detection
4. Verify haptic/audio timing and accuracy

### Phase 4: User Testing (Week 4-5)
1. Beta testing with visually impaired students (developer shadowing)
2. Iterate on feedback timing, audio clarity, haptic distinctiveness
3. Calibrate step counter for different walking speeds
4. Test SOS fallback in dead zones

### Phase 5: Final Prototype (Week 5-6)
1. End-to-end demo of all 7 features
2. Prepare presentation materials
3. Document results and observations

---

## 22. Future Scope

| Enhancement | Description |
|---|---|
| **Multi-building support** | Extend to full campus with outdoor GPS → indoor BLE handoff |
| **iOS app** | Cross-platform with Flutter or native Swift |
| **UWB positioning** | Higher accuracy (~10cm) when UWB beacons become affordable |
| **Crowd-sourced obstacle reporting** | Users report obstacles that persist for other users |
| **AR glasses integration** | Audio-only AR glasses for hands-free navigation |
| **Machine learning route optimization** | Learn popular routes and optimize pathfinding |
| **Multi-language support** | TTS in regional languages |
| **Integration with university LMS** | Automatic schedule sync from Moodle/Canvas |

---

## 23. References

1. World Health Organization. "Blindness and Vision Impairment." WHO Fact Sheet, 2023.
2. Apple Inc. "iBeacon - Apple Developer." developer.apple.com.
3. Google. "Eddystone Protocol Specification." github.com/google/eddystone.
4. Hart, P.E., Nilsson, N.J., Raphael, B. "A Formal Basis for the Heuristic Determination of Minimum Cost Paths." IEEE Transactions, 1968.
5. Google Firebase Documentation. firebase.google.com/docs.
6. TensorFlow Lite Object Detection. tensorflow.org/lite/examples/object_detection.
7. Android Accessibility Guidelines. developer.android.com/guide/topics/ui/accessibility.
8. Nordic Semiconductor. "nRF51822 / nRF52832 Product Specifications."
9. Zafari, F., et al. "A Survey of Indoor Localization Systems and Technologies." IEEE Communications Surveys & Tutorials, 2019.
10. Davidson, P., Piché, R. "A Survey of Selected Indoor Positioning Methods for Smartphones." IEEE Communications Surveys & Tutorials, 2017.

---

> **Document Version**: 1.0
> **Last Updated**: August 2026
> **Project**: NavGuide — IoT-Guided Indoor Navigation System
> **Type**: Academic Proof-of-Concept Prototype
