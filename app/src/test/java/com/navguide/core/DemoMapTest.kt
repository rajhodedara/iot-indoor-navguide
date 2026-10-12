package com.navguide.core

import com.navguide.routing.NavGraph
import com.navguide.routing.Router
import com.navguide.testutil.DemoMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoMapTest {

    private val doc = DemoMap.load()

    @Test
    fun `demo map is valid per its own validator`() {
        val problems = doc.validate()
        assertTrue("problems: $problems", problems.isEmpty())
    }

    @Test
    fun `schema basics`() {
        assertEquals(1, doc.schemaVersion)
        assertEquals(12, doc.nodes.size)
        assertEquals(11, doc.edges.size)
        assertEquals("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0", doc.uuid)
    }

    @Test
    fun `beacon minors are unique`() {
        val minors = doc.nodes.mapNotNull { it.beaconMinor }
        assertEquals(minors.size, minors.toSet().size)
    }

    @Test
    fun `every edge references existing nodes`() {
        doc.edges.forEach { e ->
            assertNotNull("missing from node ${e.from}", doc.nodeById[e.from])
            assertNotNull("missing to node ${e.to}", doc.nodeById[e.to])
            assertTrue(e.meters > 0)
        }
    }

    @Test
    fun `destinations include rooms and exits`() {
        val names = doc.destinationNodes.map { it.name }
        assertTrue(names.contains("Room 101"))
        assertTrue(names.contains("Exit East"))
        assertTrue(names.contains("Main Entrance"))
    }

    @Test
    fun `json round trip preserves structure`() {
        val reparsed = MapDoc.parse(doc.toJson())
        assertEquals(doc.nodes.size, reparsed.nodes.size)
        assertEquals(doc.edges.size, reparsed.edges.size)
        assertEquals(doc.nodes.first { it.id == "r102" }.beaconMinor, reparsed.nodes.first { it.id == "r102" }.beaconMinor)
        assertEquals(doc.edgeMeters("ja", "jb"), reparsed.edgeMeters("ja", "jb"))
    }

    @Test
    fun `a custom college-style map parses, validates and routes`() {
        // Proves the app is map-agnostic: any well-formed building map works, not just the demo.
        val json = """
        {
          "schemaVersion": 1,
          "name": "College Floor 1",
          "uuid": "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
          "floor": { "widthM": 100, "heightM": 60, "pxPerM": 10, "image": null },
          "nodes": [
            { "id": "c101", "x": 10, "y": 10, "name": "Class 101", "type": "room", "beaconMinor": 1 },
            { "id": "cor1", "x": 30, "y": 10, "name": "Corridor Junction", "type": "intersection", "beaconMinor": 2 },
            { "id": "c102", "x": 30, "y": 30, "name": "Class 102", "type": "room", "beaconMinor": 3 }
          ],
          "edges": [
            { "from": "c101", "to": "cor1", "m": 20 },
            { "from": "cor1", "to": "c102", "m": 20 }
          ]
        }
        """.trimIndent()
        val college = MapDoc.parse(json)
        assertTrue("problems: ${college.validate()}", college.validate().isEmpty())
        assertEquals(2, college.destinationNodes.size)
        assertEquals(
            listOf("c101", "cor1", "c102"),
            Router.findRoute(college, NavGraph(college), "c101", "c102")
        )
    }
}
