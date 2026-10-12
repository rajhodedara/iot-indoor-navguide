package com.navguide.features

import android.content.Context
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import com.navguide.feedback.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SosManager(
    private val context: Context,
    private val audioManager: AudioManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    fun triggerSos() {
        audioManager.speak("SOS activated. Contacting emergency services.")
        
        coroutineScope.launch {
            // Attempt primary network approach: Firestore write / FCM
            val success = tryNetworkSos()
            
            if (!success) {
                // Fallback local alarm and SMS
                triggerFallbackSos()
            }
        }
    }

    private suspend fun tryNetworkSos(): Boolean {
        // Simulate network call with timeout
        return withTimeoutOrNull(5000L) {
            // In real code: Firebase.firestore.collection("sos").add(...)
            // return@withTimeoutOrNull true on success
            // Simulating failure here to show fallback
            false 
        } ?: false
    }

    private fun triggerFallbackSos() {
        audioManager.speak("Network unavailable. Falling back to local alarm and SMS.")
        
        // 1. Max volume alarm
        playLoudAlarm()
        
        // 2. SMS to emergency contacts
        sendEmergencySms()
    }
    
    private fun playLoudAlarm() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone.play()
            
            // Stop alarm after 30 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                ringtone.stop()
            }, 30000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendEmergencySms() {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            // Emergency contact would be fetched from UserRepository
            val emergencyNumber = "1234567890" 
            val message = "Emergency! I need assistance at my current location."
            smsManager.sendTextMessage(emergencyNumber, null, message, null, null)
        } catch (e: SecurityException) {
            audioManager.speak("SMS permission not granted for emergency fallback.")
        }
    }
}
