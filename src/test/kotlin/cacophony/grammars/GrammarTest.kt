package cacophony.grammars

import cacophony.utils.AlgebraicRegex.Companion.atomic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GrammarTest {
    @Nested
    inner class ConstructingGrammarTest {
        @Test
        fun `grammar can be constructed with helper constructor`() {
            val grammar =
                Grammar(
                    0,
                    listOf(
                        0 produces (atomic(1) or atomic(2)),
                        1 produces atomic(3),
                    ),
                )

            assertEquals(grammar.start, 0)
            assertEquals(grammar.productions.size, 2)
        }

        @Test
        fun `grammar allows for multiple productions with same lhs`() {
            val grammar =
                Grammar(
                    0,
                    listOf(
                        0 produces atomic(1),
                        0 produces atomic(2),
                    ),
                )

            assertEquals(grammar.start, 0)
            assertEquals(grammar.productions.size, 2)
        }

        @Test
        fun `grammar can be constructed using all regex constructions`() {
            val grammar =
                Grammar(
                    0,
                    listOf(
                        0 produces (atomic(1).star() concat (atomic(2) or atomic(3))),
                        0 produces (atomic(1) or atomic(2)).star(),
                        0 produces (atomic(1) concat atomic(2)),
                    ),
                )

            assertEquals(grammar.start, 0)
            assertEquals(grammar.productions.size, 3)
        }
    }
}
