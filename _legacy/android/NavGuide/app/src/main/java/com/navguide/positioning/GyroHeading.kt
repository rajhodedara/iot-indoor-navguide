package com.navguide.positioning

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class GyroHeading(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    private var currentHeading: Float = 0f
    private var lastTimestamp: Long = 0

    // To handle drift compensation (simple high-pass filter concept could go here)
    
    fun start() {
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun getHeading(): Float = currentHeading

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            if (lastTimestamp != 0L) {
                val dT = (event.timestamp - lastTimestamp) * NS2S
                val axisZ = event.values[2]
                
                // Integrate to get angle (in radians)
                currentHeading += axisZ * dT
                
                // Normalize heading to 0-360 degrees
                val headingDegrees = Math.toDegrees(currentHeading.toDouble()).toFloat()
                currentHeading = (Math.toRadians((headingDegrees % 360 + 360) % 360.0)).toFloat()
            }
            lastTimestamp = event.timestamp
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val NS2S = 1.0f / 1000000000.0f
    }
}
