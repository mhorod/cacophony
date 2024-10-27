package cacophony.grammars

import cacophony.automata.DFA
import cacophony.automata.SimpleDFA
import cacophony.automata.minimalization.via
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FindFirstTest {
    private fun <T> results(vararg results: T) = results.associateWith { }

    private fun <StateType, AtomType> productions() = mapOf<Pair<StateType, AtomType>, StateType>()

    private fun <StateType, SymbolType, ResultType> stateToSymbols(
        vararg results: Pair<Pair<StateType, DFA<StateType, SymbolType, ResultType>>, Set<SymbolType>>,
    ) = results.associate { it }

    @Test
    fun `findFirst returns first symbols for simple CFG`() {
        // given
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via 'a' to 1, 1 via 'b' to 2),
                results(2),
            )

        // A -> ab
        val automata = mapOf('A' to dfaA)

        // when
        val nullable = setOf(Pair(2, dfaA)) // only accepting state is nullable

        val first = findFirst(automata, nullable)

        // then
        assertThat(first)
            .containsExactlyInAnyOrderEntriesOf(
                stateToSymbols(
                    Pair(0, dfaA) to setOf('a'),
                    Pair(1, dfaA) to setOf('b'),
                    Pair(2, dfaA) to emptySet(),
                ),
            )
    }
}
