package cacophony.grammars

import cacophony.automata.DFA
import cacophony.automata.SimpleDFA
import cacophony.automata.minimalization.via
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FindFollowTest {
    private fun <T> results(vararg results: T) = results.associateWith { }

    private fun <StateType, AtomType> productions() = mapOf<Pair<StateType, AtomType>, StateType>()

    private fun <StateType, SymbolType, ResultType> stateToSymbols(
        vararg results: Pair<Pair<StateType, DFA<StateType, SymbolType, ResultType>>, Set<SymbolType>>
    ) = results.associate { it }

    // test find follow for simple grammar without regexes
    @Test
    fun `findFollow finds empty follow sets when there are no non-terminals`() {
        // given
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via 'a' to 1, 1 via 'b' to 2),
                results(2),
            )

        // A -> ab
        val automata = mapOf('A' to dfaA)
        val nullable = setOf(Pair(2, dfaA)) // only accepting state is nullable
        val first = stateToSymbols(Pair(0, dfaA) to setOf('a'), Pair(1, dfaA) to setOf('b'))

        // when
        val follow = findFollow(automata, nullable, first)

        // then
        assertThat(follow)
            .containsExactlyInAnyOrderEntriesOf(
                stateToSymbols(
                    Pair(0, dfaA) to emptySet(),
                    Pair(1, dfaA) to emptySet(),
                    Pair(2, dfaA) to emptySet(),
                ),
            )
    }

    @Test
    fun `findFollow finds symbols following non-terminal`() {
        // given
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via 'B' to 1, 1 via 'b' to 2),
                results(2),
            )

        val dfaB =
            SimpleDFA(
                0,
                mapOf(0 via 'c' to 1, 1 via 'd' to 2),
                results(2),
            )

        // A -> Bb
        // B -> cd
        val automata = mapOf('A' to dfaA, 'B' to dfaB)
        val nullable = setOf(Pair(2, dfaA), Pair(2, dfaB)) // accepting states
        val first =
            stateToSymbols(
                Pair(0, dfaA) to setOf('B', 'a'),
                Pair(1, dfaA) to setOf('b'),
                Pair(2, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf('c'),
                Pair(1, dfaB) to setOf('d'),
                Pair(2, dfaB) to emptySet(),
            )

        // when
        val follow = findFollow(automata, nullable, first)

        // then
        assertThat(follow)
            .containsExactlyInAnyOrderEntriesOf(
                stateToSymbols(
                    Pair(0, dfaA) to emptySet(),
                    Pair(1, dfaA) to emptySet(),
                    Pair(2, dfaA) to emptySet(),
                    // From A -> Bb everything that appears at the end of B has b in follow set
                    Pair(0, dfaB) to setOf('b'),
                    Pair(1, dfaB) to setOf('b'),
                    Pair(2, dfaB) to setOf('b'),
                ),
            )
    }
}
