package cacophony.grammars

import cacophony.automata.DFA
import cacophony.automata.areEquivalent
import cacophony.automata.joinAutomata
import cacophony.automata.minimalization.buildDFAFromRegex
import cacophony.utils.AlgebraicRegex
import cacophony.utils.AlgebraicRegex.Companion.atomic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GrammarTest {
    @Nested
    inner class ConstructingGrammarTest {
        @Test
        fun `grammar can be constructed with helper constructor`() {
            val grammar =
                grammarOf(
                    0,
                    0 produces (atomic(1) or atomic(2)),
                    1 produces atomic(3),
                )

            assertEquals(grammar.start, 0)
            assertEquals(grammar.productions.size, 2)
        }

        @Test
        fun `grammar allows for multiple productions with same lhs`() {
            val grammar =
                grammarOf(
                    0,
                    0 produces atomic(1),
                    0 produces atomic(2),
                )

            assertEquals(grammar.start, 0)
            assertEquals(grammar.productions.size, 2)
        }

        @Test
        fun `grammar can be constructed using all regex constructions`() {
            val grammar =
                grammarOf(
                    0,
                    0 produces (atomic(1).star() then (atomic(2) or atomic(3))),
                    0 produces (atomic(1) or atomic(2)).star(),
                    0 produces (atomic(1) then atomic(2)),
                )

            assertEquals(grammar.start, 0)
            assertEquals(grammar.productions.size, 3)
        }
    }

    @Nested
    inner class BuildingAutomataTest {
        @Test
        fun `grammar builds automata from productions`() {
            val grammar =
                grammarOf(
                    0,
                    0 produces (atomic(1) or atomic(2)),
                    1 produces atomic(3),
                )

            val automata = grammar.buildAutomata()

            assertEquals(automata.size, 2)
        }

        @Test
        fun `multiple productions for one symbol are compressed into one automaton`() {
            // given
            val production1 = 0 produces (atomic(1) or atomic(2))
            val production2 = 0 produces atomic(3)
            val grammar =
                grammarOf(
                    0,
                    production1,
                    production2,
                )

            // when
            val automata = grammar.buildAutomata()

            // then
            val regex1 = AlgebraicRegex.UnionRegex(AlgebraicRegex.AtomicRegex(1), AlgebraicRegex.AtomicRegex(2))
            val regex2 = AlgebraicRegex.AtomicRegex(3)
            val expected: DFA<Int, Int, Production<Int>> =
                joinAutomata(
                    listOf(buildDFAFromRegex(regex1), buildDFAFromRegex(regex2)).zip(listOf(production1, production2)),
                )

            val actual: DFA<Int, Int, Production<Int>> = automata[0]!!

            assertEquals(automata.size, 1)
            assertTrue(areEquivalent(expected, actual))
        }
    }
}
