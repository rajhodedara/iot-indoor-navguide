package com.navguide.guidance

import com.navguide.core.GeoMath
import com.navguide.core.MapDoc

enum class Turn { LEFT, RIGHT, STRAIGHT, UTURN, STAIRS, ELEVATOR }

/**
 * Pure instruction text generation. Steps-based phrasing (blind users cannot gauge meters).
 */
object InstructionGenerator {

    /**
     * Direction change arriving at [nodeId] coming from [prevId], continuing to [nextId].
     * Edges that change floors are never classified as turns (use nextTurnAhead for transitions).
     */
    fun turnAt(doc: MapDoc, prevId: String, nodeId: String, nextId: String): Turn {
        val prev = doc.nodeById[prevId] ?: return Turn.STRAIGHT
        val node = doc.nodeById[nodeId] ?: return Turn.STRAIGHT
        val next = doc.nodeById[nextId] ?: return Turn.STRAIGHT
        if (prev.floor != node.floor || node.floor != next.floor) return Turn.STRAIGHT
        val inBearing = GeoMath.bearing(prev.x, prev.y, node.x, node.y)
        val outBearing = GeoMath.bearing(node.x, node.y, next.x, next.y)
        val d = GeoMath.angleDiffDeg(inBearing, outBearing)
        return when {
            kotlin.math.abs(d) <= 30.0 -> Turn.STRAIGHT
            d > 30.0 && d < 135.0 -> Turn.RIGHT
            d < -30.0 && d > -135.0 -> Turn.LEFT
            else -> Turn.UTURN
        }
    }

    /** Command spoken when the user is AT the turn point. Null for continue-straight. */
    fun turnCommand(turn: Turn): String? = when (turn) {
        Turn.LEFT -> "Turn left."
        Turn.RIGHT -> "Turn right."
        Turn.UTURN -> "Turn around."
        Turn.STRAIGHT, Turn.STAIRS, Turn.ELEVATOR -> null
    }

    fun turnName(turn: Turn): String = when (turn) {
        Turn.LEFT -> "left"
        Turn.RIGHT -> "right"
        Turn.UTURN -> "around"
        Turn.STRAIGHT -> "straight"
        Turn.STAIRS -> "stairs"
        Turn.ELEVATOR -> "elevator"
    }

    /** Countdown while walking toward a turn or a floor transition. */
    fun countdown(turn: Turn, steps: Int): String = when (turn) {
        Turn.STAIRS -> "Stairs in $steps steps."
        Turn.ELEVATOR -> "Elevator in $steps steps."
        else -> "Turn ${turnName(turn)} in $steps steps."
    }

    /** Opening instruction after a route is computed. */
    fun routeStart(turn: Turn, stepsToTurn: Int, destinationName: String): String = when (turn) {
        Turn.STRAIGHT -> "Navigation started. Walk straight for $stepsToTurn steps toward $destinationName."
        Turn.STAIRS, Turn.ELEVATOR -> "Navigation started. Walk straight for $stepsToTurn steps, then take the ${turnName(turn)}."
        else -> "Navigation started. Walk straight for $stepsToTurn steps, then turn ${turnName(turn)}."
    }

    /** Spoken when standing at a stairs/elevator node that changes floor. */
    fun transition(fromType: String, toFloor: Int): String {
        val verb = if (fromType == "elevator") "Take the elevator to floor" else "Take the stairs to floor"
        return "$verb $toFloor."
    }

    fun arrival(nodeName: String): String = "You have arrived at $nodeName."

    const val OFF_ROUTE = "You are off route. Recalculating."
    const val KEEP_WALKING = "Keep walking straight."
}
