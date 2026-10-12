# Deployment Guide

This guide provides a step-by-step walkthrough to take the IoT Indoor Navigation System from zero to a working prototype.

## 1. Prerequisites
- **Node.js**: v18.x or higher
- **Android Studio**: Giraffe or newer
- **Firebase CLI**: `npm install -g firebase-tools`
- **Hardware**: 1 Android device (Android 8.0+), 2+ BLE Beacons for testing.

## 2. Firebase Setup
1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2. Navigate to **Build > Firestore Database** and create a database (Start in production mode, we will deploy rules later).
3. Navigate to **Build > Authentication** and enable **Email/Password** sign-in.
4. On your local machine, open the terminal in the project root:
   ```bash
   firebase login
   firebase init
   ```
   *Select Firestore and Functions. Link them to your newly created project.*
5. Deploy the backend:
   ```bash
   cd firebase
   firebase deploy
   ```

## 3. Android Studio Setup
1. Register an Android App in your Firebase Console.
2. Download the `google-services.json` file.
3. Place `google-services.json` in the `/android-app/app/` directory.
4. Open the `/android-app` folder in Android Studio.
5. Sync Gradle.
6. Connect your physical Android device via USB (ensure Developer Options and USB Debugging are enabled).
7. Click **Run > Run 'app'**.
8. Grant necessary permissions on the device (Location, Nearby Devices, Microphone, Notifications).

## 4. Admin Portal & Map Creation
1. Open the `/admin-portal` directory.
2. Create a `.env` file based on `.env.example` and insert your Firebase Web SDK config.
3. Run `npm install` and `npm start`.
4. Log in using an Admin account (create an account in Firebase Auth, then manually set the `role: 'admin'` field in the corresponding Firestore user document).
5. **Create the Map**:
   - Upload a floor plan image.
   - Click to drop beacon nodes (assigning them the Minor IDs matching your physical beacons).
   - Draw paths between nodes to create the routing graph.
   - Save the map to Firestore.

## 5. Beacon Installation Walkthrough
1. Configure your hardware beacons using the manufacturer app (See `HARDWARE_SETUP.md`).
2. Walk to the physical locations dictated by your Admin Portal map.
3. Mount the beacons securely.
4. Mark the beacon as "Active" in your physical tracking spreadsheet.

## 6. First Navigation Test
1. Take the Android device to the starting beacon (e.g., Building Entrance).
2. Open the app and log in as a student user.
3. The app should automatically detect the closest beacon and announce, "You are at the Building Entrance."
4. Use Voice Commands: Double-tap the screen and say, "Take me to Room 101."
5. Follow the haptic and audio prompts. Verify that as you approach intermediate beacons, the instructions update correctly.

## 7. Calibration Tips
- **PDR Tuning**: If the app thinks the user has arrived before they reach the actual beacon, the step-length calibration in the app settings may need adjustment.
- **RSSI Thresholds**: If the app jumps to a new location too early, adjust the RSSI threshold in the Android app's `BeaconScannerManager.kt` from `-75` to `-70` to require the user to be closer.

## 8. Demo Day Checklist
- [ ] Ensure all 20 beacons have fresh batteries and are securely mounted.
- [ ] Verify the Firebase database has the correct, finalized routing graph.
- [ ] Pre-load the Android app on test devices.
- [ ] Fully charge the test Android devices.
- [ ] Test the SOS feature triggers a cloud function successfully.
- [ ] Wipe test user data from Firestore to start clean.
