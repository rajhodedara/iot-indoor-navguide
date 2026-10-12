package com.navguide.routing

import com.navguide.core.GeoMath
import com.navguide.core.MapDoc
import java.util.PriorityQueue

/**
 * Undirected graph over map nodes.
 *
 * Multi-floor support:
 *  - Stairs/elevator nodes with the SAME NAME on different floors are linked automatically
 *    (a "virtual" staircase link) unless an explicit edge already connects them.
 *  - Crossing floors costs extra (FLOOR_TRANSITION_PENALTY_M), which makes the router prefer
 *    routes with fewer floor changes ("sticky floors").
 */
class NavGraph(private val doc: MapDoc) {

    companion object {
        /** Extra equivalent-meters cost for one floor change (tunes how much the router avoids them). */
        const val FLOOR_TRANSITION_PENALTY_M = 25.0

        /** Physical equivalent length of one implicit staircase/elevator link. */
        const val IMPLICIT_STAIR_LINK_M = 15.0
    }

    private data class Link(val to: String, val baseMeters: Double)

    private val adj = mutableMapOf<String, MutableList<Link>>()

    init {
        doc.nodeById.keys.forEach { adj.getOrPut(it) { mutableListOf() } }

        doc.edges.forEach { e ->
            if (doc.nodeById.containsKey(e.from) && doc.nodeById.containsKey(e.to)) {
                addLink(e.from, e.to, e.meters)
                addLink(e.to, e.from, e.meters)
            }
        }

        // Implicit links: same-name, same-type stairs/elevator nodes on different floors.
        val transitions = doc.nodes.filter { it.type == "stairs" || it.type == "elevator" }
        for (i in transitions.indices) {
            for (j in i + 1 until transitions.size) {
                val a = transitions[i]
                val b = transitions[j]
                if (a.floor == b.floor) continue
                if (a.type != b.type) continue
                if (!a.name.equals(b.name, ignoreCase = true)) continue
                if (hasExplicitEdge(a.id, b.id)) continue
                addLink(a.id, b.id, IMPLICIT_STAIR_LINK_M)
                addLink(b.id, a.id, IMPLICIT_STAIR_LINK_M)
            }
        }
    }

    private fun addLink(from: String, to: String, meters: Double) {
        val list = adj.getOrPut(from) { mutableListOf() }
        if (list.none { it.to == to }) list.add(Link(to, meters))
    }

    private fun hasExplicitEdge(a: String, b: String): Boolean =
        doc.edges.any { (it.from == a && it.to == b) || (it.from == b && it.to == a) }

    fun neighbors(id: String): List<String> = adj[id]?.map { it.to } ?: emptyList()

    /** Physical distance in meters (explicit edge, implicit link, or straight-line fallback). */
    fun metersBetween(a: String, b: String): Double {
        adj[a]?.firstOrNull { it.to == b }?.let { return it.baseMeters }
        val na = doc.nodeById[a] ?: return Double.MAX_VALUE
        val nb = doc.nodeById[b] ?: return Double.MAX_VALUE
        return GeoMath.distance(na.x, na.y, nb.x, nb.y)
    }

    /** Meters plus a floor-transition penalty when the two nodes are on different floors. */
    fun costBetween(a: String, b: String): Double {
        val base = metersBetween(a, b)
        if (base == Double.MAX_VALUE) return base
        val fa = doc.nodeById[a]?.floor ?: 1
        val fb = doc.nodeById[b]?.floor ?: 1
        return if (fa != fb) base + FLOOR_TRANSITION_PENALTY_M else base
    }
}

/** A* shortest path over [NavGraph] (cost-aware for floor transitions). */
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
        val cameFrom = mutableMapOf<String, String>()
        val open = PriorityQueue<Pair<String, Double>>(compareBy { it.second })

        gScore[startId] = 0.0
        open.add(startId to heuristic(startId))

        val closed = mutableSetOf<String>()

        while (open.isNotEmpty()) {
            val current = open.poll()?.first ?: continue
            if (current == destId) {
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
                val tentative = gScore.getValue(current) + graph.costBetween(current, nb)
                if (tentative < (gScore[nb] ?: Double.MAX_VALUE)) {
                    cameFrom[nb] = current
                    gScore[nb] = tentative
                    open.add(nb to tentative + heuristic(nb))
                }
            }
        }
        return null
    }
}
