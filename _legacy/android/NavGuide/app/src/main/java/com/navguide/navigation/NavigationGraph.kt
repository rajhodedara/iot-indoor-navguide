package com.navguide.navigation

import java.util.PriorityQueue

data class Node(
    val id: String,
    val x: Double,
    val y: Double,
    val isAccessible: Boolean = true,
    val name: String? = null
)

data class Edge(
    val fromId: String,
    val toId: String,
    val distanceSteps: Int, // Distance measured in average steps
    val directionHeading: Float // Direction in degrees relative to the node
)

class NavigationGraph(
    val nodes: Map<String, Node>,
    val edges: List<Edge>
) {
    fun findShortestPath(startId: String, endId: String): List<Edge>? {
        if (!nodes.containsKey(startId) || !nodes.containsKey(endId)) return null

        val adjacencyList = mutableMapOf<String, MutableList<Edge>>()
        edges.forEach { edge ->
            adjacencyList.getOrPut(edge.fromId) { mutableListOf() }.add(edge)
            // Assuming bidirectional edges for simplicity
            adjacencyList.getOrPut(edge.toId) { mutableListOf() }.add(
                Edge(edge.toId, edge.fromId, edge.distanceSteps, (edge.directionHeading + 180) % 360)
            )
        }

        val distances = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val previous = mutableMapOf<String, Edge?>()
        val queue = PriorityQueue<Pair<String, Int>>(compareBy { it.second })

        distances[startId] = 0
        queue.add(startId to 0)

        while (queue.isNotEmpty()) {
            val (current, currentDist) = queue.poll()

            if (current == endId) break

            if (currentDist > distances.getValue(current)) continue

            adjacencyList[current]?.forEach { edge ->
                val neighbor = edge.toId
                if (!nodes[neighbor]!!.isAccessible) return@forEach

                val newDist = currentDist + edge.distanceSteps
                if (newDist < distances.getValue(neighbor)) {
                    distances[neighbor] = newDist
                    previous[neighbor] = edge
                    queue.add(neighbor to newDist)
                }
            }
        }

        if (!previous.containsKey(endId)) return null // No path found

        val path = mutableListOf<Edge>()
        var curr = endId
        while (curr != startId) {
            val edge = previous[curr] ?: break
            path.add(edge)
            curr = edge.fromId
        }

        return path.reversed()
    }
}
