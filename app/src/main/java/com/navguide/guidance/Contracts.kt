package com.navguide.guidance

/** Output contracts used by the navigation session. Keep Android out of this file. */

interface Speaker {
    fun speak(text: String)
    fun stop()
}

enum class HapticPattern { STRAIGHT, LEFT, RIGHT, ARRIVAL, OFF_ROUTE, SOS_ALARM }

interface Haptics {
    fun play(pattern: HapticPattern)
}
