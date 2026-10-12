package com.navguide.features

import com.navguide.feedback.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ScheduledClass(
    val id: String,
    val name: String,
    val roomId: String,
    val timestampMs: Long
)

class ScheduleManager(
    private val audioManager: AudioManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var upcomingClasses = listOf<ScheduledClass>()

    fun fetchSchedule() {
        // In real implementation: Fetch from Firestore
        coroutineScope.launch {
            // Mocking schedule fetching
            delay(1000)
            upcomingClasses = listOf(
                ScheduledClass("1", "Math 101", "Room_A", System.currentTimeMillis() + 15 * 60 * 1000)
            )
            monitorSchedule()
        }
    }

    private fun monitorSchedule() {
        coroutineScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                upcomingClasses.forEach { cls ->
                    val timeDiff = cls.timestampMs - now
                    // Check if class is in 10 minutes (600,000 ms) +/- 30s
                    if (timeDiff in (600000L - 30000L)..(600000L + 30000L)) {
                        promptNavigation(cls)
                    }
                }
                delay(60000) // Check every minute
            }
        }
    }

    private fun promptNavigation(cls: ScheduledClass) {
        audioManager.speak("You have ${cls.name} in 10 minutes. Would you like to navigate to ${cls.roomId}?")
        // This would integrate with VoiceCommandManager to listen for "Yes"
    }
}
