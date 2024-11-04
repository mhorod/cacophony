package cacophony.grammars

import cacophony.automata.DFA
import cacophony.automata.SimpleDFA
import cacophony.automata.minimalization.via
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

typealias TestDFA = DFA<Int, Char, Unit>

class FindFirstTest {
    private fun <T> results(vararg results: T) = results.associateWith { }

    private fun <StateType, AtomType> productions() = mapOf<Pair<StateType, AtomType>, StateType>()

    private fun <StateType, SymbolType, ResultType> stateToSymbols(
        vararg results: Pair<Pair<StateType, DFA<StateType, SymbolType, ResultType>>, Set<SymbolType>>,
    ) = results.associate { it }

    @Test
    fun `findFirst returns empty first symbols for empty word automaton`() {
        // given
        val dfaA: TestDFA = SimpleDFA(0, productions(), results(0))

        // A -> ε
        val automata = mapOf('A' to dfaA)
        val nullable = setOf(Pair(0, dfaA))

        // when
        val first = findFirst(automata, nullable)

        // then
        assertThat(first)
            .containsExactlyInAnyOrderEntriesOf(
                stateToSymbols(0 via dfaA to emptySet()),
            )
    }

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
        val nullable = setOf(Pair(2, dfaA)) // accepting state

        // when
        val first = findFirst(automata, nullable)

        // then
        assertThat(first)
            .containsExactlyInAnyOrderEntriesOf(
                stateToSymbols(
                    0 via dfaA to setOf('a'),
                    1 via dfaA to setOf('b'),
                    2 via dfaA to emptySet(),
                ),
            )
    }

    @Test
    fun `findFirst returns first symbols from multiple transitive non-terminals`() {
        // given
        val dfaA = SimpleDFA(0, mapOf(0 via 'a' to 1), results(1))
        val dfaB = SimpleDFA(0, mapOf(0 via 'A' to 1, 0 via 'b' to 1), results(1))
        val dfaC = SimpleDFA(0, mapOf(0 via 'B' to 1, 0 via 'c' to 1), results(1))

        // A -> a
        // B -> A | b
        // C -> B | c
        val automata = mapOf('A' to dfaA, 'B' to dfaB, 'C' to dfaC)
        val nullable = setOf(Pair(1, dfaA), Pair(1, dfaB), Pair(1, dfaC)) // accepting states

        // when
        val first = findFirst(automata, nullable)

        // then
        assertThat(first)
            .containsExactlyInAnyOrderEntriesOf(
                stateToSymbols(
                    0 via dfaA to setOf('a'), // from 0 via a to 1
                    1 via dfaA to emptySet(), // accepting state
                    0 via dfaB to
                        setOf(
                            'A',
                            'b', // direct productions
                            'a', // transitive production from A
                        ),
                    1 via dfaB to emptySet(), // accepting state
                    0 via dfaC to
                        setOf(
                            'B',
                            'c', // direct productions
                            'A',
                            'a',
                            'b', // transitive productions from B
                        ),
                    1 via dfaC to emptySet(),
                ),
            )
    }

    @Test
    fun `findFirst returns first symbols after nullable non-terminals`() {
        // given
        val dfaA: TestDFA = SimpleDFA(0, productions(), results(0))
        val dfaB: TestDFA = SimpleDFA(0, productions(), results(0))
        val dfaC =
            SimpleDFA(
                0,
                mapOf(
                    0 via 'A' to 1,
                    1 via 'B' to 2,
                    0 via 'c' to 3,
                    1 via 'a' to 3,
                    2 via 'b' to 3,
                ),
                results(3),
            )

        // A -> ε
        // B -> ε
        // C -> c | Aa | ABb
        val automata = mapOf('A' to dfaA, 'B' to dfaB, 'C' to dfaC)
        val nullable = setOf(Pair(0, dfaA), Pair(0, dfaB), Pair(3, dfaC)) // accepting states

        // when
        val first = findFirst(automata, nullable)

        // then
        assertThat(first)
            .containsExactlyInAnyOrderEntriesOf(
                stateToSymbols(
                    0 via dfaA to emptySet(), // accepting state
                    0 via dfaB to emptySet(), // accepting state
                    0 via dfaC to
                        setOf(
                            'c', // direct production
                            'A', // direct production
                            'B', // from AB with nullable B
                            'a', // from Aa with nullable A
                            'b', // from ABb with nullable A and B
                        ),
                    1 via dfaC to
                        setOf(
                            'B', // direct production
                            'a', // direct production
                            'b', // from Bb with nullable B
                        ),
                    2 via dfaC to setOf('b'), // direct production
                    3 via dfaC to emptySet(), // accepting state
                ),
            )
    }

    @Test
    fun `findFirst returns first symbols for grammar with recursive productions`() {
        // given
        val dfaA = SimpleDFA(0, mapOf(0 via 'B' to 1, 0 via 'b' to 1), results(1))
        val dfaB = SimpleDFA(0, mapOf(0 via 'A' to 1, 0 via 'a' to 1), results(1))

        // A -> B | b
        // B -> A | a
        val automata = mapOf('A' to dfaA, 'B' to dfaB)
        val nullable = setOf(Pair(1, dfaA), Pair(1, dfaB)) // accepting states

        // when
        val first = findFirst(automata, nullable)

        // then
        assertThat(first)
            .containsExactlyInAnyOrderEntriesOf(
                stateToSymbols(
                    0 via dfaA to
                        setOf(
                            'B', // direct production
                            'b', // direct production
                            'A', // from B
                            'a', // from B
                        ),
                    1 via dfaA to emptySet(), // accepting state
                    0 via dfaB to
                        setOf(
                            'A', // direct production
                            'a', // direct production
                            'B', // from A
                            'b', // from A
                        ),
                    1 via dfaB to emptySet(), // accepting state
                ),
            )
    }
}
