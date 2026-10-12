package com.navguide.routing

import com.navguide.core.MapDoc
import com.navguide.testutil.DemoMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouterTest {

    private val doc: MapDoc = DemoMap.load()

    @Test
    fun `shortest path entrance to room 102`() {
        val route = Router.findRoute(doc, NavGraph(doc), "entrance", "r102")
        assertEquals(listOf("entrance", "ja", "jb", "r102"), route)
    }

    @Test
    fun `shortest path works in reverse`() {
        val route = Router.findRoute(doc, NavGraph(doc), "r102", "entrance")
        assertEquals(listOf("r102", "jb", "ja", "entrance"), route)
    }

    @Test
    fun `same start and destination is a trivial route`() {
        assertEquals(listOf("entrance"), Router.findRoute(doc, NavGraph(doc), "entrance", "entrance"))
    }

    @Test
    fun `isolated node has no route`() {
        val modified = doc.copy(edges = doc.edges.filterNot { it.from == "ja" && it.to == "lab1" })
        assertNull(Router.findRoute(modified, NavGraph(modified), "ja", "lab1"))
    }

    @Test
    fun `unknown ids return null`() {
        assertNull(Router.findRoute(doc, NavGraph(doc), "nope", "ja"))
        assertNull(Router.findRoute(doc, NavGraph(doc), "ja", "nope"))
    }

    @Test
    fun `class to class route room 101 to room 103`() {
        // The core user story: a student walks from one classroom to another.
        val route = Router.findRoute(doc, NavGraph(doc), "r101", "r103")
        assertEquals(listOf("r101", "ja", "jb", "jc", "r103"), route)
    }
}
