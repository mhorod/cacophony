package cacophony.parser

import cacophony.automata.SimpleDFA
import cacophony.automata.minimalization.via
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.DFAStateReference
import cacophony.grammars.Production
import cacophony.utils.AlgebraicRegex
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LLOneParserTest {
    enum class Symbol {
        // nonterminals
        A,
        B,
        C,

        // terminals
        X,
        Y,
    }

    @Test
    fun `constructor doesn't throw on a simple grammar`() {
        // A -> B
        // B -> C
        // C -> X
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.B to 1),
                mapOf(
                    1 to Production(Symbol.A, AlgebraicRegex.atomic(Symbol.B)),
                ),
            )
        val dfaB = SimpleDFA(0, mapOf(0 via Symbol.C to 1), mapOf(1 to Production(Symbol.B, AlgebraicRegex.atomic(Symbol.C))))
        val dfaC = SimpleDFA(0, mapOf(0 via Symbol.X to 1), mapOf(0 to Production(Symbol.C, AlgebraicRegex.atomic(Symbol.X))))
        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB, Symbol.C to dfaC)
        val nullable =
            setOf(
                Pair(1, dfaA),
                Pair(1, dfaB),
                Pair(1, dfaC),
            )
        val first =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                Pair(0, dfaA) to setOf(Symbol.B, Symbol.C, Symbol.X),
                Pair(1, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf(Symbol.C, Symbol.X),
                Pair(1, dfaB) to emptySet(),
                Pair(0, dfaC) to setOf(Symbol.X),
                Pair(1, dfaC) to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                Pair(0, dfaA) to emptySet(),
                Pair(1, dfaA) to emptySet(),
                Pair(0, dfaB) to emptySet(),
                Pair(1, dfaB) to emptySet(),
                Pair(0, dfaC) to emptySet(),
                Pair(1, dfaC) to emptySet(),
            )

        val analyzedGrammar = AnalyzedGrammar(listOf(), automata, nullable, first, follow)

        assertDoesNotThrow {
            LLOneParser.fromAnalyzedGrammar(analyzedGrammar)
        }
    }

    @Test
    fun `constructor throws given ambiguous grammar`() {
        // A -> B | C
        // B -> C
        // C -> X
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.B to 1, 0 via Symbol.C to 2),
                mapOf(
                    1 to Production(Symbol.A, AlgebraicRegex.atomic(Symbol.B)),
                    2 to Production(Symbol.A, AlgebraicRegex.atomic(Symbol.C)),
                ),
            )
        val dfaB = SimpleDFA(0, mapOf(0 via Symbol.C to 1), mapOf(1 to Production(Symbol.B, AlgebraicRegex.atomic(Symbol.C))))
        val dfaC = SimpleDFA(0, mapOf(0 via Symbol.X to 1), mapOf(0 to Production(Symbol.C, AlgebraicRegex.atomic(Symbol.X))))
        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB, Symbol.C to dfaC)
        val nullable =
            setOf(
                Pair(1, dfaA),
                Pair(2, dfaA),
                Pair(1, dfaB),
                Pair(1, dfaC),
            )
        val first =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                Pair(0, dfaA) to setOf(Symbol.B, Symbol.C, Symbol.X),
                Pair(1, dfaA) to emptySet(),
                Pair(2, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf(Symbol.C, Symbol.X),
                Pair(1, dfaB) to emptySet(),
                Pair(0, dfaC) to setOf(Symbol.X),
                Pair(1, dfaC) to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                Pair(0, dfaA) to emptySet(),
                Pair(1, dfaA) to emptySet(),
                Pair(2, dfaA) to emptySet(),
                Pair(0, dfaB) to emptySet(),
                Pair(1, dfaB) to emptySet(),
                Pair(0, dfaC) to emptySet(),
                Pair(1, dfaC) to emptySet(),
            )

        val analyzedGrammar = AnalyzedGrammar(listOf(), automata, nullable, first, follow)

        assertThrows(ParserConstructorErrorException::class.java) {
            LLOneParser.fromAnalyzedGrammar(analyzedGrammar)
        }
    }

    @Test
    fun `constructor doesn't throw on a grammar with nonempty follow`() {
        // A -> Bx
        // B -> y | ε
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.B to 1, 1 via Symbol.X to 2),
                mapOf(
                    2 to
                        Production(
                            Symbol.A,
                            AlgebraicRegex.ConcatenationRegex(AlgebraicRegex.atomic(Symbol.B), AlgebraicRegex.atomic(Symbol.X)),
                        ),
                ),
            )
        val dfaB =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.Y to 1),
                mapOf(
                    0 to Production(Symbol.B, AlgebraicRegex.atomic(Symbol.B)),
                    // ^ this one should be empty, we cannot do that now
                    1 to Production(Symbol.B, AlgebraicRegex.atomic(Symbol.Y)),
                ),
            )

        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB)
        val nullable =
            setOf(
                Pair(2, dfaA),
                Pair(0, dfaB),
                Pair(1, dfaB),
            )
        val first =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                Pair(0, dfaA) to setOf(Symbol.X, Symbol.Y),
                Pair(1, dfaA) to setOf(Symbol.X),
                Pair(2, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf(Symbol.Y),
                Pair(1, dfaB) to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                Pair(0, dfaA) to emptySet(),
                Pair(1, dfaA) to emptySet(),
                Pair(2, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf(Symbol.X),
                Pair(1, dfaB) to setOf(Symbol.X),
            )

        val analyzedGrammar = AnalyzedGrammar(listOf(), automata, nullable, first, follow)

        assertDoesNotThrow {
            LLOneParser.fromAnalyzedGrammar(analyzedGrammar)
        }
    }
}
