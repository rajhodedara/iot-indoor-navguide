package com.navguide.guidance

import com.navguide.testutil.DemoMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class RecordingSpeaker : Speaker {
    val spoken = mutableListOf<String>()
    override fun speak(text: String) { spoken.add(text) }
    override fun stop() { }
}

private class RecordingHaptics : Haptics {
    val patterns = mutableListOf<HapticPattern>()
    override fun play(pattern: HapticPattern) { patterns.add(pattern) }
}

class NavigationSessionTest {

    private val doc = DemoMap.load()

    private fun session(anchor: String? = null): Triple<NavigationSession, RecordingSpeaker, RecordingHaptics> {
        val speaker = RecordingSpeaker()
        val haptics = RecordingHaptics()
        val s = NavigationSession(doc, strideM = 0.7, speaker = speaker, haptics = haptics)
        if (anchor != null) s.onNodeHop(anchor)
        return Triple(s, speaker, haptics)
    }

    @Test
    fun `start without anchor waits for signal`() {
        val (s, speaker, _) = session()
        s.start("r102")
        assertTrue(speaker.spoken.isEmpty())
        assertTrue(s.active)
    }

    @Test
    fun `route start speaks steps to first turn`() {
        val (s, speaker, _) = session(anchor = "entrance")
        s.start("r102")
        val first = speaker.spoken.first()
        assertTrue("was: $first", first.contains("Navigation started"))
        // entrance -> ja -> jb: 12 + 12 steps straight, then LEFT at jb (north into Room 102).
        assertTrue("was: $first", first.contains("24"))
        assertTrue("was: $first", first.contains("left"))
    }

    @Test
    fun `arrival at destination is announced`() {
        val (s, speaker, haptics) = session(anchor = "entrance")
        s.start("r102")
        s.onNodeHop("r102")
        assertTrue(speaker.spoken.last().contains("You have arrived at Room 102"))
        assertTrue(haptics.patterns.contains(HapticPattern.ARRIVAL))
        assertTrue(s.arrived)
    }

    @Test
    fun `off route snaps trigger recalculation`() {
        val (s, speaker, _) = session(anchor = "entrance")
        s.start("exitE")
        speaker.spoken.clear()
        s.onNodeHop("lab1") // not on the entrance->exitE corridor route
        assertTrue(speaker.spoken.first().contains("off route", ignoreCase = true))
    }

    @Test
    fun `countdown fires near a turn`() {
        val (s, speaker, _) = session(anchor = "entrance")
        s.start("r102")
        speaker.spoken.clear()
        // 24 steps to the turn at jb; after 14 steps, 10 remain => countdown.
        repeat(14) { s.onStep() }
        assertTrue("spoken=${speaker.spoken}", speaker.spoken.any { it.contains("Turn left in 10 steps") })
    }

    @Test
    fun `turn command fires when arriving at the turn node`() {
        val (s, speaker, haptics) = session(anchor = "entrance")
        s.start("r102")
        speaker.spoken.clear()
        haptics.patterns.clear()
        s.onNodeHop("jb") // the left turn point into Room 102
        assertTrue("spoken=${speaker.spoken}", speaker.spoken.any { it.contains("Turn left") })
        assertTrue(haptics.patterns.contains(HapticPattern.LEFT))
    }

    @Test
    fun `repeat speaks the last instruction again`() {
        val (s, speaker, _) = session(anchor = "entrance")
        s.start("r102")
        val last = speaker.spoken.last()
        speaker.spoken.clear()
        s.repeatLastInstruction()
        assertEquals(last, speaker.spoken.last())
    }

    @Test
    fun `no route case is reported`() {
        val modifiedMap = doc.copy(nodes = doc.nodes.map { if (it.id == "exitE") it.copy(id = "exitE2") else it })
        // exercise via a doc whose destination id does not exist
        val speaker = RecordingSpeaker()
        val s2 = NavigationSession(modifiedMap, 0.7, speaker, RecordingHaptics())
        s2.onNodeHop("entrance")
        s2.start("exitE") // doesn't exist in modified map
        assertTrue(speaker.spoken.any { it.contains("no route", ignoreCase = true) })
    }

    @Test
    fun `class to class walk from room 101 to room 103`() {
        // The core user story end-to-end: start inside one class, arrive in another.
        val (s, speaker, _) = session(anchor = "r101")
        s.start("r103")
        val first = speaker.spoken.first()
        assertTrue("was: $first", first.contains("Navigation started"))
        assertTrue("was: $first", first.contains("left"))

        s.onNodeHop("ja")
        s.onNodeHop("jb")
        s.onNodeHop("jc")
        s.onNodeHop("r103")
        assertTrue("spoken=${speaker.spoken}", speaker.spoken.last().contains("You have arrived at Room 103"))
        assertTrue(s.arrived)
    }
}
