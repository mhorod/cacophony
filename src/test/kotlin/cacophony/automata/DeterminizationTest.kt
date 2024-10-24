package cacophony.automata

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeterminizationTest {
    private fun makeNFA(
        starting: Int,
        productions: Map<Pair<Int, Char>, List<Int>>,
        epsilonProductions: Map<Int, List<Int>>,
        accepting: Int,
    ): NFA<Int, Char> =
        object : NFA<Int, Char> {
            private val allStates =
                (
                    setOf(starting, accepting) union
                        productions.keys
                            .unzip()
                            .first
                            .toSet() union productions.values.flatten().toSet() union epsilonProductions.keys union
                        epsilonProductions.values
                            .flatten()
                            .toSet()
                ).toList()

            override fun getStartingState() = starting

            override fun getProductions() = productions

            override fun getProductions(
                state: Int,
                symbol: Char,
            ) = productions[state to symbol] ?: emptyList()

            override fun getEpsilonProductions() = epsilonProductions

            override fun getAcceptingState() = accepting

            override fun isAccepting(state: Int) = state == accepting

            override fun getAllStates() = allStates
        }

    @Test
    fun `empty language NFA maps to empty language DFA`() {
        val nfa =
            makeNFA(
                1,
                mapOf(
                    Pair(1, 'a') to listOf(1, 3),
                    Pair(2, 'a') to listOf(2),
                    Pair(3, 'b') to listOf(3),
                ),
                mapOf(
                    3 to listOf(1),
                ),
                2,
            )

        val expected = SimpleDFA<Int, Char, Unit>(1, mapOf(), mapOf(2 to Unit))

        assertTrue(areEquivalent(determinize(nfa), expected))
    }

    @Test
    fun `empty word singleton NFA maps to empty word singleton DFA`() {
        val nfa =
            makeNFA(
                1,
                mapOf(
                    Pair(1, 'a') to listOf(3),
                    Pair(3, 'b') to listOf(3),
                ),
                mapOf(
                    1 to listOf(2, 3),
                ),
                2,
            )

        val expected = SimpleDFA<Int, Char, Unit>(1, mapOf(), mapOf(1 to Unit))
        val actual = determinize(nfa)
        assertTrue(areEquivalent(actual, expected), actual.toString())
    }

    // Language is (a|b)*
    @Test
    fun `deterministic NFA maps to equivalent DFA`() {
        val nfa =
            makeNFA(
                1,
                mapOf(
                    Pair(1, 'a') to listOf(1),
                    Pair(1, 'b') to listOf(1),
                ),
                mapOf(),
                1,
            )

        val expected = SimpleDFA(1, mapOf(Pair(1, 'a') to 1, Pair(1, 'b') to 1), mapOf(1 to Unit))
        val actual = determinize(nfa)
        assertTrue(areEquivalent(actual, expected), actual.toString())
    }

    // Language is (a|b)*
    @Test
    fun `highly nondeterministic NFA maps to equivalent DFA`() {
        val nfa =
            makeNFA(
                1,
                mapOf(
                    Pair(1, 'a') to listOf(1, 2, 3),
                    Pair(1, 'b') to listOf(1, 2, 3),
                    Pair(2, 'a') to listOf(1, 2, 3),
                    Pair(2, 'b') to listOf(1, 2, 3),
                    Pair(3, 'a') to listOf(1, 2, 3),
                    Pair(3, 'b') to listOf(1, 2, 3),
                    Pair(4, 'a') to listOf(4, 5),
                    Pair(4, 'b') to listOf(4, 5),
                    Pair(5, 'a') to listOf(4, 5),
                    Pair(5, 'b') to listOf(4, 5),
                ),
                mapOf(1 to listOf(2, 4), 2 to listOf(4), 3 to listOf(4)),
                2,
            )

        val expected = SimpleDFA(1, mapOf(Pair(1, 'a') to 1, Pair(1, 'b') to 1), mapOf(1 to Unit))
        val actual = determinize(nfa)
        assertTrue(areEquivalent(actual, expected), actual.toString())
    }

    // Language is (baaaa|bcba|bcaaa|caaa)*
    @Test
    fun `large NFA maps to equivalent DFA`() {
        val nfa =
            makeNFA(
                1,
                mapOf(
                    Pair(1, 'a') to listOf(-4),
                    Pair(1, 'b') to listOf(2, -2),
                    Pair(1, 'c') to listOf(3, -3, 100, -4),
                    Pair(2, 'a') to listOf(3, 100),
                    Pair(2, 'c') to listOf(7),
                    Pair(3, 'a') to listOf(4),
                    Pair(4, 'a') to listOf(5),
                    Pair(5, 'a') to listOf(0, 1),
                    Pair(6, 'a') to listOf(0),
                    Pair(7, 'a') to listOf(4, 101),
                    Pair(7, 'b') to listOf(8, 5),
                    Pair(8, 'a') to listOf(0),
                    Pair(100, 'a') to listOf(4, 101),
                    Pair(101, 'a') to listOf(5, 102),
                    Pair(102, 'a') to listOf(0, -3),
                    Pair(0, 'x') to listOf(-2),
                    Pair(-2, 'a') to listOf(-3, -4),
                    Pair(-2, 'b') to listOf(-3),
                    Pair(-2, 'c') to listOf(-3, -2),
                    Pair(-3, 'a') to listOf(-3, -4),
                    Pair(-3, 'b') to listOf(-3),
                    Pair(-3, 'c') to listOf(-3, -2),
                    Pair(-4, 'a') to listOf(-3, -4),
                    Pair(-4, 'b') to listOf(-3),
                    Pair(-4, 'c') to listOf(-3, -2),
                ),
                mapOf(0 to listOf(1, 0), 1 to listOf(-2, -3), 101 to listOf(-2, 4)),
                1,
            )

        val expected =
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
                    1 to Unit,
                ),
            )
        val actual = determinize(nfa)
        assertTrue(areEquivalent(actual, expected), actual.toString())
    }
}
