package com.navguide.core

import com.navguide.routing.NavGraph
import com.navguide.routing.Router
import com.navguide.testutil.DemoMap
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ground-floor map derived from the KJSIT Sion emergency escape plan photo must parse,
 * validate, and be routable (e.g. Library -> Canteen, Main Entry -> Admin Office).
 */
class KjsitGroundTest {

    private val doc = MapDoc.parse(DemoMap.readAsset("kjsit_ground.json"))

    @Test
    fun `ground floor map is valid`() {
        val problems = doc.validate()
        assertTrue("problems: $problems", problems.isEmpty())
    }

    @Test
    fun `expected rooms and stairs exist`() {
        val names = doc.nodes.map { it.name }
        listOf(
            "Main Entry Exit", "Library", "Seminar Hall", "IT Department", "E&TC Department",
            "Canteen", "Reception", "Principal Cabin", "Staircase 1", "Staircase 2", "Staircase 4"
        ).forEach { assertTrue("missing: $it", names.contains(it)) }
    }

    @Test
    fun `library to canteen is routable`() {
        val route = Router.findRoute(doc, NavGraph(doc), "library", "canteen")
        assertNotNull(route)
        assertTrue(route!!.first() == "library")
        assertTrue(route.last() == "canteen")
    }

    @Test
    fun `main entry to admin office is routable`() {
        assertNotNull(Router.findRoute(doc, NavGraph(doc), "main_entry", "admin"))
    }

    @Test
    fun `every staircase is on the network`() {
        val graph = NavGraph(doc)
        doc.nodes.filter { it.type == "stairs" }.forEach { stair ->
            assertTrue("stair ${stair.name} isolated", graph.neighbors(stair.id).isNotEmpty())
        }
    }

    @Test
    fun `all exits are reachable from the main entrance`() {
        val graph = NavGraph(doc)
        doc.nodes.filter { it.type == "exit" }.forEach { exit ->
            assertNotNull("exit ${exit.name} unreachable", Router.findRoute(doc, graph, "main_entry", exit.id))
        }
    }
}
