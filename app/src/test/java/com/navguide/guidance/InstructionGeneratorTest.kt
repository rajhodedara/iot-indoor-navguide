package com.navguide.guidance

import com.navguide.testutil.DemoMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructionGeneratorTest {

    private val doc = DemoMap.load()

    @Test
    fun `turning north from eastbound corridor is a left turn`() {
        // ja -> r101: walking east (0), then north (270) => left.
        val turn = InstructionGenerator.turnAt(doc, "entrance", "ja", "r101")
        assertEquals(Turn.LEFT, turn)
    }

    @Test
    fun `turning south into lab from eastbound corridor is a right turn`() {
        // ja -> lab1: east (0) then south (90) => right.
        val turn = InstructionGenerator.turnAt(doc, "entrance", "ja", "lab1")
        assertEquals(Turn.RIGHT, turn)
    }

    @Test
    fun `straight through a junction stays straight`() {
        val turn = InstructionGenerator.turnAt(doc, "ja", "jb", "jc")
        assertEquals(Turn.STRAIGHT, turn)
    }

    @Test
    fun `reverse direction is a u-turn`() {
        val turn = InstructionGenerator.turnAt(doc, "entrance", "ja", "entrance")
        assertEquals(Turn.UTURN, turn)
    }

    @Test
    fun `phrases contain key words`() {
        assertTrue(InstructionGenerator.countdown(Turn.LEFT, 10).contains("left"))
        assertTrue(InstructionGenerator.countdown(Turn.LEFT, 10).contains("10"))
        assertTrue(InstructionGenerator.turnCommand(Turn.RIGHT)!!.contains("right"))
        assertEquals(null, InstructionGenerator.turnCommand(Turn.STRAIGHT))
        assertTrue(InstructionGenerator.arrival("Room 102").contains("Room 102"))
        assertTrue(InstructionGenerator.routeStart(Turn.LEFT, 24, "Room 102").contains("24"))
        assertTrue(InstructionGenerator.routeStart(Turn.LEFT, 24, "Room 102").contains("left"))
    }
}
