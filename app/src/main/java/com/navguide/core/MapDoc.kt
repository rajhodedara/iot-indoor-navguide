package com.navguide.core

import org.json.JSONArray
import org.json.JSONObject

data class MapNode(
    val id: String,
    val x: Double,
    val y: Double,
    val name: String,
    val type: String,
    val beaconMinor: Int?,
    val floor: Int = 1
)

data class MapEdge(
    val from: String,
    val to: String,
    val meters: Double
)

/**
 * Schema v1 map document. Pure Kotlin (no android.* imports) so it is unit-testable on the JVM.
 */
data class MapDoc(
    val schemaVersion: Int,
    val name: String,
    val uuid: String,
    val floorWidthM: Double,
    val floorHeightM: Double,
    val pxPerM: Int,
    val nodes: List<MapNode>,
    val edges: List<MapEdge>
) {
    val nodeById: Map<String, MapNode> by lazy { nodes.associateBy { it.id } }
    val nodeByMinor: Map<Int, MapNode> by lazy {
        nodes.mapNotNull { n -> n.beaconMinor?.let { it to n } }.toMap()
    }
    val destinationNodes: List<MapNode>
        get() = nodes.filter {
            it.type == "room" || it.type == "entrance" || it.type == "stairs" ||
                it.type == "exit" || it.type == "elevator"
        }

    /** True when the map spans more than one floor (floors are labelled in UI lists). */
    val hasMultipleFloors: Boolean
        get() = nodes.map { it.floor }.distinct().size > 1

    /** Display label for UI lists: "F2 · Class 201" on multi-floor maps, plain name otherwise. */
    fun displayName(node: MapNode): String =
        if (hasMultipleFloors) "F${node.floor} · ${node.name}" else node.name

    /** Meters between two directly connected nodes, or null. */
    fun edgeMeters(a: String, b: String): Double? {
        edges.forEach { e ->
            if ((e.from == a && e.to == b) || (e.from == b && e.to == a)) return e.meters
        }
        return null
    }

    /** Returns a list of problems; empty means the map is valid. */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()
        if (schemaVersion != 1) problems.add("Unsupported schemaVersion: $schemaVersion")
        if (nodes.isEmpty()) problems.add("Map has no nodes")
        val ids = mutableSetOf<String>()
        val minors = mutableSetOf<Int>()
        nodes.forEach { n ->
            if (!ids.add(n.id)) problems.add("Duplicate node id: ${n.id}")
            if (n.name.isBlank()) problems.add("Node ${n.id} has a blank name")
            n.beaconMinor?.let { m -> if (!minors.add(m)) problems.add("Duplicate beaconMinor: $m") }
        }
        edges.forEach { e ->
            if (!ids.contains(e.from)) problems.add("Edge references missing node: ${e.from}")
            if (!ids.contains(e.to)) problems.add("Edge references missing node: ${e.to}")
            if (e.meters <= 0) problems.add("Edge ${e.from}-${e.to} has non-positive length")
        }
        // Connectivity (BFS over undirected edges, including implicit stairs/elevator links)
        if (ids.isNotEmpty()) {
            val adj = mutableMapOf<String, MutableList<String>>()
            edges.forEach { e ->
                if (ids.contains(e.from) && ids.contains(e.to)) {
                    adj.getOrPut(e.from) { mutableListOf() }.add(e.to)
                    adj.getOrPut(e.to) { mutableListOf() }.add(e.from)
                }
            }
            // Implicit links: same-name, same-type stairs/elevator nodes on different floors.
            val transitions = nodes.filter { it.type == "stairs" || it.type == "elevator" }
            for (a in transitions) {
                for (b in transitions) {
                    if (a === b || a.floor == b.floor || a.type != b.type) continue
                    if (!a.name.equals(b.name, ignoreCase = true)) continue
                    adj.getOrPut(a.id) { mutableListOf() }.add(b.id)
                    adj.getOrPut(b.id) { mutableListOf() }.add(a.id)
                }
            }
            val seen = mutableSetOf<String>()
            val queue = ArrayDeque<String>()
            queue.add(ids.first())
            seen.add(ids.first())
            while (queue.isNotEmpty()) {
                val cur = queue.removeFirst()
                adj[cur]?.forEach { nb -> if (seen.add(nb)) queue.add(nb) }
            }
            if (seen.size != ids.size) problems.add("Graph is not fully connected (${seen.size}/${ids.size} reachable)")
        }
        return problems
    }

    fun toJson(): String = toJsonObject().toString(2)

    private fun toJsonObject(): JSONObject {
        val o = JSONObject()
        o.put("schemaVersion", schemaVersion)
        o.put("name", name)
        o.put("uuid", uuid)
        o.put("floor", JSONObject().apply {
            put("widthM", floorWidthM)
            put("heightM", floorHeightM)
            put("pxPerM", pxPerM)
            put("image", JSONObject.NULL)
        })
        o.put("nodes", JSONArray().apply {
            nodes.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id)
                    put("x", n.x)
                    put("y", n.y)
                    put("name", n.name)
                    put("type", n.type)
                    put("floor", n.floor)
                    if (n.beaconMinor != null) put("beaconMinor", n.beaconMinor)
                })
            }
        })
        o.put("edges", JSONArray().apply {
            edges.forEach { e ->
                put(JSONObject().apply {
                    put("from", e.from)
                    put("to", e.to)
                    put("m", e.meters)
                })
            }
        })
        return o
    }

    companion object {
        fun parse(json: String): MapDoc = fromJsonObject(JSONObject(json))

        fun fromJsonObject(o: JSONObject): MapDoc {
            val floor = o.optJSONObject("floor") ?: JSONObject()
            val nodesArr = o.optJSONArray("nodes") ?: JSONArray()
            val edgesArr = o.optJSONArray("edges") ?: JSONArray()

            val nodes = (0 until nodesArr.length()).map { i ->
                val n = nodesArr.getJSONObject(i)
                MapNode(
                    id = n.getString("id"),
                    x = n.getDouble("x"),
                    y = n.getDouble("y"),
                    name = n.optString("name", n.getString("id")),
                    type = n.optString("type", "room"),
                    beaconMinor = if (n.has("beaconMinor") && !n.isNull("beaconMinor")) n.getInt("beaconMinor") else null,
                    floor = n.optInt("floor", 1)
                )
            }
            val edges = (0 until edgesArr.length()).map { i ->
                val e = edgesArr.getJSONObject(i)
                MapEdge(
                    from = e.getString("from"),
                    to = e.getString("to"),
                    meters = e.getDouble("m")
                )
            }
            return MapDoc(
                schemaVersion = o.optInt("schemaVersion", 1),
                name = o.optString("name", "Untitled"),
                uuid = o.optString("uuid", "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"),
                floorWidthM = floor.optDouble("widthM", 40.0),
                floorHeightM = floor.optDouble("heightM", 25.0),
                pxPerM = floor.optInt("pxPerM", 20),
                nodes = nodes,
                edges = edges
            )
        }
    }
}
