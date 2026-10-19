package com.navguide.routing

import com.navguide.core.MapDoc
import com.navguide.testutil.DemoMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RouterTest {

    private val doc: MapDoc = DemoMap.load()

    @Test
    fun `shortest path main entrance to canteen`() {
        val route = Router.findRoute(doc, NavGraph(doc), "main_entry", "canteen")
        assertEquals(
            listOf("main_entry", "stair1", "corridor1", "stair2", "s28", "stair4", "exit_center", "s44", "canteen"),
            route
        )
    }

    @Test
    fun `shortest path works in reverse`() {
        val route = Router.findRoute(doc, NavGraph(doc), "canteen", "main_entry")
        assertEquals(
            listOf("canteen", "s44", "exit_center", "stair4", "s28", "stair2", "corridor1", "stair1", "main_entry"),
            route
        )
    }

    @Test
    fun `same start and destination is a trivial route`() {
        assertEquals(listOf("main_entry"), Router.findRoute(doc, NavGraph(doc), "main_entry", "main_entry"))
    }

    @Test
    fun `isolated section has no route`() {
        // Cut the link connecting the entrance; the entrance is then unreachable from the rest.
        val modified = doc.copy(edges = doc.edges.filterNot {
            (it.from == "main_entry" && it.to == "stair1") || (it.from == "stair1" && it.to == "main_entry")
        })
        assertNull(Router.findRoute(modified, NavGraph(modified), "main_entry", "canteen"))
    }

    @Test
    fun `unknown ids return null`() {
        assertNull(Router.findRoute(doc, NavGraph(doc), "nope", "stair1"))
        assertNull(Router.findRoute(doc, NavGraph(doc), "stair1", "nope"))
    }

    @Test
    fun `class to class route library to it department`() {
        // The core user story: a student walks from one location to another, via corridor junctions.
        val route = Router.findRoute(doc, NavGraph(doc), "library", "it_dept")
        assertEquals(listOf("library", "corridor1", "stair2", "s28", "it_dept"), route)
    }

    @Test
    fun `multi-floor route from ground floor to 8th floor drawing hall`() {
        val route = Router.findRoute(doc, NavGraph(doc), "main_entry", "drawing_hall_f8")
        assertNotNull(route)
        assertEquals("drawing_hall_f8", route!!.last())
    }
}
