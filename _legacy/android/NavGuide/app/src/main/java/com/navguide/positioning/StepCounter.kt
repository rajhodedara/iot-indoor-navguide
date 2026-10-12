package com.navguide.positioning

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class StepCounter(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _stepFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val stepFlow = _stepFlow.asSharedFlow()

    // Peak detection variables
    private var lastZ = 0f
    private var currentZ = 0f
    private var isPeak = false
    private val threshold = 1.5f // Threshold for step detection

    fun start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val z = event.values[2]
            currentZ = z
            
            if (currentZ > lastZ + threshold && !isPeak) {
                isPeak = true
            } else if (currentZ < lastZ - threshold && isPeak) {
                isPeak = false
                _stepFlow.tryEmit(Unit)
            }
            lastZ = currentZ
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
