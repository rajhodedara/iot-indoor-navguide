package com.navguide.data

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val p: SharedPreferences = context.getSharedPreferences("navguide_prefs", Context.MODE_PRIVATE)

    var emergencyContact: String?
        get() = p.getString("emergency_contact", null)
        set(value) = p.edit().putString("emergency_contact", value).apply()

    var strideM: Float
        get() = p.getFloat("stride_m", 0.7f)
        set(value) = p.edit().putFloat("stride_m", value).apply()

    var ttsSpeed: Float
        get() = p.getFloat("tts_speed", 1.0f)
        set(value) = p.edit().putFloat("tts_speed", value).apply()
}
