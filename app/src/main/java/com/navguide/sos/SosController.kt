package com.navguide.sos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.navguide.R
import com.navguide.guidance.HapticPattern
import com.navguide.guidance.Haptics
import com.navguide.guidance.Speaker

/**
 * Local SOS: loud alarm + a message to the saved contact (privacy-first: no cloud involved).
 *
 * Sending strategy:
 *  - SEND_SMS granted  -> sent silently in the background.
 *  - not granted       -> the Messages app is opened with the emergency text pre-filled
 *                         (no permission required; user just presses send).
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

        val canSendSilently = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

        if (canSendSilently) {
            try {
                val sms = context.getSystemService(SmsManager::class.java)
                sms?.sendTextMessage(contact, null, message, null, null)
                return
            } catch (_: Exception) {
                // fall through to the Messages-app fallback
            }
        }
        openMessagingApp(contact, message)
    }

    private fun openMessagingApp(contact: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$contact")).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            speaker.speak(context.getString(R.string.sos_opening_sms))
        } catch (_: Exception) {
            speaker.speak(context.getString(R.string.sos_could_not_open_sms))
        }
    }
}
