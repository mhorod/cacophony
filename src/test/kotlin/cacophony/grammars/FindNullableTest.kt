package cacophony.grammars

import cacophony.automata.SimpleDFA
import cacophony.automata.minimalization.via
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FindNullableTest {
    private fun <T> results(vararg results: T) = results.associateWith { }

    private fun <StateType, AtomType> productions() = mapOf<Pair<StateType, AtomType>, StateType>()

    @Test
    fun `findNullable finds accepting states`() {
        // given
        val dfa = SimpleDFA(0, mapOf(0 via 'a' to 1, 1 via 'b' to 2), results(0, 2))
        val automata = mapOf('A' to dfa)

        // when
        val nullable = findNullable(automata)

        // then
        assertThat(nullable).containsExactlyInAnyOrder(
            Pair(0, dfa),
            Pair(2, dfa),
        )
    }

    @Test
    fun `findNullable finds nullable state for empty word automaton`() {
        // given
        val dfaA = SimpleDFA(0, productions<Int, Char>(), results(0))
        val automata = mapOf('A' to dfaA)

        // when
        val nullable = findNullable(automata)

        // then
        assertThat(nullable).containsExactlyInAnyOrder(
            Pair(0, dfaA), // accepting state
        )
    }

    @Test
    fun `findNullable finds state with nullable production`() {
        // given
        val dfaA = SimpleDFA(0, productions<Int, Char>(), results(0))
        val dfaB = SimpleDFA(0, mapOf(0 via 'A' to 1), results(1))
        val automata = mapOf('A' to dfaA, 'B' to dfaB)

        // when
        val nullable = findNullable(automata)

        // then
        assertThat(nullable).containsExactlyInAnyOrder(
            Pair(0, dfaA), // accepting state
            Pair(1, dfaB), // accepting state
            Pair(0, dfaB), // state with nullable production
        )
    }

    @Test
    fun `findNullable finds states with mutually recursive productions`() {
        // given
        val dfaN = SimpleDFA(0, mapOf(0 via 'N' to 1), results(0, 1))
        val dfaA = SimpleDFA(0, mapOf(0 via 'A' to 1, 0 via 'N' to 2), results(1, 2))
        val dfaB = SimpleDFA(0, mapOf(0 via 'B' to 1, 0 via 'N' to 2), results(1, 2))
        val automata = mapOf('A' to dfaA, 'B' to dfaB, 'N' to dfaN)

        // when
        val nullable = findNullable(automata)

        // then
        assertThat(nullable).containsExactlyInAnyOrder(
            Pair(0, dfaN), // accepting state
            Pair(1, dfaA), // accepting state
            Pair(1, dfaB), // accepting state
            Pair(0, dfaA), // state with nullable production
            Pair(0, dfaB), // state with nullable production
        )
    }

    @Test
    fun `findNullable finds states with self recursive productions`() {
        // given
        val dfaA =
            SimpleDFA(
                0,
                mapOf(
                    0 via 'A' to 1,
                    1 via 'A' to 2,
                    2 via 'A' to 3,
                ),
                results(0, 3),
            )
        val automata = mapOf('A' to dfaA)

        // when
        val nullable = findNullable(automata)

        // then
        assertThat(nullable).containsExactlyInAnyOrder(
            Pair(0, dfaA), // accepting state
            Pair(1, dfaA), // state with nullable production
            Pair(2, dfaA), // state with nullable production
            Pair(3, dfaA), // accepting state
        )
    }

    @Test
    fun `findNullable finds states with transitively nullable productions`() {
        // given
        val dfaA = SimpleDFA(0, productions<Int, Char>(), results(0))
        val dfaB = SimpleDFA(0, productions<Int, Char>(), results(0))
        val dfaC = SimpleDFA(0, productions<Int, Char>(), results(0))
        val dfaX =
            SimpleDFA(
                0,
                // X -> ABC
                mapOf(0 via 'A' to 1, 1 via 'B' to 2, 2 via 'C' to 3),
                results(3),
            )

        val automata = mapOf('X' to dfaX, 'A' to dfaA, 'B' to dfaB, 'C' to dfaC)

        // when
        val nullable = findNullable(automata)

        // then
        assertThat(nullable).containsExactlyInAnyOrder(
            // Nullables A, B, C
            Pair(0, dfaA), // accepting state
            Pair(0, dfaB), // accepting state
            Pair(0, dfaC), // accepting state
            Pair(3, dfaX), // accepting state
            Pair(2, dfaX), // Nullable via C
            Pair(1, dfaX), // Nullable via BC
            Pair(0, dfaX), // Nullable via ABC
        )
    }
}
