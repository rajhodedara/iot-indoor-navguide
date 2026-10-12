package com.navguide.guidance

import com.navguide.ble.IBeaconReading
import com.navguide.core.GeoMath
import com.navguide.core.MapDoc
import com.navguide.positioning.PositionTracker
import com.navguide.routing.NavGraph
import com.navguide.routing.Router

/**
 * Drives a navigation session: beacon snaps + step counts in, spoken instructions out.
 * Pure Kotlin logic; TTS/haptics arrive through interfaces. Unit-testable on the JVM.
 */
class NavigationSession(
    private val doc: MapDoc,
    private val strideM: Double,
    private val speaker: Speaker,
    private val haptics: Haptics,
    private val onStatus: (String) -> Unit = {},
    private val onArrived: () -> Unit = {}
) {
    private val graph = NavGraph(doc)
    private val tracker = PositionTracker(doc)

    private var destinationId: String? = null
    private var route: List<String> = emptyList()
    private var currentIndex = 0
    private var stepsSinceNode = 0
    private var lastInstruction: String = ""
    private var countdownDoneForSegment = -1
    private var softPromptDoneForSegment = -1

    var active: Boolean = false
        private set
    var arrived: Boolean = false
        private set

    val currentLocationName: String?
        get() = tracker.currentNodeId?.let { doc.nodeById[it]?.name }

    fun start(destinationId: String) {
        this.destinationId = destinationId
        active = true
        arrived = false
        route = emptyList()
        currentIndex = 0
        stepsSinceNode = 0
        countdownDoneForSegment = -1
        softPromptDoneForSegment = -1

        val anchor = tracker.currentNodeId
        if (anchor == null) {
            updateStatus("Waiting for beacon signal…")
        } else {
            beginRouteFrom(anchor)
        }
    }

    fun onReading(reading: IBeaconReading) {
        if (!active) return
        val snapped = tracker.onReading(reading) ?: return
        handleSnap(snapped)
    }

    /** Simulator: user hops to a node directly. Also sets the start anchor when used before start(). */
    fun onNodeHop(nodeId: String) {
        val changed = tracker.forceNode(nodeId)
        if (active && changed) handleSnap(nodeId)
    }

    fun onStep() {
        if (!active) return
        stepsSinceNode++
        if (route.isEmpty()) {
            updateStatus("Waiting for position. Steps so far: $stepsSinceNode. Use the demo controls below to set your location.")
        } else {
            evaluateSegmentProgress()
        }
    }

    fun repeatLastInstruction() {
        if (lastInstruction.isNotEmpty()) {
            speaker.speak(lastInstruction)
        } else {
            speaker.speak("No instruction yet.")
        }
    }

    /** Demo: anchor at the entrance if the user hasn't set a position, then begin the route. */
    fun startAutoTour(): Boolean {
        if (!active) return false
        if (tracker.currentNodeId == null) {
            val startNode = doc.nodes.firstOrNull { it.type == "entrance" } ?: doc.nodes.firstOrNull() ?: return false
            tracker.forceNode(startNode.id)
            beginRouteFrom(startNode.id)
        }
        return route.isNotEmpty()
    }

    /** Demo/simulator: advance one step and automatically move to the next route node when its segment completes. */
    fun demoTick() {
        if (!active || route.isEmpty()) return
        onStep()
        if (!active) return
        if (currentIndex < route.size - 1 && stepsSinceNode >= stepsForSegment(currentIndex)) {
            onNodeHop(route[currentIndex + 1])
        }
    }

    fun stop() {
        active = false
        speaker.stop()
        updateStatus("Navigation stopped.")
    }

    // ---------------------------------------------------------------- internals

    private fun beginRouteFrom(anchorId: String) {
        val dest = destinationId ?: return
        if (anchorId == dest) {
            finishArrival()
            return
        }
        val path = Router.findRoute(doc, graph, anchorId, dest)
        if (path == null) {
            updateStatus("No route to destination.")
            speaker.speak("Sorry, no route was found to the destination.")
            active = false
            return
        }
        route = path
        currentIndex = 0
        stepsSinceNode = 0
        countdownDoneForSegment = -1
        softPromptDoneForSegment = -1
        speakRouteStart()
    }

    private fun handleSnap(nodeId: String) {
        if (route.isEmpty()) {
            beginRouteFrom(nodeId)
            return
        }
        if (nodeId == route.last()) {
            finishArrival()
            return
        }
        // Re-anchor: is the node on the remaining route?
        val foundIndex = route.indexOfFirstFrom(nodeId, currentIndex)
        if (foundIndex < 0) {
            // Off route — recompute from here.
            speaker.speak(InstructionGenerator.OFF_ROUTE)
            haptics.play(HapticPattern.OFF_ROUTE)
            beginRouteFrom(nodeId)
            return
        }
        val previousIndex = currentIndex
        currentIndex = foundIndex
        stepsSinceNode = 0
        countdownDoneForSegment = -1
        softPromptDoneForSegment = -1

        // Floor transition handled here (stairs/elevator node that changes floor).
        if (foundIndex > previousIndex) {
            val node = doc.nodeById[nodeId]
            val next = if (foundIndex + 1 <= route.size - 1) doc.nodeById[route[foundIndex + 1]] else null
            if (node != null && next != null && node.floor != next.floor) {
                say(InstructionGenerator.transition(node.type, next.floor))
                haptics.play(HapticPattern.TRANSITION)
                return
            }
        }

        // Command for the turn that happens here (relative to previous segment).
        val turn = turnAtCurrent()
        if (turn != null && turn != Turn.STRAIGHT) {
            val cmd = InstructionGenerator.turnCommand(turn)
            if (cmd != null) {
                say(cmd)
                haptics.play(turnToHaptic(turn))
            }
        } else {
            updateStatus("Continue straight.")
        }
    }

    private fun speakRouteStart() {
        val turnInfo = nextTurnAhead()
        val dest = destinationId?.let { doc.nodeById[it]?.name } ?: "destination"
        if (turnInfo == null) {
            say("Navigation started. Walk straight to $dest.")
            return
        }
        val steps = stepsToTurn(currentIndex, turnInfo.first)
        say(InstructionGenerator.routeStart(turnInfo.second, steps, dest))
    }

    /** Total steps from [fromSegment] through the segment that ends at the turn node. */
    private fun stepsToTurn(fromSegment: Int, turnSegment: Int): Int {
        var s = 0
        for (k in fromSegment..turnSegment) s += stepsForSegment(k)
        return s
    }

    /** Turn performed AT the current node (comparing previous and next segments). */
    private fun turnAtCurrent(): Turn? {
        if (currentIndex <= 0 || currentIndex >= route.size - 1) return null
        return InstructionGenerator.turnAt(
            doc,
            route[currentIndex - 1],
            route[currentIndex],
            route[currentIndex + 1]
        )
    }

    /** First turn or floor transition ahead of the current position: (segmentIndex, kind). */
    private fun nextTurnAhead(): Pair<Int, Turn>? {
        if (route.size < 2) return null
        for (i in currentIndex until route.size - 1) {
            val from = doc.nodeById[route[i]]
            val to = doc.nodeById[route[i + 1]]
            // Floor transition on this segment?
            if (from != null && to != null && from.floor != to.floor) {
                val kind = if (from.type == "elevator" || to.type == "elevator") Turn.ELEVATOR else Turn.STAIRS
                return i to kind
            }
            // Turn AT node i+1 relative to segment i -> i+1.
            if (i + 2 <= route.size - 1) {
                val turn = InstructionGenerator.turnAt(doc, route[i], route[i + 1], route[i + 2])
                if (turn != Turn.STRAIGHT) return i to turn
            } else {
                // Last segment leads to destination; report straight.
                return i to Turn.STRAIGHT
            }
        }
        return null
    }

    private fun evaluateSegmentProgress() {
        if (route.isEmpty() || currentIndex >= route.size - 1) return
        val totalSteps = stepsForSegment(currentIndex)

        val turnInfo = nextTurnAhead()
        if (turnInfo != null && turnInfo.second != Turn.STRAIGHT) {
            val remaining = remainingStepsToTurn(turnInfo.first)
            if (remaining in 1..10 && countdownDoneForSegment != turnInfo.first) {
                say(InstructionGenerator.countdown(turnInfo.second, remaining))
                countdownDoneForSegment = turnInfo.first
            }
        }

        if (stepsSinceNode > (totalSteps * 1.6).toInt() && softPromptDoneForSegment != currentIndex) {
            say(InstructionGenerator.KEEP_WALKING)
            softPromptDoneForSegment = currentIndex
        }
    }

    /** Steps still to walk before reaching the turn executed at the end of [turnSegment]. */
    private fun remainingStepsToTurn(turnSegment: Int): Int {
        if (turnSegment < currentIndex) return 0
        var s = 0
        for (k in currentIndex..turnSegment) {
            s += if (k == currentIndex) {
                stepsForSegment(k) - stepsSinceNode
            } else {
                stepsForSegment(k)
            }
        }
        return s.coerceAtLeast(0)
    }

    private fun stepsForSegment(segmentStartIndex: Int): Int {
        if (segmentStartIndex >= route.size - 1) return 0
        val meters = graph.metersBetween(route[segmentStartIndex], route[segmentStartIndex + 1])
        return GeoMath.stepsFor(meters, strideM)
    }

    private fun finishArrival() {
        val name = tracker.currentNodeId?.let { doc.nodeById[it]?.name }
            ?: destinationId?.let { doc.nodeById[it]?.name }
            ?: "your destination"
        say(InstructionGenerator.arrival(name))
        haptics.play(HapticPattern.ARRIVAL)
        arrived = true
        active = false
        onArrived()
    }

    private fun say(text: String) {
        lastInstruction = text
        speaker.speak(text)
        updateStatus(text)
    }

    private fun updateStatus(text: String) {
        onStatus(text)
    }

    private fun turnToHaptic(turn: Turn): HapticPattern = when (turn) {
        Turn.LEFT -> HapticPattern.LEFT
        Turn.RIGHT -> HapticPattern.RIGHT
        Turn.UTURN -> HapticPattern.OFF_ROUTE
        Turn.STAIRS, Turn.ELEVATOR -> HapticPattern.TRANSITION
        Turn.STRAIGHT -> HapticPattern.STRAIGHT
    }

    private fun List<String>.indexOfFirstFrom(item: String, fromIndex: Int): Int {
        for (i in fromIndex until size) {
            if (this[i] == item) return i
        }
        return -1
    }
}
