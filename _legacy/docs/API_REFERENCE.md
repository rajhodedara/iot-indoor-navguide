# API Reference

This document covers the Firebase Cloud Functions API and the Firestore data model used by the IoT Indoor Navigation System.

## 1. Cloud Functions

All functions are deployed to Firebase as HTTPS Callable functions or HTTP endpoints.

### `triggerSOS` (Callable)
Triggers an emergency alert, broadcasting the user's location to admins.

- **Request Payload:**
  ```json
  {
    "beaconId": "Major1-Minor12",
    "timestamp": 1691234567890
  }
  ```
- **Response Payload:**
  ```json
  {
    "status": "success",
    "message": "SOS dispatched to security."
  }
  ```
- **Error Codes:**
  - `unauthenticated`: User is not logged in.
  - `invalid-argument`: Missing `beaconId`.

### `getRoute` (Callable)
Calculates the shortest path using Dijkstra's algorithm based on the current map graph.

- **Request Payload:**
  ```json
  {
    "startNode": "Major1-Minor2",
    "endNode": "Major1-Minor15"
  }
  ```
- **Response Payload:**
  ```json
  {
    "status": "success",
    "path": ["Major1-Minor2", "Major1-Minor5", "Major1-Minor10", "Major1-Minor15"],
    "instructions": [
      "Walk straight 10 meters.",
      "Turn right.",
      "Arrive at destination."
    ]
  }
  ```

## 2. Firestore Data Model

### Collection: `users`
Stores user profiles and preferences.
- Document ID: `UID` (from Firebase Auth)
- Fields:
  - `name` (String)
  - `role` (String: "student", "admin")
  - `emergencyContact` (String)
  - `schedule` (Array of Map)
    - `course` (String)
    - `roomId` (String)
    - `time` (Timestamp)

### Collection: `beacons`
Stores physical beacon metadata.
- Document ID: `{Major}-{Minor}` (e.g., `1-12`)
- Fields:
  - `major` (Number)
  - `minor` (Number)
  - `label` (String: "Room 101", "Main Entrance")
  - `coordinates` (Map: `x`, `y` for admin map plotting)
  - `floor` (Number)

### Collection: `edges`
Defines the walkable paths between beacons for routing.
- Document ID: Auto-generated
- Fields:
  - `nodeA` (String: Reference to `beacons` ID)
  - `nodeB` (String: Reference to `beacons` ID)
  - `distance` (Number: meters)
  - `direction` (String: "straight", "left", "right" relative to nodeA)

### Collection: `sos_alerts`
Active emergency alerts.
- Document ID: Auto-generated
- Fields:
  - `uid` (String: User requesting help)
  - `beaconId` (String: Last known location)
  - `timestamp` (Timestamp)
  - `status` (String: "active", "resolved")
