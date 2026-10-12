package com.navguide.navigation

import com.navguide.feedback.AudioManager
import com.navguide.feedback.HapticFeedback
import com.navguide.positioning.PositioningEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RouteManager(
    private val graph: NavigationGraph,
    private val positioningEngine: PositioningEngine,
    private val audioManager: AudioManager,
    private val hapticFeedback: HapticFeedback
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    private var currentRoute: List<Edge>? = null
    private var currentEdgeIndex = 0
    private var stepsTakenOnCurrentEdge = 0

    fun startNavigation(startId: String, endId: String) {
        val path = graph.findShortestPath(startId, endId)
        if (path.isNullOrEmpty()) {
            audioManager.speak("Unable to find a route to the destination.")
            return
        }
        
        currentRoute = path
        currentEdgeIndex = 0
        stepsTakenOnCurrentEdge = 0
        
        audioManager.speak("Navigation started. Route found. Walk straight.")
        hapticFeedback.playPattern(HapticFeedback.Pattern.DOUBLE_PULSE) // Straight
        
        // In a real implementation, you'd collect step counts and gyro heading
        // to update progress along the edge.
        // E.g., listening to step events to decrement distance remaining on current edge.
    }

    fun onStepDetected() {
        val route = currentRoute ?: return
        if (currentEdgeIndex >= route.size) {
            audioManager.speak("You have arrived at your destination.")
            hapticFeedback.playPattern(HapticFeedback.Pattern.SMOOTH_LONG) // Arrival
            currentRoute = null
            return
        }

        stepsTakenOnCurrentEdge++
        val currentEdge = route[currentEdgeIndex]
        
        if (stepsTakenOnCurrentEdge >= currentEdge.distanceSteps) {
            // Reached node
            currentEdgeIndex++
            stepsTakenOnCurrentEdge = 0
            
            if (currentEdgeIndex < route.size) {
                val nextEdge = route[currentEdgeIndex]
                val turnAngle = calculateTurnAngle(currentEdge.directionHeading, nextEdge.directionHeading)
                giveTurnInstruction(turnAngle)
            } else {
                audioManager.speak("You have arrived at your destination.")
                hapticFeedback.playPattern(HapticFeedback.Pattern.SMOOTH_LONG) // Arrival
                currentRoute = null
            }
        } else {
            // Drift detection / off-route logic would be evaluated here
            // If heading differs significantly from currentEdge.directionHeading:
            // hapticFeedback.playPattern(HapticFeedback.Pattern.ALTERNATING) // off-route
        }
    }
    
    private fun calculateTurnAngle(currentHeading: Float, nextHeading: Float): Float {
        var diff = nextHeading - currentHeading
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360
        return diff
    }
    
    private fun giveTurnInstruction(turnAngle: Float) {
        when {
            turnAngle > 45 && turnAngle <= 135 -> {
                audioManager.speak("Turn right.")
                hapticFeedback.playPattern(HapticFeedback.Pattern.LONG_PULSE)
            }
            turnAngle < -45 && turnAngle >= -135 -> {
                audioManager.speak("Turn left.")
                hapticFeedback.playPattern(HapticFeedback.Pattern.SHORT_PULSE)
            }
            turnAngle > 135 || turnAngle < -135 -> {
                audioManager.speak("Turn around.")
                hapticFeedback.playPattern(HapticFeedback.Pattern.ALTERNATING)
            }
            else -> {
                audioManager.speak("Continue straight.")
                hapticFeedback.playPattern(HapticFeedback.Pattern.DOUBLE_PULSE)
            }
        }
    }
}
