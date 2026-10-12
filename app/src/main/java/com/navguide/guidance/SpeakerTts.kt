package com.navguide.guidance

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.ArrayDeque
import java.util.Locale

/** TextToSpeech implementation of [Speaker]. Queues early calls until the engine is ready. */
class SpeakerTts(context: Context) : Speaker, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private val pending = ArrayDeque<String>()

    var speed: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val lang = tts?.setLanguage(Locale.getDefault())
            if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(speed)
            ready = true
            while (pending.isNotEmpty()) {
                speakNow(pending.pollFirst() ?: "")
            }
        }
    }

    override fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            if (pending.size >= 3) pending.pollFirst()
            pending.addLast(text)
            return
        }
        speakNow(text)
    }

    private fun speakNow(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "navguide-${System.nanoTime()}")
    }

    override fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
