package cacophony.automata.minimalization

import cacophony.automata.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class DFAMinimalizationTest {
    @Test
    fun `DFA with only accepting states is minimized to single state`() {
        val dfa = createDFA(
            0, setOf(0, 1, 2), mapOf(
                0 via 'a' to 1,
                1 via 'a' to 2,
                2 via 'a' to 0,
            )
        )
        val minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(1, minimized.getAllStates().size)
    }

    @Test
    fun `DFA accepting a single letter is minimized to two states`() {
        val dfa = createDFA(
            0, setOf(1), mapOf(
                0 via 'a' to 1,
                1 via 'b' to 2,
                2 via 'a' to 3,
                3 via 'a' to 2,
            )
        )
        val minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(2, minimized.getAllStates().size)
    }

    @Test
    fun `DFA accepting a single 15-word is minimized to 16 states `() {
        val dfa = createDFA(
            0, setOf(15), mapOf(
                0 via 'd' to 1,
                1 via 'e' to 2,
                2 via 'r' to 3,
                3 via 'm' to 4,
                4 via 'a' to 5,
                5 via 't' to 6,
                6 via 'o' to 7,
                7 via 'g' to 8,
                8 via 'l' to 9,
                9 via 'y' to 10,
                10 via 'p' to 11,
                11 via 'h' to 12,
                12 via 'i' to 13,
                13 via 'c' to 14,
                14 via 's' to 15,

                16 via 'x' to 4, // unreachable state
                7 via 'x' to 17, // dead state
            )
        )
        val minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(16, minimized.getAllStates().size)
    }
}