package cacophony.automata

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DFAEquivalenceTest {
    private fun createDFA(
        starting: Int,
        productions: Map<Pair<Int, Char>, Int>,
        accepting: Set<Int>,
    ): DFA<Int> {
        val allStates = (setOf(starting) union accepting union productions.keys.map { it.first }.toSet() union productions.values).toList()
        return object : DFA<Int> {
            override fun getStartingState(): Int {
                return starting
            }

            override fun getAllStates(): List<Int> {
                return allStates
            }

            override fun getProductions(): Map<Pair<Int, Char>, Int> {
                return productions
            }

            override fun getProduction(
                state: Int,
                symbol: Char,
            ): Int? {
                return productions[Pair(state, symbol)]
            }

            override fun isAccepting(state: Int): Boolean {
                return accepting.contains(state)
            }
        }
    }

    @Test
    fun testDFAIsEquivalentToItself() {
        val dfa =
            createDFA(
                42,
                mapOf(
                    Pair(42, 'b') to 43,
                    Pair(43, 'c') to 53,
                ),
                setOf(53),
            )
        assertTrue(areEquivalent(dfa, dfa))
    }

    @Test
    fun testTwoDFAsForTheSameSimpleLanguageAndOfTheSameSizeAreEquivalent() {
        // These two DFAs accept all words that contain at least one 'b'
        // They are identical up to state renaming
        val first =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 2,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                ),
                setOf(2),
            )
        val second =
            createDFA(
                2,
                mapOf(
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 1,
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 1,
                ),
                setOf(1),
            )
        assertTrue(areEquivalent(first, second))
    }

    @Test
    fun testTwoDFAsForDifferentSimpleLanguagesButOfTheSameSizeAreNotEquivalent() {
        // This DFA accepts all words that contain at least one 'b'
        val first =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 2,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                ),
                setOf(2),
            )
        // This DFA accepts all words that contain at least one 'a'
        val second =
            createDFA(
                2,
                mapOf(
                    Pair(2, 'a') to 1,
                    Pair(2, 'b') to 2,
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 1,
                ),
                setOf(1),
            )
        assertFalse(areEquivalent(first, second))
    }

    @Test
    fun testTwoDFAsForFullLanguageWithoutEpsilonAreEquivalent() {
        // These two DFAs accept all nonempty words
        val first =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(1, 'b') to 2,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                ),
                setOf(2),
            )
        val second =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(1, 'b') to 3,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                    Pair(3, 'a') to 3,
                    Pair(3, 'b') to 3,
                ),
                setOf(2, 3),
            )
        assertTrue(areEquivalent(first, second))
    }

    @Test
    fun testDFAsForTheSameSimpleLanguageAreEquivalentWhenOneOfThemHasMissingTransitions() {
        // These two DFAs accept all words that start with 'a'
        val first =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                ),
                setOf(2),
            )
        val second =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(1, 'b') to 3,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                    Pair(3, 'a') to 3,
                    Pair(3, 'b') to 3,
                ),
                setOf(2),
            )
        assertTrue(areEquivalent(first, second))
    }

    @Test
    fun testTwoDFAsForDifferentSimpleLanguagesAreNotEquivalentWhenBothHaveMissingTransitions() {
        // This DFA accepts all words that start with 'a'
        val first =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(2, 'a') to 2,
                    Pair(2, 'b') to 2,
                ),
                setOf(2),
            )
        // This DFA accepts only nonempty sequences of 'a''s
        val second =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 2,
                    Pair(1, 'b') to 3,
                    Pair(2, 'a') to 2,
                    Pair(3, 'a') to 3,
                    Pair(3, 'b') to 3,
                ),
                setOf(2),
            )
        assertFalse(areEquivalent(first, second))
    }

    @Test
    fun testTwoDFAsForTheSameLanguageAreEquivalent() {
        // The two DFAs recognize the regex: (baaaa|bcba|bcaaa|caaa)*
        val large =
            createDFA(
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
                setOf(2),
            )
        val small =
            createDFA(
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
                setOf(1),
            )
        assertTrue(areEquivalent(large, small))
    }

    @Test
    fun testTwoDFAsForTheSameLanguageAreEquivalentWhenOneHasMultipleAcceptingStates() {
        val singleAccepting =
            createDFA(
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
                setOf(6),
            )
        val multipleAccepting =
            createDFA(
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
                setOf(3, 5, 7, 9),
            )
        assertTrue(areEquivalent(singleAccepting, multipleAccepting))
    }

    @Test
    fun testTwoDFAsForTheSameLanguageAreEquivalentWhenBothHaveMultipleAcceptingStates() {
        val first =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 3,
                    Pair(1, 'b') to 2,
                    Pair(2, 'a') to 3,
                    Pair(3, 'a') to 4,
                    Pair(3, 'b') to 5,
                ),
                setOf(3, 4, 5),
            )
        val second =
            createDFA(
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
                setOf(2, 3, 5, 6, 7, 8),
            )
        assertTrue(areEquivalent(first, second))
    }

    @Test
    fun testDFAWithNoAcceptingStatesIsEquivalentToOneWithUnreachableAcceptingStates() {
        val noAcceptingStates =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 1,
                ),
                setOf(),
            )
        val unreachableAcceptingStates =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 1,
                    Pair(2, 'a') to 1,
                    Pair(3, 'b') to 1,
                    Pair(4, 'a') to 2,
                    Pair(4, 'b') to 3,
                ),
                setOf(),
            )
        assertTrue(areEquivalent(noAcceptingStates, unreachableAcceptingStates))
    }

    @Test
    fun testDFAWithNoAcceptingStateIsNotEquivalentToOneWithReachableAcceptingStates() {
        val noAcceptingStates =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 1,
                ),
                setOf(),
            )
        val reachableAcceptingStates =
            createDFA(
                1,
                mapOf(
                    Pair(1, 'a') to 1,
                    Pair(1, 'b') to 2,
                ),
                setOf(2),
            )
        assertFalse(areEquivalent(noAcceptingStates, reachableAcceptingStates))
    }
}
