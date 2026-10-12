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
        s.start("canteen")
        assertTrue(speaker.spoken.isEmpty())
        assertTrue(s.active)
    }

    @Test
    fun `route start speaks steps to first turn`() {
        val (s, speaker, _) = session(anchor = "main_entry")
        s.start("canteen")
        val first = speaker.spoken.first()
        assertTrue("was: $first", first.contains("Navigation started"))
        // Route: main_entry -> stair1 -> corridor1 -> library?? no: toward canteen via corridor.
        // First turn on the way is at corridor1 (north/south rooms aside; corridor heads east).
        assertTrue("was: $first", first.contains("steps"))
    }

    @Test
    fun `arrival at destination is announced`() {
        val (s, speaker, haptics) = session(anchor = "main_entry")
        s.start("canteen")
        s.onNodeHop("canteen")
        assertTrue(speaker.spoken.last().contains("You have arrived at Canteen"))
        assertTrue(haptics.patterns.contains(HapticPattern.ARRIVAL))
        assertTrue(s.arrived)
    }

    @Test
    fun `off route snaps trigger recalculation`() {
        val (s, speaker, _) = session(anchor = "main_entry")
        s.start("exit_east")
        speaker.spoken.clear()
        s.onNodeHop("library") // not on the main_entry->exit_east corridor route
        assertTrue(speaker.spoken.first().contains("off route", ignoreCase = true))
    }

    @Test
    fun `countdown fires near the first turn`() {
        val (s, speaker, _) = session(anchor = "library")
        s.start("entc_dept")
        speaker.spoken.clear()
        // Route: library -> corridor1 -> stair2 -> s28 -> entc_dept.
        // First turn is LEFT at corridor1 (12 steps of the first segment are walked toward it).
        var guard = 0
        while (guard < 200 && speaker.spoken.none { it.contains("in 10 steps") }) { s.onStep(); guard++ }
        assertTrue("spoken=${speaker.spoken}", speaker.spoken.any { it.contains("Turn left in 10 steps") })
    }

    @Test
    fun `turn command fires when arriving at the turn node`() {
        val (s, speaker, haptics) = session(anchor = "library")
        s.start("entc_dept")
        speaker.spoken.clear()
        haptics.patterns.clear()
        s.onNodeHop("corridor1")
        s.onNodeHop("stair2")
        s.onNodeHop("s28") // the right-turn point into E&TC
        assertTrue("spoken=${speaker.spoken}", speaker.spoken.any { it.contains("Turn right") })
        assertTrue(haptics.patterns.contains(HapticPattern.RIGHT))
    }

    @Test
    fun `repeat speaks the last instruction again`() {
        val (s, speaker, _) = session(anchor = "main_entry")
        s.start("canteen")
        val last = speaker.spoken.last()
        speaker.spoken.clear()
        s.repeatLastInstruction()
        assertEquals(last, speaker.spoken.last())
    }

    @Test
    fun `no route case is reported`() {
        val speaker = RecordingSpeaker()
        val s2 = NavigationSession(doc, 0.7, speaker, RecordingHaptics())
        s2.onNodeHop("main_entry")
        s2.start("does_not_exist")
        assertTrue(speaker.spoken.any { it.contains("no route", ignoreCase = true) })
    }

    @Test
    fun `class to class walk from library to it department`() {
        // End-to-end: start inside the Library, arrive at the IT Department.
        val (s, speaker, _) = session(anchor = "library")
        s.start("it_dept")
        val first = speaker.spoken.first()
        assertTrue("was: $first", first.contains("Navigation started"))
        s.onNodeHop("corridor1")
        s.onNodeHop("stair2")
        s.onNodeHop("s28")
        s.onNodeHop("it_dept")
        assertTrue("spoken=${speaker.spoken}", speaker.spoken.last().contains("You have arrived at IT Department"))
        assertTrue(s.arrived)
    }

    @Test
    fun `steps without position give feedback`() {
        val statuses = mutableListOf<String>()
        val s = NavigationSession(doc, 0.7, RecordingSpeaker(), RecordingHaptics(), onStatus = { statuses.add(it) })
        s.start("canteen")
        statuses.clear()
        s.onStep()
        s.onStep()
        s.onStep()
        assertTrue("statuses=$statuses", statuses.any { it.contains("Steps so far: 3") })
    }

    @Test
    fun `auto tour starts from the entrance and walks the full route`() {
        val (s, speaker, haptics) = session() // no anchor set
        s.start("stair4")
        assertTrue(s.startAutoTour())
        var guard = 0
        while (s.active && guard < 600) { s.demoTick(); guard++ }
        assertTrue("active=${s.active} guard=$guard", s.arrived)
        assertTrue("spoken=${speaker.spoken}", speaker.spoken.any { it.contains("You have arrived at Staircase 4") })
        assertTrue(haptics.patterns.contains(HapticPattern.ARRIVAL))
    }

    @Test
    fun `demo tick does nothing when route not started`() {
        val (s, speaker, _) = session()
        s.start("canteen")
        speaker.spoken.clear()
        s.demoTick() // no anchor -> no route yet -> no crash, no instructions
        assertTrue(speaker.spoken.none { it.contains("Navigation started") })
    }
}
