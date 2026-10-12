package com.navguide.feedback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceCommandManager(
    private val context: Context,
    private val audioManager: AudioManager
) {
    private var speechRecognizer: SpeechRecognizer? = null
    
    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupListener()
        } else {
            audioManager.speak("Speech recognition is not available on this device.")
        }
    }

    private fun setupListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                audioManager.speak("Sorry, I didn't catch that. Please try again.")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0].lowercase()
                    parseCommand(command)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        audioManager.speak("Listening for command.")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    private fun parseCommand(command: String) {
        when {
            command.contains("navigate") -> {
                audioManager.speak("Navigating to destination.")
                // Trigger navigation intent/callback
            }
            command.contains("schedule") -> {
                audioManager.speak("Checking schedule.")
                // Trigger schedule intent/callback
            }
            command.contains("sos") || command.contains("help") -> {
                audioManager.speak("Activating SOS.")
                // Trigger SOS manager
            }
            command.contains("obstacle") || command.contains("camera") -> {
                audioManager.speak("Toggling obstacle detection.")
                // Toggle obstacle detector
            }
            else -> {
                audioManager.speak("Command not recognized.")
            }
        }
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }
}
