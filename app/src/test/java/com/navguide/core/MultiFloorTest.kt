package com.navguide.core

import com.navguide.routing.NavGraph
import com.navguide.routing.Router
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Multi-floor routing tests: nodes carry a `floor`, and stairs/elevator nodes on different
 * floors are linked automatically so class-to-class routes can cross floors.
 */
class MultiFloorTest {

    private val json = """
    {
      "schemaVersion": 1,
      "name": "Two-floor test",
      "uuid": "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
      "floor": { "widthM": 60, "heightM": 40, "pxPerM": 20, "image": null },
      "nodes": [
        { "id": "c101", "x": 10, "y": 10, "name": "Class 101", "type": "room", "beaconMinor": 1, "floor": 1 },
        { "id": "jA",   "x": 20, "y": 10, "name": "Junction A", "type": "intersection", "beaconMinor": 2, "floor": 1 },
        { "id": "stAir","x": 30, "y": 10, "name": "Staircase A", "type": "stairs", "beaconMinor": 3, "floor": 1 },
        { "id": "stAir2","x": 30, "y": 10, "name": "Staircase A", "type": "stairs", "beaconMinor": 4, "floor": 2 },
        { "id": "jB",   "x": 30, "y": 10, "name": "Junction B", "type": "intersection", "beaconMinor": 5, "floor": 2 },
        { "id": "c201", "x": 30, "y": 20, "name": "Class 201", "type": "room", "beaconMinor": 6, "floor": 2 }
      ],
      "edges": [
        { "from": "c101", "to": "jA",    "m": 10, "floor": 1 },
        { "from": "jA",   "to": "stAir", "m": 10, "floor": 1 },
        { "from": "stAir2","to": "jB",   "m": 10, "floor": 2 },
        { "from": "jB",   "to": "c201",  "m": 10, "floor": 2 }
      ]
    }
    """.trimIndent()

    private val doc = MapDoc.parse(json)

    @Test
    fun `schema keeps floor numbers`() {
        assertEquals(1, doc.nodeById["c101"]!!.floor)
        assertEquals(2, doc.nodeById["c201"]!!.floor)
        assertTrue(doc.hasMultipleFloors)
    }

    @Test
    fun `stairs on different floors are linked automatically`() {
        val graph = NavGraph(doc)
        assertTrue(graph.neighbors("stAir").contains("stAir2"))
    }

    @Test
    fun `class to class route crosses floors via the staircase`() {
        val route = Router.findRoute(doc, NavGraph(doc), "c101", "c201")
        assertEquals(listOf("c101", "jA", "stAir", "stAir2", "jB", "c201"), route)
    }

    @Test
    fun `validator accepts the two-floor map`() {
        assertTrue("problems: ${doc.validate()}", doc.validate().isEmpty())
    }

    @Test
    fun `display name includes floor on multi-floor maps`() {
        assertEquals("F2 · Class 201", doc.displayName(doc.nodeById["c201"]!!))
    }
}
