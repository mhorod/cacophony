package cacophony.automata.minimalization

import cacophony.automata.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.ceil
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

    fun <E> checkMinimality(dfa: DFA<E>) {
        val helper = createHelper(dfa, dfa)

        val states = dfa.getAllStates()
        val equivClassesMap: MutableMap<E, MutableSet<E>> = mutableMapOf()

        states.forEach {
            for (s in states) {
                if (helper.areEquivalent(s, it)) {
                    equivClassesMap.getOrPut(s) { mutableSetOf() }.add(it)
                    return@forEach
                }
            }
            throw Exception()
        }

        val equivClassesBruted = equivClassesMap.values.toSet()

        val minDfa = dfa.minimalize()
        val equivalenceClasses = minDfa.getAllStates().map { it.originalStates.toSet() }.toMutableSet()

        val returnedByMinimalization = equivalenceClasses.flatten()
        assertEquals(returnedByMinimalization.toSet().size, returnedByMinimalization.size)

        val missingEquivalenceClass = dfa.getAllStates().minus(returnedByMinimalization.toSet())
        if (missingEquivalenceClass.isNotEmpty()) {
            equivalenceClasses.add(missingEquivalenceClass.toSet())
        }

        assertEquals(equivClassesBruted, equivalenceClasses)
    }

    private fun <E> check(dfa: DFA<E>) {
        if (dfa.isLanguageEmpty()) {
            assertThrows<IllegalArgumentException> {
                dfa.minimalize()
            }
        } else {
            val minDfa = dfa.minimalize()
            assertTrue(areEquivalent(dfa, minDfa))
            checkMinimality(dfa)
        }
    }

    private fun generateDFA(
        n: Int,
        random: Random,
    ): DFA<Int> {
        val density = 0.2
        val symbols = "abc"
        val states = 1..n
        return createDFA(
            states.random(random),
            states.filter { random.nextDouble() < 0.2 }.toSet(),
            (0..<ceil(density * n * symbols.length).toInt()).associate {
                states.random(random) via symbols.random(random) to states.random(random)
            },
        )
    }

    @Test
    fun `randomly check DFAs`() {
        (0..2000).forEach { check(generateDFA(1 + it % 50, Random(0))) }
    }
}
