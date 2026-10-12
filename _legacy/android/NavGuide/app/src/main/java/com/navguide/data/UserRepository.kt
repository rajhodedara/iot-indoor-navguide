package com.navguide.data

import android.content.Context
import android.content.SharedPreferences

class UserRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("NavGuidePrefs", Context.MODE_PRIVATE)

    fun saveEmergencyContact(number: String) {
        prefs.edit().putString("emergency_contact", number).apply()
    }

    fun getEmergencyContact(): String? {
        return prefs.getString("emergency_contact", null)
    }

    fun setHapticIntensity(intensity: Int) {
        prefs.edit().putInt("haptic_intensity", intensity).apply()
    }

    fun getHapticIntensity(): Int {
        return prefs.getInt("haptic_intensity", 100) // Default 100%
    }
    
    fun setTtsSpeed(speed: Float) {
        prefs.edit().putFloat("tts_speed", speed).apply()
    }

    fun getTtsSpeed(): Float {
        return prefs.getFloat("tts_speed", 1.0f)
    }
}
