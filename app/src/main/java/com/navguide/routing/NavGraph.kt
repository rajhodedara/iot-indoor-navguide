package com.navguide.routing

import com.navguide.core.GeoMath
import com.navguide.core.MapDoc
import java.util.PriorityQueue

/** Undirected graph over map nodes. */
class NavGraph(private val doc: MapDoc) {

    private val adj = mutableMapOf<String, MutableList<String>>()

    init {
        doc.edges.forEach { e ->
            if (doc.nodeById.containsKey(e.from) && doc.nodeById.containsKey(e.to)) {
                adj.getOrPut(e.from) { mutableListOf() }.add(e.to)
                adj.getOrPut(e.to) { mutableListOf() }.add(e.from)
            }
        }
    }

    fun neighbors(id: String): List<String> = adj[id] ?: emptyList()

    /** Edge length in meters (falls back to straight-line distance). */
    fun metersBetween(a: String, b: String): Double {
        doc.edgeMeters(a, b)?.let { return it }
        val na = doc.nodeById[a] ?: return Double.MAX_VALUE
        val nb = doc.nodeById[b] ?: return Double.MAX_VALUE
        return GeoMath.distance(na.x, na.y, nb.x, nb.y)
    }
}

/** A* shortest path over [NavGraph]. Returns the list of node ids from start to dest, or null. */
object Router {

    fun findRoute(doc: MapDoc, graph: NavGraph, startId: String, destId: String): List<String>? {
        if (!doc.nodeById.containsKey(startId) || !doc.nodeById.containsKey(destId)) return null
        if (startId == destId) return listOf(startId)

        val heuristic = { id: String ->
            val n = doc.nodeById[id]
            val d = doc.nodeById[destId]
            if (n != null && d != null) GeoMath.distance(n.x, n.y, d.x, d.y) else 0.0
        }

        val gScore = mutableMapOf<String, Double>()
        val fScore = mutableMapOf<String, Double>()
        val cameFrom = mutableMapOf<String, String>()
        val open = PriorityQueue<Pair<String, Double>>(compareBy { it.second })

        gScore[startId] = 0.0
        fScore[startId] = heuristic(startId)
        open.add(startId to fScore.getValue(startId))

        val closed = mutableSetOf<String>()

        while (open.isNotEmpty()) {
            val current = open.poll()?.first ?: continue
            if (current == destId) {
                // Reconstruct.
                val path = mutableListOf(destId)
                var cur = destId
                while (cur != startId) {
                    cur = cameFrom[cur] ?: return null
                    path.add(cur)
                }
                return path.reversed()
            }
            if (!closed.add(current)) continue

            graph.neighbors(current).forEach { nb ->
                val tentative = gScore.getValue(current) + graph.metersBetween(current, nb)
                if (tentative < (gScore[nb] ?: Double.MAX_VALUE)) {
                    cameFrom[nb] = current
                    gScore[nb] = tentative
                    fScore[nb] = tentative + heuristic(nb)
                    open.add(nb to fScore.getValue(nb))
                }
            }
        }
        return null
    }
}
