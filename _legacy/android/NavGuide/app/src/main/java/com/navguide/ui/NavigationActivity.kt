package com.navguide.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.navguide.R
import com.navguide.features.ObstacleDetector
import com.navguide.feedback.AudioManager

class NavigationActivity : AppCompatActivity() {

    private lateinit var obstacleDetector: ObstacleDetector
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        audioManager = AudioManager(this)
        obstacleDetector = ObstacleDetector(this, audioManager)

        val statusText = findViewById<TextView>(R.id.tvStatus)
        statusText.text = "Navigating to Destination"

        val toggleObstacleBtn = findViewById<Button>(R.id.btnToggleObstacles)
        toggleObstacleBtn.setOnClickListener {
            obstacleDetector.toggle()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.shutdown()
    }
}
