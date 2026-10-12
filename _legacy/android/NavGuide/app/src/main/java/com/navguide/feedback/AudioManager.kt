package com.navguide.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager as SystemAudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.LinkedList
import java.util.Queue

class AudioManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val systemAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager
    
    private val messageQueue: Queue<String> = LinkedList()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language data is missing or the language is not supported.
            } else {
                isTtsInitialized = true
                processQueue()
            }
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!isTtsInitialized) {
            messageQueue.add(text)
            return
        }

        if (requestAudioFocus()) {
            val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, queueMode, null, "NavGuideTTS")
            // Normally would abandon audio focus in an UtteranceProgressListener onDone
        }
    }

    private fun processQueue() {
        while (messageQueue.isNotEmpty()) {
            speak(messageQueue.poll() ?: "", flush = false)
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(SystemAudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { _ -> }
                .build()
            
            val result = systemAudioManager.requestAudioFocus(focusRequest)
            return result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = systemAudioManager.requestAudioFocus(
                null,
                SystemAudioManager.STREAM_ACCESSIBILITY,
                SystemAudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            return result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    fun playEarcon(soundId: Int) {
        // Implementation for earcon sounds (obstacles)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
