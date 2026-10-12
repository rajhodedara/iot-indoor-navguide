package com.navguide.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.navguide.R
import com.navguide.feedback.AudioManager
import com.navguide.feedback.VoiceCommandManager
import com.navguide.features.SosManager
import com.navguide.ble.BleScanner

class MainActivity : AppCompatActivity() {

    private lateinit var audioManager: AudioManager
    private lateinit var voiceCommandManager: VoiceCommandManager
    private lateinit var sosManager: SosManager

    private var volumeDownPressed = false
    private var volumeUpPressed = false
    private var volumeDownPressTime = 0L
    private var sosTriggered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = AudioManager(this)
        voiceCommandManager = VoiceCommandManager(this, audioManager)
        sosManager = SosManager(this, audioManager)

        // Start BLE Service
        val serviceIntent = Intent(this, BleScanner::class.java)
        startForegroundService(serviceIntent)

        val navButton = findViewById<Button>(R.id.btnNavigate)
        navButton.setOnClickListener {
            startActivity(Intent(this, NavigationActivity::class.java))
        }
        
        val settingsButton = findViewById<Button>(R.id.btnSettings)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (!volumeDownPressed) {
                volumeDownPressed = true
                volumeDownPressTime = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - volumeDownPressTime > 3000 && !volumeUpPressed) {
                // Long press volume down (3s) for voice command
                voiceCommandManager.startListening()
            }
        }
        
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeUpPressed = true
        }

        // SOS condition: Vol up + Vol down for 3s
        if (volumeUpPressed && volumeDownPressed && !sosTriggered) {
            if (System.currentTimeMillis() - volumeDownPressTime > 3000) {
                sosManager.triggerSos()
                sosTriggered = true
            }
        }
        
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeDownPressed = false
            voiceCommandManager.stopListening()
            sosTriggered = false
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeUpPressed = false
            sosTriggered = false
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceCommandManager.destroy()
        audioManager.shutdown()
    }
}
