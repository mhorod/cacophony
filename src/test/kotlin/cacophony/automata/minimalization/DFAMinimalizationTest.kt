package cacophony.automata.minimalization

import cacophony.automata.DFA
import cacophony.automata.areEquivalent
import cacophony.automata.createDFA
import cacophony.automata.createDFAEquivalenceHelper
import cacophony.automata.via
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class DFAMinimalizationTest {
    @Test
    fun `DFA accepting multiples of a is minimized to single state`() {
        val dfa =
            createDFA(
                0,
                setOf(0, 1, 2),
                mapOf(
                    0 via 'a' to 1,
                    1 via 'a' to 2,
                    2 via 'a' to 0,
                ),
            )
        val minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(1, minimized.getAllStates().size)
    }

    @Test
    fun `DFA accepting a single letter is minimized to two states`() {
        val dfa =
            createDFA(
                0,
                setOf(1),
                mapOf(
                    0 via 'a' to 1,
                    1 via 'b' to 2,
                    2 via 'a' to 3,
                    3 via 'a' to 2,
                ),
            )
        val minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(2, minimized.getAllStates().size)
    }

    @Test
    fun `DFA accepting a single 15-word is minimized to 16 states`() {
        val dfa =
            createDFA(
                0,
                setOf(15),
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
            )
        val minimized = dfa.minimalize()
        assertTrue(areEquivalent(dfa, minimized))
        assertEquals(16, minimized.getAllStates().size)
    }

    fun <E> checkThatMinimalizedDFAIsEquivalent(dfa: DFA<E>) {
        val minDfa = dfa.minimalize()
        val helper = createDFAEquivalenceHelper(dfa, minDfa)

        for (state in minDfa.getAllStates()) {
            for (oldState in state.originalStates) {
                assert(helper.areEquivalent(oldState, state))
            }
        }

        assert(helper.areEquivalent(dfa.getStartingState(), minDfa.getStartingState()))
    }

    private fun <E> bruteDFAStatesEquivalenceClasses(dfa: DFA<E>): Set<Set<E>> {
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

    private fun <E> checkThatMinimalizedDFAIsMinimal(dfa: DFA<E>) {
        val equivalenceClassesExpected = bruteDFAStatesEquivalenceClasses(dfa.withAliveReachableStates())
        val equivalenceClassesActual = dfa.minimalize().getAllStates().map { it.originalStates.toSet() }.toMutableSet()

        assertEquals(equivalenceClassesExpected, equivalenceClassesActual)
    }

    private fun <E> checkRandomDFA(dfa: DFA<E>) {
        if ((dfa.getAliveStates() intersect dfa.getReachableStates()).isEmpty()) {
            try {
                dfa.minimalize()
                TODO("Fix tests once DFA interface allows empty language")
            } catch (e: NotImplementedError) {
                return
            }
        }

        checkThatMinimalizedDFAIsEquivalent(dfa)
        checkThatMinimalizedDFAIsMinimal(dfa)
    }

    private fun generateRandomDFA(
        n: Int,
        seed: Int,
    ): DFA<Int> {
        val random = Random(seed)
        val symbols = "abc"
        val states = (1..n).toList()
        val accepting = states.map { random.nextDouble() < 0.3 }
        val start = states.random(random)
        val productions: MutableMap<Pair<Int, Char>, Int> = mutableMapOf()
        for (s in states) {
            for (c in symbols) {
                if (random.nextDouble() < 0.4) {
                    productions[Pair(s, c)] = states.random(random)
                }
            }
        }
        return object : DFA<Int> {
            override fun getStartingState(): Int {
                return start
            }

            override fun getAllStates(): List<Int> {
                return states
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
                return accepting[state - 1]
            }
        }
    }

    @Test
    fun `Random DFAs`() {
        (0..2000).forEach { checkRandomDFA(generateRandomDFA(1 + it % 50, it)) }
    }
}
