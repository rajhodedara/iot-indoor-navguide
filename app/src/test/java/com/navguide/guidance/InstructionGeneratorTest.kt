package com.navguide.guidance

import com.navguide.testutil.DemoMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructionGeneratorTest {

    private val doc = DemoMap.load()

    @Test
    fun `turning north to the library from eastbound corridor is a left turn`() {
        // stair1 -> corridor1 (east) then north up to library => left.
        val turn = InstructionGenerator.turnAt(doc, "stair1", "corridor1", "library")
        assertEquals(Turn.LEFT, turn)
    }

    @Test
    fun `turning south into E&TC from eastbound corridor is a right turn`() {
        // stair2 -> s28 (east) then south into E&TC => right.
        val turn = InstructionGenerator.turnAt(doc, "stair2", "s28", "entc_dept")
        assertEquals(Turn.RIGHT, turn)
    }

    @Test
    fun `straight through a corridor junction stays straight`() {
        val turn = InstructionGenerator.turnAt(doc, "corridor1", "stair2", "s28")
        assertEquals(Turn.STRAIGHT, turn)
    }

    @Test
    fun `reverse direction is a u-turn`() {
        val turn = InstructionGenerator.turnAt(doc, "stair2", "corridor1", "stair2")
        assertEquals(Turn.UTURN, turn)
    }

    @Test
    fun `phrases contain key words`() {
        assertTrue(InstructionGenerator.countdown(Turn.LEFT, 10).contains("left"))
        assertTrue(InstructionGenerator.countdown(Turn.LEFT, 10).contains("10"))
        assertTrue(InstructionGenerator.turnCommand(Turn.RIGHT)!!.contains("right"))
        assertEquals(null, InstructionGenerator.turnCommand(Turn.STRAIGHT))
        assertTrue(InstructionGenerator.arrival("Library").contains("Library"))
        assertTrue(InstructionGenerator.routeStart(Turn.LEFT, 15, "Library").contains("15"))
        assertTrue(InstructionGenerator.routeStart(Turn.LEFT, 15, "Library").contains("left"))
    }
}
