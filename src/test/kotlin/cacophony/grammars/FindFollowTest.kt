package cacophony.grammars

import cacophony.automata.DFA
import cacophony.automata.SimpleDFA
import cacophony.automata.via
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FindFollowTest {
    private fun <T> results(vararg results: T) = results.associateWith { }

    private fun <StateT, AtomT> productions() = mapOf<Pair<StateT, AtomT>, StateT>()

    private fun <StateT, SymbolT, ResultT> stateToSymbols(
        vararg results: Pair<Pair<StateT, DFA<StateT, SymbolT, ResultT>>, Set<SymbolT>>,
    ) = results.associate { it }

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
        val nullable = setOf(Pair(2, dfaA)) // accepting state
        val first =
            stateToSymbols(
                // direct productions
                0 via dfaA to setOf('a'),
                1 via dfaA to setOf('b'),
                2 via dfaA to emptySet(),
            )

        // when
        val follow = findFollow(automata, nullable, first)

        // then
        assertThat(follow)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    'A' to emptySet(), // A does not appear in any production
                    'a' to setOf('b'), // in production A -> ab
                    'b' to emptySet(), // b only appears at the end of the only production
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
                Pair(0, dfaA) to
                    setOf(
                        'B', // direct production
                        'c', // transitive from B -> cd
                    ),
                1 via dfaA to setOf('b'), // direct production
                2 via dfaA to emptySet(), // accepting state
                0 via dfaB to setOf('c'), // direct production
                1 via dfaB to setOf('d'), // direct production
                2 via dfaB to emptySet(), // accepting state
            )

        // when
        val follow = findFollow(automata, nullable, first)

        // then
        assertThat(follow)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    'A' to emptySet(), // A does not appear in any production
                    'B' to setOf('b'), // in production A -> Bb
                    'b' to emptySet(), // b only appears at the end
                    'c' to setOf('d'), // in production B -> cd
                    'd' to setOf('b'), // transitive from B -> cd in A -> Bb -> cdb
                ),
            )
    }

    @Test
    fun `findFollow finds first symbols from following non-terminal`() {
        // given
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via 'a' to 1),
                results(1),
            )

        val dfaB =
            SimpleDFA(
                0,
                mapOf(0 via 'b' to 1, 0 via 'c' to 1),
                results(1),
            )

        val dfaC =
            SimpleDFA(
                0,
                mapOf(0 via 'A' to 1, 1 via 'B' to 2),
                results(2),
            )

        // A -> a
        // B -> b | c
        // C -> AB
        val automata = mapOf('A' to dfaA, 'B' to dfaB, 'C' to dfaC)
        val nullable = setOf(Pair(1, dfaA), Pair(1, dfaB), Pair(2, dfaC)) // accepting states
        val first =
            stateToSymbols(
                0 via dfaA to setOf('a'), // direct production
                1 via dfaA to emptySet(), // accepting state
                0 via dfaB to setOf('b', 'c'), // direct productions
                1 via dfaB to emptySet(), // accepting state
                Pair(0, dfaC) to
                    setOf(
                        'A', // direct production
                        'a', // transitive from A -> a
                    ),
                Pair(1, dfaC) to
                    setOf(
                        'B', // direct production
                        'b', // transitive from B -> b
                        'c', // transitive from B -> c
                    ),
                2 via dfaC to emptySet(), // accepting state
            )

        // when
        val follow = findFollow(automata, nullable, first)

        // then
        assertThat(follow)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    'A' to setOf('B', 'b', 'c'), // in C -> AB, and then also applying B -> b or B -> c
                    'B' to emptySet(), // B only appears at the end
                    'C' to emptySet(), // C does not appear in any production
                    'a' to setOf('B', 'b', 'c'), // transitive from A -> a in C -> AB -> aB, and then applying B -> b or B -> c
                    'b' to emptySet(), // b can only appear from B, which can only appear at the end of a derivation
                    'c' to emptySet(), // c can only appear from B, which can only appear at the end of a derivation
                ),
            )
    }

    @Test
    fun `findFollow finds symbols following nullable non-terminal`() {
        // given
        val dfaA = SimpleDFA(0, productions<Int, Char>(), results(0))
        val dfaB =
            SimpleDFA(
                0,
                mapOf(
                    0 via 'A' to 1,
                    1 via 'b' to 2,
                ),
                results(2),
            )
        val dfaC = SimpleDFA(0, mapOf(0 via 'c' to 1), results(1))
        val dfaD = SimpleDFA(0, mapOf(0 via 'C' to 1, 1 via 'B' to 2), results(2))

        // A -> ε
        // B -> Ab
        // C -> c
        // D -> CB
        val automata = mapOf('A' to dfaA, 'B' to dfaB, 'C' to dfaC, 'D' to dfaD)
        val nullable = setOf(Pair(0, dfaA), Pair(2, dfaB), Pair(1, dfaC), Pair(2, dfaD)) // accepting states
        val first =
            stateToSymbols(
                0 via dfaA to emptySet(), // accepting state
                0 via dfaB to setOf('A'), // direct production
                1 via dfaB to setOf('b'), // direct production
                2 via dfaB to emptySet(), // accepting state
                0 via dfaC to setOf('c'), // direct production
                1 via dfaC to emptySet(), // accepting state
                Pair(0, dfaD) to
                    setOf(
                        'C', // direct production
                        'c', // transitive from C -> c
                    ),
                Pair(1, dfaD) to
                    setOf(
                        'B', // direct production
                        'A', // transitive from B -> A
                        'b', // transitive from B -> Ab with nullable A
                    ),
                2 via dfaD to emptySet(), // accepting state
            )

        // when
        val follow = findFollow(automata, nullable, first)

        // then
        assertThat(follow)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    'A' to setOf('b'), // in B -> Ab
                    'B' to setOf(), // B only appears at the end
                    // C is by B in D -> CB
                    // then by A because CB *-> CAb (from B -> Ab)
                    // and finally by b in CAb *-> Cb (from A -> ε)
                    'C' to setOf('A', 'B', 'b'),
                    'D' to setOf(), // D does not appear in any production
                    // b can only appear at the end of something derived from B, which itself only appears at the end
                    'b' to setOf(),
                    // c can only be derived from C -> c, so it has the same followers as C
                    'c' to setOf('A', 'B', 'b'),
                ),
            )
    }
}
