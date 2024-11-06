package cacophony.grammars

import cacophony.automata.DFA
import cacophony.automata.SimpleDFA
import cacophony.automata.areEquivalent
import cacophony.automata.via
import cacophony.grammars.AnalyzedGrammar.Companion.buildAutomata
import cacophony.utils.AlgebraicRegex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Nested
class BuildingAutomataTest {
    @Test
    fun `automaton is created for each production`() {
        // given
        val grammar =
            Grammar(
                0,
                listOf(
                    0 produces (AlgebraicRegex.atomic(1) or AlgebraicRegex.atomic(2)),
                    1 produces AlgebraicRegex.atomic(3),
                ),
            )

        // when
        val automata = buildAutomata(grammar)

        // then
        assertThat(automata.size).isEqualTo(2)
    }

    @Test
    fun `multiple productions for one symbol are compressed into one automaton`() {
        // given
        val production1 = 'A' produces (AlgebraicRegex.atomic('a') or AlgebraicRegex.atomic('b'))
        val production2 = 'A' produces AlgebraicRegex.atomic('c')
        val grammar =
            Grammar(
                'A',
                listOf(
                    production1,
                    production2,
                ),
            )

        // when
        val automata = buildAutomata(grammar)

        // then
        val expected =
            SimpleDFA(
                0,
                mapOf(0 via 'a' to 1, 0 via 'b' to 1, 0 via 'c' to 2),
                mapOf(1 to production1, 2 to production2),
            )

        val actual: DFA<Int, Char, Production<Char>> = automata['A']!!

        assertThat(automata.size).isEqualTo(1)
        assertThat(areEquivalent(expected, actual)).isTrue()
    }
}
