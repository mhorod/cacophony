package cacophony.automata

import cacophony.automata.minimization.via
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
                    42 via 'b' to 43,
                    43 via 'c' to 53,
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
                    1 via 'a' to 1,
                    1 via 'b' to 2,
                    2 via 'a' to 2,
                    2 via 'b' to 2,
                ),
                mapOf(
                    2 to true,
                ),
            )
        val second =
            SimpleDFA(
                2,
                mapOf(
                    2 via 'a' to 2,
                    2 via 'b' to 1,
                    1 via 'a' to 1,
                    1 via 'b' to 1,
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
                    1 via 'a' to 1,
                    1 via 'b' to 2,
                    2 via 'a' to 2,
                    2 via 'b' to 2,
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
                    2 via 'a' to 1,
                    2 via 'b' to 2,
                    1 via 'a' to 1,
                    1 via 'b' to 1,
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
                    1 via 'a' to 2,
                    1 via 'b' to 2,
                    2 via 'a' to 2,
                    2 via 'b' to 2,
                ),
                mapOf(
                    2 to true,
                ),
            )
        val second =
            SimpleDFA(
                1,
                mapOf(
                    1 via 'a' to 2,
                    1 via 'b' to 3,
                    2 via 'a' to 2,
                    2 via 'b' to 2,
                    3 via 'a' to 3,
                    3 via 'b' to 3,
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
                    1 via 'a' to 2,
                    2 via 'a' to 2,
                    2 via 'b' to 2,
                ),
                mapOf(
                    2 to true,
                ),
            )
        val second =
            SimpleDFA(
                1,
                mapOf(
                    1 via 'a' to 2,
                    1 via 'b' to 3,
                    2 via 'a' to 2,
                    2 via 'b' to 2,
                    3 via 'a' to 3,
                    3 via 'b' to 3,
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
                    1 via 'a' to 2,
                    2 via 'a' to 2,
                    2 via 'b' to 2,
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
                    1 via 'a' to 2,
                    1 via 'b' to 3,
                    2 via 'a' to 2,
                    3 via 'a' to 3,
                    3 via 'b' to 3,
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
                        1 via 'a' to 4,
                        1 via 'b' to 12,
                        1 via 'c' to 5,
                        2 via 'a' to 10,
                        2 via 'b' to 1,
                        2 via 'c' to 9,
                        3 via 'a' to 7,
                        3 via 'b' to 13,
                        3 via 'c' to 13,
                        4 via 'a' to 11,
                        4 via 'b' to 14,
                        4 via 'c' to 14,
                        5 via 'a' to 3,
                        5 via 'b' to 7,
                        5 via 'c' to 13,
                        6 via 'a' to 7,
                        6 via 'b' to 15,
                        6 via 'c' to 15,
                        7 via 'a' to 2,
                        7 via 'b' to 8,
                        7 via 'c' to 8,
                        8 via 'a' to 8,
                        8 via 'b' to 8,
                        8 via 'c' to 8,
                        9 via 'a' to 6,
                        9 via 'b' to 15,
                        9 via 'c' to 15,
                    ) +
                        mapOf(
                            10 via 'a' to 10,
                            10 via 'b' to 10,
                            10 via 'c' to 10,
                            11 via 'a' to 7,
                            11 via 'b' to 12,
                            11 via 'c' to 12,
                            12 via 'a' to 12,
                            12 via 'b' to 12,
                            12 via 'c' to 12,
                            13 via 'a' to 13,
                            13 via 'b' to 13,
                            13 via 'c' to 13,
                            14 via 'a' to 13,
                            14 via 'b' to 13,
                            14 via 'c' to 13,
                            15 via 'a' to 13,
                            15 via 'b' to 13,
                            15 via 'c' to 13,
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
                    1 via 'b' to 2,
                    1 via 'c' to 4,
                    2 via 'a' to 4,
                    2 via 'c' to 3,
                    3 via 'a' to 5,
                    3 via 'b' to 6,
                    4 via 'a' to 5,
                    5 via 'a' to 6,
                    6 via 'a' to 1,
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
                    1 via 'a' to 2,
                    1 via 'b' to 3,
                    1 via 'c' to 4,
                    1 via 'd' to 5,
                    2 via 'a' to 6,
                    3 via 'a' to 6,
                    4 via 'a' to 6,
                    5 via 'a' to 6,
                ),
                mapOf(
                    6 to true,
                ),
            )
        val multipleAccepting =
            SimpleDFA(
                1,
                mapOf(
                    1 via 'a' to 2,
                    2 via 'a' to 3,
                    1 via 'b' to 4,
                    4 via 'a' to 5,
                    1 via 'c' to 6,
                    6 via 'a' to 7,
                    1 via 'd' to 8,
                    8 via 'a' to 9,
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
                    1 via 'a' to 3,
                    1 via 'b' to 2,
                    2 via 'a' to 3,
                    3 via 'a' to 4,
                    3 via 'b' to 5,
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
                    1 via 'a' to 2,
                    1 via 'b' to 4,
                    2 via 'a' to 3,
                    2 via 'b' to 8,
                    4 via 'a' to 5,
                    5 via 'a' to 7,
                    5 via 'b' to 6,
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
                    1 via 'a' to 1,
                    1 via 'b' to 1,
                ),
                mapOf<Int, Boolean>(),
            )
        val unreachableAcceptingStates =
            SimpleDFA(
                1,
                mapOf(
                    1 via 'a' to 1,
                    1 via 'b' to 1,
                    2 via 'a' to 1,
                    3 via 'b' to 1,
                    4 via 'a' to 2,
                    4 via 'b' to 3,
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
                    1 via 'a' to 1,
                    1 via 'b' to 1,
                ),
                mapOf<Int, Boolean>(),
            )
        val reachableAcceptingStates =
            SimpleDFA(
                1,
                mapOf(
                    1 via 'a' to 1,
                    1 via 'b' to 2,
                ),
                mapOf(
                    2 to true,
                ),
            )
        assertFalse(areEquivalent(noAcceptingStates, reachableAcceptingStates))
    }
}
