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

            override fun getProductions(state: Int, symbol: Char) = productions[state to symbol] ?: emptyList()

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
                    1 via 'a' to listOf(1, 3),
                    2 via 'a' to listOf(2),
                    3 via 'b' to listOf(3),
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
                    1 via 'a' to listOf(3),
                    3 via 'b' to listOf(3),
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
                    1 via 'a' to listOf(1),
                    1 via 'b' to listOf(1),
                ),
                mapOf(),
                1,
            )

        val expected = SimpleDFA(1, mapOf(1 via 'a' to 1, 1 via 'b' to 1), mapOf(1 to Unit))
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
                    1 via 'a' to listOf(1, 2, 3),
                    1 via 'b' to listOf(1, 2, 3),
                    2 via 'a' to listOf(1, 2, 3),
                    2 via 'b' to listOf(1, 2, 3),
                    3 via 'a' to listOf(1, 2, 3),
                    3 via 'b' to listOf(1, 2, 3),
                    4 via 'a' to listOf(4, 5),
                    4 via 'b' to listOf(4, 5),
                    5 via 'a' to listOf(4, 5),
                    5 via 'b' to listOf(4, 5),
                ),
                mapOf(1 to listOf(2, 4), 2 to listOf(4), 3 to listOf(4)),
                2,
            )

        val expected = SimpleDFA(1, mapOf(1 via 'a' to 1, 1 via 'b' to 1), mapOf(1 to Unit))
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
                    1 via 'a' to listOf(-4),
                    1 via 'b' to listOf(2, -2),
                    1 via 'c' to listOf(3, -3, 100, -4),
                    2 via 'a' to listOf(3, 100),
                    2 via 'c' to listOf(7),
                    3 via 'a' to listOf(4),
                    4 via 'a' to listOf(5),
                    5 via 'a' to listOf(0, 1),
                    6 via 'a' to listOf(0),
                    7 via 'a' to listOf(4, 101),
                    7 via 'b' to listOf(8, 5),
                    8 via 'a' to listOf(0),
                    100 via 'a' to listOf(4, 101),
                    101 via 'a' to listOf(5, 102),
                    102 via 'a' to listOf(0, -3),
                    0 via 'x' to listOf(-2),
                    -2 via 'a' to listOf(-3, -4),
                    -2 via 'b' to listOf(-3),
                    -2 via 'c' to listOf(-3, -2),
                    -3 via 'a' to listOf(-3, -4),
                    -3 via 'b' to listOf(-3),
                    -3 via 'c' to listOf(-3, -2),
                    -4 via 'a' to listOf(-3, -4),
                    -4 via 'b' to listOf(-3),
                    -4 via 'c' to listOf(-3, -2),
                ),
                mapOf(0 to listOf(1, 0), 1 to listOf(-2, -3), 101 to listOf(-2, 4)),
                1,
            )

        val expected =
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
                    1 to Unit,
                ),
            )
        val actual = determinize(nfa)
        assertTrue(areEquivalent(actual, expected), actual.toString())
    }
}
