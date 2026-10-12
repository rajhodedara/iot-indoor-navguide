package com.navguide.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.navguide.R
import com.navguide.data.Prefs
import com.navguide.guidance.HapticPattern
import com.navguide.guidance.HapticsImpl
import com.navguide.guidance.SpeakerTts

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var speaker: SpeakerTts
    private lateinit var haptics: HapticsImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)
        speaker = SpeakerTts(this)
        haptics = HapticsImpl(this)

        val etContact = findViewById<EditText>(R.id.etContact)
        etContact.setText(prefs.emergencyContact ?: "")

        val sbStride = findViewById<SeekBar>(R.id.sbStride)
        val tvStrideValue = findViewById<TextView>(R.id.tvStrideValue)
        sbStride.max = 40
        sbStride.progress = ((prefs.strideM - 0.5f) * 100).toInt().coerceIn(0, 40)

        fun strideValue(): Float = 0.5f + sbStride.progress / 100f
        fun updateStrideLabel() {
            tvStrideValue.text = String.format("%.2f m", strideValue())
        }
        updateStrideLabel()
        sbStride.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) =
                updateStrideLabel()
            override fun onStartTrackingTouch(seekBar: SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }
        })

        val rgSpeed = findViewById<RadioGroup>(R.id.rgSpeed)
        when {
            prefs.ttsSpeed < 0.95f -> rgSpeed.check(R.id.rbSlow)
            prefs.ttsSpeed > 1.05f -> rgSpeed.check(R.id.rbFast)
            else -> rgSpeed.check(R.id.rbNormal)
        }

        fun speedFromUi(): Float = when (rgSpeed.checkedRadioButtonId) {
            R.id.rbSlow -> 0.85f
            R.id.rbFast -> 1.15f
            else -> 1.0f
        }

        findViewById<Button>(R.id.btnTestVoice).setOnClickListener {
            speaker.speed = speedFromUi()
            speaker.speak("This is the NavGuide voice at the selected speed.")
        }
        findViewById<Button>(R.id.btnTestVibration).setOnClickListener {
            speaker.speak("Playing vibration patterns.")
            haptics.playSequence(
                listOf(HapticPattern.LEFT, HapticPattern.RIGHT, HapticPattern.STRAIGHT, HapticPattern.ARRIVAL)
            )
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.emergencyContact = etContact.text.toString().trim().ifBlank { null }
            prefs.strideM = strideValue()
            prefs.ttsSpeed = speedFromUi()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speaker.shutdown()
    }
}
