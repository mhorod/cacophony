package cacophony.automata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SimpleDFATest {
    @Test
    fun `all states includes every state used in constructor`() {
        val dfa =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(3, 'b') to 100,
                    Pair(0, 'a') to 1,
                ),
                mapOf(
                    -1 to true,
                    0 to true,
                ),
            )

        assertEquals(setOf(-1, 0, 1, 2, 3, 100), dfa.getAllStates().toSet())
    }

    @Test
    fun `starting state is unchanged`() {
        val dfa =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(3, 'b') to 100,
                    Pair(0, 'a') to 1,
                ),
                mapOf(
                    -1 to true,
                    0 to true,
                ),
            )

        assertEquals(1, dfa.getStartingState())
    }

    @Test
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
                mapOf(
                    -1 to true,
                    0 to true,
                ),
            )

        assertEquals(productions, dfa.getProductions())
    }

    @Test
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
                mapOf(
                    -1 to true,
                    0 to true,
                ),
            )

        assertEquals(2, dfa.getProduction(1, 'a'))
        assertEquals(100, dfa.getProduction(3, 'b'))
        assertEquals(1, dfa.getProduction(0, 'a'))
    }

    @Test
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
                mapOf(
                    -1 to true,
                    0 to true,
                ),
            )

        assertNull(dfa.getProduction(1, 'b'))
        assertNull(dfa.getProduction(1, 'x'))
        assertNull(dfa.getProduction(99, 'a'))
        assertNull(dfa.getProduction(-100, 'x'))
    }

    @Test
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
                mapOf(
                    -1 to true,
                    0 to true,
                ),
            )

        for (state in listOf(-1, 0)) {
            assertTrue(dfa.isAccepting(state))
        }

        for (state in listOf(1, 2, 3, 4, 100)) {
            assertFalse(dfa.isAccepting(state))
        }
    }

    @Test
    fun `isAccepting works with different ResultType`() {
        val dfa =
            SimpleDFA(
                0,
                mapOf(
                    Pair(0, 'a') to 1,
                    Pair(1, 'a') to 2,
                    Pair(2, 'a') to 3,
                ),
                mapOf(
                    1 to "Good",
                    2 to "Bad",
                ),
            )
        assertTrue(dfa.isAccepting(1))
        assertTrue(dfa.isAccepting(2))
        assertFalse(dfa.isAccepting(3))
    }

    @Test
    fun `result works with different ResultType`() {
        val dfa =
            SimpleDFA(
                0,
                mapOf(
                    Pair(0, 'a') to 1,
                    Pair(1, 'a') to 2,
                    Pair(2, 'a') to 3,
                ),
                mapOf(
                    1 to "Good",
                    2 to "Bad",
                ),
            )
        assertEquals(dfa.result(1), "Good")
        assertEquals(dfa.result(2), "Bad")
        assertEquals(dfa.result(3), null)
    }
}
