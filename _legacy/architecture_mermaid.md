```text
flowchart LR
    %% Compact PPT Architecture (Fits 16:9 easily)
    classDef hw fill:#0f172a,stroke:#38bdf8,stroke-width:2px,color:#fff;
    classDef edge fill:#1e1b4b,stroke:#a855f7,stroke-width:2px,color:#fff;
    classDef ui fill:#4c0519,stroke:#f43f5e,stroke-width:2px,color:#fff;
    classDef db fill:#422006,stroke:#f59e0b,stroke-width:2px,color:#fff;

    subgraph Hardware ["📡 1. IoT Sensors"]
        direction TB
        ESP["ESP32 & BLE Beacons"]:::hw
        Sensors["IMU & ToF Sensors"]:::hw
    end

    subgraph Processing ["📱 2. Mobile Engine"]
        direction TB
        Fusion["Sensor Fusion Unit"]:::edge
        AStar["A* Offline Routing"]:::edge
    end

    subgraph Output ["📳 3. User Feedback"]
        direction TB
        Haptics["DRV2605L Haptics"]:::ui
        TTS["Voice Guidance (TTS)"]:::ui
    end

    subgraph Backend ["☁️ 4. Cloud"]
        direction TB
        Firebase[("Firebase Sync")]:::db
    end

    %% Connections
    ESP --> Fusion
    Sensors --> Fusion
    Fusion --> AStar
    AStar --> Haptics
    AStar --> TTS
    Firebase -.->|Map Update| AStar
```
