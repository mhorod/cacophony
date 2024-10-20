package cacophony.automata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

class SimpleDFATest {
    fun `all states includes every state used in constructor`() {
        val dfa =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(3, 'b') to 100,
                    Pair(0, 'a') to 1,
                ),
                setOf(-1, 0),
            )

        assertEquals(setOf(-1, 0, 1, 2, 3, 100), dfa.getAllStates())
    }

    fun `starting state is unchanged`() {
        val dfa =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(3, 'b') to 100,
                    Pair(0, 'a') to 1,
                ),
                setOf(-1, 0),
            )

        assertEquals(1, dfa.getStartingState())
    }

    fun `productions are unchanged`() {
        val productions =
            mapOf(
                Pair(1, 'a') to 2,
                Pair(3, 'b') to 100,
                Pair(0, 'a') to 1,
            )
        val dfa =
            SimpleDFA(
                1,
                productions,
                setOf(-1, 0),
            )

        assertEquals(productions, dfa.getProductions())
    }

    fun `singular production matches definition in productions map`() {
        val productions =
            mapOf(
                Pair(1, 'a') to 2,
                Pair(3, 'b') to 100,
                Pair(0, 'a') to 1,
            )
        val dfa =
            SimpleDFA(
                1,
                productions,
                setOf(-1, 0),
            )

        assertEquals(2, dfa.getProduction(1, 'a'))
        assertEquals(100, dfa.getProduction(3, 'b'))
        assertEquals(1, dfa.getProduction(0, 'a'))
    }

    fun `nonexistent production returns null`() {
        val productions =
            mapOf(
                Pair(1, 'a') to 2,
                Pair(3, 'b') to 100,
                Pair(0, 'a') to 1,
            )
        val dfa =
            SimpleDFA(
                1,
                productions,
                setOf(-1, 0),
            )

        assertNull(dfa.getProduction(1, 'b'))
        assertNull(dfa.getProduction(1, 'x'))
        assertNull(dfa.getProduction(99, 'a'))
        assertNull(dfa.getProduction(-100, 'x'))
    }

    fun `exactly the provided accepting states are accepting`() {
        val productions =
            mapOf(
                Pair(1, 'a') to 2,
                Pair(3, 'b') to 100,
                Pair(0, 'a') to 1,
            )
        val dfa =
            SimpleDFA(
                1,
                productions,
                setOf(-1, 0),
            )

        for (state in listOf(-1, 0)) {
            assertTrue(dfa.isAccepting(state))
        }

        for (state in listOf(1, 2, 3, 4, 100)) {
            assertFalse(dfa.isAccepting(state))
        }
    }
}
