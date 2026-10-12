package com.navguide.features

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.navguide.feedback.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ObstacleDetector(
    private val context: Context,
    private val audioManager: AudioManager
) : ImageAnalysis.Analyzer, SensorEventListener {
    
    private var isEnabled = false
    private var lastAnalysisTime = 0L
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private var isPhoneUpright = false

    fun toggle() {
        isEnabled = !isEnabled
        if (isEnabled) {
            audioManager.speak("Obstacle detection enabled. Please hold the phone upright.")
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            audioManager.speak("Obstacle detection disabled.")
            sensorManager.unregisterListener(this)
        }
    }

    override fun analyze(image: ImageProxy) {
        if (!isEnabled || !isPhoneUpright) {
            image.close()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < 1000) { // 1 FPS
            image.close()
            return
        }
        lastAnalysisTime = currentTime

        // Perform TFLite Object Detection (Mocked)
        coroutineScope.launch {
            detectObstacles(image)
            image.close()
        }
    }

    private suspend fun detectObstacles(image: ImageProxy) {
        // TFLite MobileNet SSD execution would go here
        // simulate detection delay
        delay(100)
        
        val objectDetected = false // mock result
        if (objectDetected) {
            // Play earcon sound for obstacle
            audioManager.playEarcon(1) 
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val y = event.values[1]
            val z = event.values[2]
            
            // Check if phone is relatively upright (y is positive, z is small)
            isPhoneUpright = y > 5.0f && Math.abs(z) < 5.0f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
