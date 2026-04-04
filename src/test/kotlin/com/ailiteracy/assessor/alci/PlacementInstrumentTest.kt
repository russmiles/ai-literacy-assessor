/**
 * Tests for the PlacementInstrument scoring logic.
 *
 * The two tests here are structural contracts for the level computation: the
 * cluster with the highest average score must become the primary level, and
 * when two clusters tie, the lower level wins. These rules follow from the
 * framework's intent — a tie means the team has not yet consolidated the
 * higher level, so the conservative reading is the accurate one.
 *
 * We test the scoring logic directly rather than through a Spring context
 * because PlacementInstrument is a stateless calculator. Its @Component
 * annotation is irrelevant for unit testing — we just construct it.
 */
package com.ailiteracy.assessor.alci

import com.ailiteracy.assessor.domain.LiteracyLevel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class PlacementInstrumentTest {

    private val instrument = PlacementInstrument()

    @Test
    fun `highest scoring cluster determines primary level`() {
        // L2 (EXPERIMENTING) cluster has the highest average (4.5), so it wins
        val scores = mapOf(
            0 to listOf(1, 1),   // L0: avg 1.0
            1 to listOf(2, 2),   // L1: avg 2.0
            2 to listOf(4, 5),   // L2: avg 4.5 — highest
            3 to listOf(3, 3),   // L3: avg 3.0
            4 to listOf(2, 2)    // L4: avg 2.0
        )
        val result = instrument.computeLevel(scores)
        assertEquals(LiteracyLevel.EXPERIMENTING, result)
    }

    @Test
    fun `tie breaks to lower level`() {
        // L1 (AWARE) and L2 (EXPERIMENTING) tie at 3.0 — lower level wins
        val scores = mapOf(
            0 to listOf(1, 1),   // L0: avg 1.0
            1 to listOf(3, 3),   // L1: avg 3.0 — tied with L2
            2 to listOf(3, 3),   // L2: avg 3.0 — tied with L1
            3 to listOf(2, 2),   // L3: avg 2.0
            4 to listOf(1, 1)    // L4: avg 1.0
        )
        val result = instrument.computeLevel(scores)
        // Lower level (L1 = AWARE) wins the tie
        assertEquals(LiteracyLevel.AWARE, result)
    }
}
