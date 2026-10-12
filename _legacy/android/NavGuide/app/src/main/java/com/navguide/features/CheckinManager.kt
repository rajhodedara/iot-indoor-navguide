package com.navguide.features

import com.navguide.feedback.AudioManager

class CheckinManager(
    private val audioManager: AudioManager
) {
    fun checkIn(locationId: String) {
        // Write to Firestore with 30-min TTL
        // Example: Firestore.collection("checkins").add(userId, locationId, timestamp)
        audioManager.speak("You have successfully checked in.")
    }

    fun queryNearbyCheckins(locationId: String) {
        // Query Firestore for check-ins in the last 30 minutes at locationId
        val checkedInCount = 2 // Mock value
        if (checkedInCount > 0) {
            audioManager.speak("There are $checkedInCount other students checked in nearby.")
        } else {
            audioManager.speak("No other students are checked in nearby.")
        }
    }
}
