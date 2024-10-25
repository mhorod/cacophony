package cacophony.automata

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DFAEquivalenceTest {
    @Test
    fun `DFA is equivalent to itself`() {
        val dfa =
            SimpleDFA(
                42,
                mapOf(
                    Pair(42, 'b') to 43,
                    Pair(43, 'c') to 53,
                ),
                mapOf(
                    53 to true,
                ),
            )
        assertTrue(areEquivalent(dfa, dfa))
    }

    @Test
    fun `two DFAs for the same simple language and of the same size are equivalent`() {
        // These two DFAs accept all words that contain at least one 'b'
        // They are identical up to state renaming
        val first =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 2,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                ),
                mapOf(
                    2 to true,
                ),
            )
        val second =
            SimpleDFA(
                2,
                mapOf(
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 1,
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 1,
                ),
                mapOf(
                    1 to true,
                ),
            )
        assertTrue(areEquivalent(first, second))
    }

    @Test
    fun `two DFAs for different simple languages but of the same size are not equivalent`() {
        // This DFA accepts all words that contain at least one 'b'
        val first =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 2,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                ),
                mapOf(
                    2 to true,
                ),
            )
        // This DFA accepts all words that contain at least one 'a'
        val second =
            SimpleDFA(
                2,
                mapOf(
                    Pair(2, 'a') to 1,
                    Pair(2, 'b') to 2,
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 1,
                ),
                mapOf(
                    1 to true,
                ),
            )
        assertFalse(areEquivalent(first, second))
    }

    @Test
    fun `two DFAs for full language without epsilon are equivalent`() {
        // These two DFAs accept all nonempty words
        val first =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(1, 'b') to 2,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                ),
                mapOf(
                    2 to true,
                ),
            )
        val second =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(1, 'b') to 3,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                    Pair(3, 'a') to 3,
                    Pair(3, 'b') to 3,
                ),
                mapOf(
                    2 to true,
                    3 to true,
                ),
            )
        assertTrue(areEquivalent(first, second))
    }

    @Test
    fun `DFAs for the same simple language are equivalent when one of them has missing transitions`() {
        // These two DFAs accept all words that start with 'a'
        val first =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                ),
                mapOf(
                    2 to true,
                ),
            )
        val second =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(1, 'b') to 3,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                    Pair(3, 'a') to 3,
                    Pair(3, 'b') to 3,
                ),
                mapOf(
                    2 to true,
                ),
            )
        assertTrue(areEquivalent(first, second))
    }

    @Test
    fun `two DFAs for different simple languages are not equivalent when both have missing transitions`() {
        // This DFA accepts all words that start with 'a'
        val first =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                ),
                mapOf(
                    2 to true,
                ),
            )
        // This DFA accepts only nonempty sequences of 'a''s
        val second =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(1, 'b') to 3,
                    Pair(2, 'a') to 2,
                    Pair(3, 'a') to 3,
                    Pair(3, 'b') to 3,
                ),
                mapOf(
                    2 to true,
                ),
            )
        assertFalse(areEquivalent(first, second))
    }

    @Test
    fun `two DFAs for the same language are equivalent`() {
        // The two DFAs recognize the regex: (baaaa|bcba|bcaaa|caaa)*
        val large =
            SimpleDFA(
                2,
                (
                    mapOf(
                        Pair(1, 'a') to 4,
                        Pair(1, 'b') to 12,
                        Pair(1, 'c') to 5,
                        Pair(2, 'a') to 10,
                        Pair(2, 'b') to 1,
                        Pair(2, 'c') to 9,
                        Pair(3, 'a') to 7,
                        Pair(3, 'b') to 13,
                        Pair(3, 'c') to 13,
                        Pair(4, 'a') to 11,
                        Pair(4, 'b') to 14,
                        Pair(4, 'c') to 14,
                        Pair(5, 'a') to 3,
                        Pair(5, 'b') to 7,
                        Pair(5, 'c') to 13,
                        Pair(6, 'a') to 7,
                        Pair(6, 'b') to 15,
                        Pair(6, 'c') to 15,
                        Pair(7, 'a') to 2,
                        Pair(7, 'b') to 8,
                        Pair(7, 'c') to 8,
                        Pair(8, 'a') to 8,
                        Pair(8, 'b') to 8,
                        Pair(8, 'c') to 8,
                        Pair(9, 'a') to 6,
                        Pair(9, 'b') to 15,
                        Pair(9, 'c') to 15,
                    ) +
                        mapOf(
                            Pair(10, 'a') to 10,
                            Pair(10, 'b') to 10,
                            Pair(10, 'c') to 10,
                            Pair(11, 'a') to 7,
                            Pair(11, 'b') to 12,
                            Pair(11, 'c') to 12,
                            Pair(12, 'a') to 12,
                            Pair(12, 'b') to 12,
                            Pair(12, 'c') to 12,
                            Pair(13, 'a') to 13,
                            Pair(13, 'b') to 13,
                            Pair(13, 'c') to 13,
                            Pair(14, 'a') to 13,
                            Pair(14, 'b') to 13,
                            Pair(14, 'c') to 13,
                            Pair(15, 'a') to 13,
                            Pair(15, 'b') to 13,
                            Pair(15, 'c') to 13,
                        )
                ),
                mapOf(
                    2 to true,
                ),
            )
        val small =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'b') to 2,
                    Pair(1, 'c') to 4,
                    Pair(2, 'a') to 4,
                    Pair(2, 'c') to 3,
                    Pair(3, 'a') to 5,
                    Pair(3, 'b') to 6,
                    Pair(4, 'a') to 5,
                    Pair(5, 'a') to 6,
                    Pair(6, 'a') to 1,
                ),
                mapOf(
                    1 to true,
                ),
            )
        assertTrue(areEquivalent(large, small))
    }

    @Test
    fun `two DFAs for the same language are equivalent when one has multiple accepting states`() {
        val singleAccepting =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(1, 'b') to 3,
                    Pair(1, 'c') to 4,
                    Pair(1, 'd') to 5,
                    Pair(2, 'a') to 6,
                    Pair(3, 'a') to 6,
                    Pair(4, 'a') to 6,
                    Pair(5, 'a') to 6,
                ),
                mapOf(
                    6 to true,
                ),
            )
        val multipleAccepting =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(2, 'a') to 3,
                    Pair(1, 'b') to 4,
                    Pair(4, 'a') to 5,
                    Pair(1, 'c') to 6,
                    Pair(6, 'a') to 7,
                    Pair(1, 'd') to 8,
                    Pair(8, 'a') to 9,
                ),
                mapOf(
                    3 to true,
                    5 to true,
                    7 to true,
                    9 to true,
                ),
            )
        assertTrue(areEquivalent(singleAccepting, multipleAccepting))
    }

    @Test
    fun `two DFAs for the same language are equivalent when both have multiple accepting states`() {
        val first =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 3,
                    Pair(1, 'b') to 2,
                    Pair(2, 'a') to 3,
                    Pair(3, 'a') to 4,
                    Pair(3, 'b') to 5,
                ),
                mapOf(
                    3 to true,
                    4 to true,
                    5 to true,
                ),
            )
        val second =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(1, 'b') to 4,
                    Pair(2, 'a') to 3,
                    Pair(2, 'b') to 8,
                    Pair(4, 'a') to 5,
                    Pair(5, 'a') to 7,
                    Pair(5, 'b') to 6,
                ),
                mapOf(
                    2 to true,
                    3 to true,
                    5 to true,
                    6 to true,
                    7 to true,
                    8 to true,
                ),
            )
        assertTrue(areEquivalent(first, second))
    }

    @Test
    fun `DFA with no accepting states is equivalent to one with unreachable accepting states`() {
        val noAcceptingStates =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 1,
                ),
                mapOf<Int, Boolean>(),
            )
        val unreachableAcceptingStates =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 1,
                    Pair(2, 'a') to 1,
                    Pair(3, 'b') to 1,
                    Pair(4, 'a') to 2,
                    Pair(4, 'b') to 3,
                ),
                mapOf(
                    2 to true,
                    3 to true,
                    4 to true,
                ),
            )
        assertTrue(areEquivalent(noAcceptingStates, unreachableAcceptingStates))
    }

    @Test
    fun `DFA with no accepting states is not equivalent to one with reachable accepting states`() {
        val noAcceptingStates =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 1,
                ),
                mapOf<Int, Boolean>(),
            )
        val reachableAcceptingStates =
            SimpleDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 2,
                ),
                mapOf(
                    2 to true,
                ),
            )
        assertFalse(areEquivalent(noAcceptingStates, reachableAcceptingStates))
    }
}
