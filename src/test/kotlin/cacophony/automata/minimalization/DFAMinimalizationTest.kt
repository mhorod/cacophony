package cacophony.automata.minimalization

import cacophony.automata.DFA
import cacophony.automata.SimpleDFA
import cacophony.automata.areEquivalent
import cacophony.automata.createDFAEquivalenceHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.ceil
import kotlin.random.Random

class DFAMinimalizationTest {
    @Test
    fun `DFA accepting empty word and aa is not changed`() {
        val dfa =
            SimpleDFA(
                0,
                mapOf(
                    0 via 'a' to 1,
                    1 via 'a' to 2,
                ),
                mapOf(
                    0 to true,
                    2 to true,
                ),
            )
        val minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(3, minimized.getAllStates().size)
    }

    @Test
    fun `DFA accepting multiples of a is minimized to single state`() {
        val dfa =
            SimpleDFA(
                0,
                mapOf(
                    0 via 'a' to 1,
                    1 via 'a' to 2,
                    2 via 'a' to 0,
                ),
                mapOf(
                    0 to true,
                    1 to true,
                    2 to true,
                ),
            )
        val minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(1, minimized.getAllStates().size)
    }

    @Test
    fun `DFA with different result type is minimized properly`() {
        var dfa =
            SimpleDFA(
                0,
                mapOf(
                    0 via 'a' to 1,
                    0 via 'b' to 2,
                ),
                mapOf(
                    1 to "Good",
                    2 to "Good",
                ),
            )
        var minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(2, minimized.getAllStates().size)

        dfa =
            SimpleDFA(
                0,
                mapOf(
                    0 via 'a' to 1,
                    0 via 'b' to 2,
                ),
                mapOf(
                    1 to "Good",
                    2 to "NoGood",
                ),
            )
        minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(3, minimized.getAllStates().size)

        dfa =
            SimpleDFA(
                0,
                mapOf(
                    0 via 'a' to 1,
                    0 via 'b' to 2,
                    1 via 'a' to 3,
                    2 via 'a' to 4,
                ),
                mapOf(
                    3 to "Good",
                    4 to "Good",
                ),
            )
        minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(3, minimized.getAllStates().size)
    }

    @Test
    fun `DFA accepting a single letter is minimized to two states`() {
        val dfa =
            SimpleDFA(
                0,
                mapOf(
                    0 via 'a' to 1,
                    1 via 'b' to 2,
                    2 via 'a' to 3,
                    3 via 'a' to 2,
                ),
                mapOf(
                    1 to true,
                ),
            )
        val minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(2, minimized.getAllStates().size)
    }

    @Test
    fun `DFA accepting a single 15-word is minimized to 16 states`() {
        val dfa =
            SimpleDFA(
                0,
                mapOf(
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
                    // duplicated path
                    7 via 'g' to 16,
                    16 via 'l' to 17,
                    17 via 'y' to 18,
                    18 via 'p' to 19,
                    19 via 'h' to 12,
                    // unreachable state
                    20 via 'x' to 7,
                ),
                mapOf(
                    15 to true,
                ),
            )
        val minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(16, minimized.getAllStates().size)
    }

    private fun <E> checkThatMinimalizedDFAIsEquivalent(dfa: DFA<E, Char, Boolean>) {
        val minDfa = dfa.minimalize()
        val helper = createDFAEquivalenceHelper(dfa, minDfa)

        for (state in minDfa.getAllStates()) {
            for (oldState in state.originalStates) {
                assert(helper.areEquivalent(oldState, state))
            }
        }

        assert(helper.areEquivalent(dfa.getStartingState(), minDfa.getStartingState()))
    }

    private fun <E> bruteDFAStatesEquivalenceClasses(dfa: DFA<E, Char, Boolean>): Set<Set<E>> {
        val helper = createDFAEquivalenceHelper(dfa, dfa)
        val states = dfa.getAllStates()

        val equivalenceClassesMap: MutableMap<E, MutableSet<E>> = mutableMapOf()
        states.forEach {
            for (s in states) {
                if (helper.areEquivalent(s, it)) {
                    equivalenceClassesMap.getOrPut(s) { mutableSetOf() }.add(it)
                    return@forEach
                }
            }
            throw Exception("State is not equivalent to itself")
        }
        return equivalenceClassesMap.values.toSet()
    }

    private fun <E> checkThatMinimalizedDFAIsMinimal(dfa: DFA<E, Char, Boolean>) {
        val equivalenceClassesExpected = bruteDFAStatesEquivalenceClasses(dfa.withAliveReachableStates())
        val equivalenceClassesActual =
            dfa
                .minimalize()
                .getAllStates()
                .map { it.originalStates.toSet() }
                .toMutableSet()

        assertEquals(equivalenceClassesExpected, equivalenceClassesActual)
    }

    private fun <E> checkRandomDFA(dfa: DFA<E, Char, Boolean>) {
        if ((dfa.getAliveStates() intersect dfa.getReachableStates()).isEmpty()) {
            // Generated DFA is invalid.
            assertThrows<IllegalArgumentException> { dfa.minimalize() }
            return
        }

        checkThatMinimalizedDFAIsEquivalent(dfa)
        checkThatMinimalizedDFAIsMinimal(dfa)
    }

    private fun generateRandomDFA(
        n: Int,
        random: Random,
    ): DFA<Int, Char, Boolean> {
        val density = 0.2
        val symbols = "abc"
        val states = 1..n
        return SimpleDFA(
            states.random(random),
            (0..<ceil(density * n * symbols.length).toInt()).associate {
                states.random(random) via symbols.random(random) to states.random(random)
            },
            states.filter { random.nextDouble() < 0.2 }.associate { it to true },
        )
    }

    @Test
    fun `Random DFAs`() {
        (0..2000).forEach { checkRandomDFA(generateRandomDFA(1 + it % 50, Random(it))) }
    }
}
