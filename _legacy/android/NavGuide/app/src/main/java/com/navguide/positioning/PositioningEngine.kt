package com.navguide.positioning

import com.navguide.ble.BeaconData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Position(val x: Double, val y: Double)

class PositioningEngine(
    private val stepCounter: StepCounter,
    private val gyroHeading: GyroHeading
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var currentPosition = Position(0.0, 0.0)
    private val stepLength = 0.7 // Average step length in meters

    private val _positionFlow = MutableStateFlow(currentPosition)
    val positionFlow = _positionFlow.asStateFlow()

    init {
        coroutineScope.launch {
            stepCounter.stepFlow.collect {
                updatePositionWithPdr()
            }
        }
    }

    fun start() {
        stepCounter.start()
        gyroHeading.start()
    }

    fun stop() {
        stepCounter.stop()
        gyroHeading.stop()
    }

    private fun updatePositionWithPdr() {
        val heading = gyroHeading.getHeading()
        
        // PDR calculation: x' = x + stepLength * cos(heading), y' = y + stepLength * sin(heading)
        val newX = currentPosition.x + stepLength * kotlin.math.cos(heading.toDouble())
        val newY = currentPosition.y + stepLength * kotlin.math.sin(heading.toDouble())
        
        currentPosition = Position(newX, newY)
        _positionFlow.value = currentPosition
    }

    fun updatePositionWithBeacon(beacon: BeaconData, beaconPosition: Position) {
        // Nearest beacon snap: Proximity-based correction
        // If distance < threshold, snap to beacon position to correct drift
        if (beacon.distance < 2.0 && beacon.distance > 0) {
            currentPosition = beaconPosition
            _positionFlow.value = currentPosition
            // Note: Heading drift correction could also be triggered here if there are known path vectors.
        }
    }
}
