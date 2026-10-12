package com.navguide.sos

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import com.navguide.R
import com.navguide.guidance.HapticPattern
import com.navguide.guidance.Haptics
import com.navguide.guidance.Speaker

/**
 * Local SOS: loud alarm + SMS to the saved contact (privacy-first: no cloud involved).
 */
class SosController(
    private val context: Context,
    private val speaker: Speaker,
    private val haptics: Haptics
) {
    private var ringtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())

    fun trigger(contact: String?, locationName: String?) {
        if (contact.isNullOrBlank()) {
            speaker.speak(context.getString(R.string.sos_no_contact))
            return
        }
        speaker.speak(context.getString(R.string.sos_triggered))
        haptics.play(HapticPattern.SOS_ALARM)
        playAlarm()
        sendSms(contact, locationName)
    }

    private fun playAlarm() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
            handler.postDelayed({ stopAlarm() }, 30_000)
        } catch (_: Exception) {
        }
    }

    fun stopAlarm() {
        try { ringtone?.stop() } catch (_: Exception) { }
        ringtone = null
    }

    private fun sendSms(contact: String, locationName: String?) {
        val where = if (locationName.isNullOrBlank()) "somewhere in the building" else "near $locationName"
        val message = "EMERGENCY: A NavGuide user needs help $where. Please respond immediately."
        try {
            val sms = context.getSystemService(SmsManager::class.java)
            sms?.sendTextMessage(contact, null, message, null, null)
        } catch (e: SecurityException) {
            speaker.speak("SMS permission is missing. Please call for help.")
        } catch (_: Exception) {
            speaker.speak("Could not send the emergency message.")
        }
    }
}
