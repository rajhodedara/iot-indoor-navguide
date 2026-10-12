package com.navguide.core

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
}
