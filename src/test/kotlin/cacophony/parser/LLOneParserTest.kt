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

typealias ReS = AlgebraicRegex<LLOneParserTest.Symbol>
typealias CatS = AlgebraicRegex.ConcatenationRegex<LLOneParserTest.Symbol>
typealias ReSA = AlgebraicRegex<LLOneParserTest.SymbolAryt>
typealias CatSA = AlgebraicRegex.ConcatenationRegex<LLOneParserTest.SymbolAryt>

class LLOneParserTest {
    enum class Symbol {
        // nonterminals
        A,
        B,
        C,

        // terminals
        X,
        Y,
        Z,
    }

    enum class SymbolAryt {
        // nonterminals
        A,
        B,
        C,

        // terminals
        X,
        SUM,
        PROD,
        LPAREN,
        RPAREN,
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
                mapOf(1 to Production(Symbol.A, ReS.atomic(Symbol.B))),
            )
        val dfaB = SimpleDFA(0, mapOf(0 via Symbol.C to 1), mapOf(1 to Production(Symbol.B, ReS.atomic(Symbol.C))))
        val dfaC = SimpleDFA(0, mapOf(0 via Symbol.X to 1), mapOf(0 to Production(Symbol.C, ReS.atomic(Symbol.X))))
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
                    1 to Production(Symbol.A, ReS.atomic(Symbol.B)),
                    2 to Production(Symbol.A, ReS.atomic(Symbol.C)),
                ),
            )
        val dfaB = SimpleDFA(0, mapOf(0 via Symbol.C to 1), mapOf(1 to Production(Symbol.B, ReS.atomic(Symbol.C))))
        val dfaC = SimpleDFA(0, mapOf(0 via Symbol.X to 1), mapOf(0 to Production(Symbol.C, ReS.atomic(Symbol.X))))
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
        // A -> BX
        // B -> Y | ε
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.B to 1, 1 via Symbol.X to 2),
                mapOf(2 to Production(Symbol.A, CatS(ReS.atomic(Symbol.B), ReS.atomic(Symbol.X)))),
            )
        val dfaB =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.Y to 1),
                mapOf(
                    0 to Production(Symbol.B, ReS.atomic(Symbol.B)),
                    // ^ this one should be empty, we cannot do that now
                    1 to Production(Symbol.B, ReS.atomic(Symbol.Y)),
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

    @Test
    fun `constructor throws given grammar with first-first conflict`() {
        // A -> BY | CZ
        // B -> X
        // C -> X
        val dfaA =
            SimpleDFA(
                0,
                mapOf(
                    0 via Symbol.B to 1,
                    1 via Symbol.Y to 2,
                    0 via Symbol.C to 3,
                    3 via Symbol.Z to 4,
                ),
                mapOf(
                    2 to Production(Symbol.A, CatS(ReS.atomic(Symbol.B), ReS.atomic(Symbol.Y))),
                    4 to Production(Symbol.A, CatS(ReS.atomic(Symbol.C), ReS.atomic(Symbol.Z))),
                ),
            )
        val dfaB = SimpleDFA(0, mapOf(0 via Symbol.X to 1), mapOf(1 to Production(Symbol.B, ReS.atomic(Symbol.X))))
        val dfaC = SimpleDFA(0, mapOf(0 via Symbol.X to 1), mapOf(0 to Production(Symbol.C, ReS.atomic(Symbol.X))))
        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB, Symbol.C to dfaC)
        val nullable =
            setOf(
                Pair(2, dfaA),
                Pair(4, dfaA),
                Pair(1, dfaB),
                Pair(1, dfaC),
            )
        val first =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                Pair(0, dfaA) to setOf(Symbol.B, Symbol.C, Symbol.X),
                Pair(1, dfaA) to setOf(Symbol.Y),
                Pair(2, dfaA) to emptySet(),
                Pair(3, dfaA) to setOf(Symbol.Z),
                Pair(4, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf(Symbol.X),
                Pair(1, dfaB) to emptySet(),
                Pair(0, dfaC) to setOf(Symbol.X),
                Pair(1, dfaC) to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                Pair(0, dfaA) to emptySet(),
                Pair(1, dfaA) to emptySet(),
                Pair(2, dfaA) to emptySet(),
                Pair(3, dfaA) to emptySet(),
                Pair(4, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf(Symbol.Y),
                Pair(1, dfaB) to setOf(Symbol.Y),
                Pair(0, dfaC) to setOf(Symbol.Z),
                Pair(1, dfaC) to setOf(Symbol.Z),
            )

        val analyzedGrammar = AnalyzedGrammar(listOf(), automata, nullable, first, follow)

        assertThrows(ParserConstructorErrorException::class.java) {
            LLOneParser.fromAnalyzedGrammar(analyzedGrammar)
        }
    }

    @Test
    fun `constructor throws given grammar with first-follow conflict`() {
        // A -> BC
        // B -> XY | ε
        // C -> XZ
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.B to 1, 1 via Symbol.C to 2),
                mapOf(2 to Production(Symbol.A, CatS(ReS.atomic(Symbol.B), ReS.atomic(Symbol.C)))),
            )
        val dfaB =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.X to 1, 1 via Symbol.Y to 2),
                mapOf(
                    0 to Production(Symbol.B, ReS.atomic(Symbol.B)),
                    // ^ this one should be empty, we cannot do that now
                    2 to Production(Symbol.B, CatS(ReS.atomic(Symbol.X), ReS.atomic(Symbol.Y))),
                ),
            )
        val dfaC =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.X to 1, 1 via Symbol.Z to 2),
                mapOf(2 to Production(Symbol.C, CatS(ReS.atomic(Symbol.X), ReS.atomic(Symbol.Z)))),
            )
        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB, Symbol.C to dfaC)
        val nullable =
            setOf(
                Pair(2, dfaA),
                Pair(0, dfaB),
                Pair(2, dfaB),
                Pair(2, dfaC),
            )
        val first =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                Pair(0, dfaA) to setOf(Symbol.B, Symbol.X),
                Pair(1, dfaA) to setOf(Symbol.C, Symbol.X),
                Pair(2, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf(Symbol.X),
                Pair(1, dfaB) to setOf(Symbol.Y),
                Pair(2, dfaB) to emptySet(),
                Pair(0, dfaC) to setOf(Symbol.X),
                Pair(1, dfaC) to setOf(Symbol.Z),
                Pair(2, dfaC) to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                Pair(0, dfaA) to emptySet(),
                Pair(1, dfaA) to emptySet(),
                Pair(2, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf(Symbol.C, Symbol.X),
                Pair(1, dfaB) to setOf(Symbol.C, Symbol.X),
                Pair(2, dfaB) to setOf(Symbol.C, Symbol.X),
                Pair(0, dfaC) to emptySet(),
                Pair(1, dfaC) to emptySet(),
                Pair(2, dfaC) to emptySet(),
            )

        val analyzedGrammar = AnalyzedGrammar(listOf(), automata, nullable, first, follow)

        assertThrows(ParserConstructorErrorException::class.java) {
            LLOneParser.fromAnalyzedGrammar(analyzedGrammar)
        }
    }

    @Test
    fun `constructor doesn't throw on the arithmetic grammar`() {
        // A -> B | B + A
        // B -> C | C * B
        // C -> X | (A)
        val aToB = Production(SymbolAryt.A, ReSA.atomic(SymbolAryt.B))
        val aToSum =
            Production(
                SymbolAryt.A,
                CatSA(ReSA.atomic(SymbolAryt.B), ReSA.atomic(SymbolAryt.SUM), ReSA.atomic(SymbolAryt.A)),
            )
        val bToC = Production(SymbolAryt.B, ReSA.atomic(SymbolAryt.C))
        val bToProd =
            Production(
                SymbolAryt.B,
                CatSA(ReSA.atomic(SymbolAryt.C), ReSA.atomic(SymbolAryt.PROD), ReSA.atomic(SymbolAryt.B)),
            )
        val cToX = Production(SymbolAryt.C, ReSA.atomic(SymbolAryt.X))
        val cToGroup =
            Production(
                SymbolAryt.C,
                CatSA(ReSA.atomic(SymbolAryt.LPAREN), ReSA.atomic(SymbolAryt.A), ReSA.atomic(SymbolAryt.RPAREN)),
            )
        val dfaA =
            SimpleDFA(
                0,
                mapOf(
                    0 via SymbolAryt.B to 1,
                    1 via SymbolAryt.SUM to 2,
                    2 via SymbolAryt.A to 3,
                ),
                mapOf(3 to aToSum, 1 to aToB),
            )
        val dfaB =
            SimpleDFA(
                0,
                mapOf(
                    0 via SymbolAryt.C to 1,
                    1 via SymbolAryt.PROD to 2,
                    2 via SymbolAryt.B to 3,
                ),
                mapOf(3 to bToProd, 1 to bToC),
            )
        val dfaC =
            SimpleDFA(
                0,
                mapOf(
                    0 via SymbolAryt.X to 1,
                    0 via SymbolAryt.LPAREN to 2,
                    2 via SymbolAryt.A to 3,
                    3 via SymbolAryt.RPAREN to 4,
                ),
                mapOf(4 to cToGroup, 1 to cToX),
            )
        val automata = mapOf(SymbolAryt.A to dfaA, SymbolAryt.B to dfaB, SymbolAryt.C to dfaC)
        val nullable =
            setOf(
                Pair(1, dfaA),
                Pair(3, dfaA),
                Pair(1, dfaB),
                Pair(3, dfaB),
                Pair(1, dfaC),
                Pair(4, dfaC),
            )
        val first =
            mapOf<DFAStateReference<Int, SymbolAryt, Production<SymbolAryt>>, Set<SymbolAryt>>(
                Pair(0, dfaA) to setOf(SymbolAryt.B, SymbolAryt.C, SymbolAryt.X, SymbolAryt.LPAREN),
                Pair(1, dfaA) to setOf(SymbolAryt.SUM),
                Pair(2, dfaA) to setOf(SymbolAryt.A, SymbolAryt.B, SymbolAryt.C, SymbolAryt.X, SymbolAryt.LPAREN),
                Pair(3, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf(SymbolAryt.C, SymbolAryt.X, SymbolAryt.LPAREN),
                Pair(1, dfaB) to setOf(SymbolAryt.PROD),
                Pair(2, dfaB) to setOf(SymbolAryt.B, SymbolAryt.C, SymbolAryt.X, SymbolAryt.LPAREN),
                Pair(3, dfaB) to emptySet(),
                Pair(0, dfaC) to setOf(SymbolAryt.X, SymbolAryt.LPAREN),
                Pair(1, dfaC) to setOf(),
                Pair(2, dfaC) to setOf(SymbolAryt.A, SymbolAryt.B, SymbolAryt.C, SymbolAryt.X, SymbolAryt.LPAREN),
                Pair(3, dfaC) to setOf(SymbolAryt.RPAREN),
                Pair(4, dfaC) to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, SymbolAryt, Production<SymbolAryt>>, Set<SymbolAryt>>(
                Pair(0, dfaA) to setOf(SymbolAryt.RPAREN),
                Pair(1, dfaA) to setOf(SymbolAryt.RPAREN),
                Pair(2, dfaA) to setOf(SymbolAryt.RPAREN),
                Pair(3, dfaA) to setOf(SymbolAryt.RPAREN),
                Pair(0, dfaB) to setOf(SymbolAryt.SUM, SymbolAryt.RPAREN),
                Pair(1, dfaB) to setOf(SymbolAryt.SUM, SymbolAryt.RPAREN),
                Pair(2, dfaB) to setOf(SymbolAryt.SUM, SymbolAryt.RPAREN),
                Pair(3, dfaB) to setOf(SymbolAryt.SUM, SymbolAryt.RPAREN),
                Pair(0, dfaC) to setOf(SymbolAryt.PROD, SymbolAryt.SUM, SymbolAryt.RPAREN),
                Pair(1, dfaC) to setOf(SymbolAryt.PROD, SymbolAryt.SUM, SymbolAryt.RPAREN),
                Pair(2, dfaC) to setOf(SymbolAryt.PROD, SymbolAryt.SUM, SymbolAryt.RPAREN),
                Pair(3, dfaC) to setOf(SymbolAryt.PROD, SymbolAryt.SUM, SymbolAryt.RPAREN),
                Pair(4, dfaC) to setOf(SymbolAryt.PROD, SymbolAryt.SUM, SymbolAryt.RPAREN),
            )

        val analyzedGrammar = AnalyzedGrammar(listOf(), automata, nullable, first, follow)

        assertDoesNotThrow {
            LLOneParser.fromAnalyzedGrammar(analyzedGrammar)
        }

//        The following nextAction for terminals should be generated:
//        val nextAction =
//            mapOf(
//                Symbol.X to
//                        mapOf(
//                            DFAStateReference(0, dfaA) to Symbol.B,
//                            DFAStateReference(2, dfaA) to Symbol.A,
//                            DFAStateReference(0, dfaB) to Symbol.C,
//                            DFAStateReference(2, dfaB) to Symbol.B,
//                            DFAStateReference(0, dfaC) to Symbol.X,
//                            DFAStateReference(2, dfaC) to Symbol.A,
//                        ),
//                Symbol.SUM to
//                        mapOf(
//                            DFAStateReference(1, dfaA) to Symbol.SUM,
//                        ),
//                Symbol.PROD to
//                        mapOf(
//                            DFAStateReference(1, dfaB) to Symbol.PROD,
//                        ),
//                Symbol.LPAREN to
//                        mapOf(
//                            DFAStateReference(0, dfaA) to Symbol.B,
//                            DFAStateReference(2, dfaA) to Symbol.A,
//                            DFAStateReference(0, dfaB) to Symbol.C,
//                            DFAStateReference(2, dfaB) to Symbol.B,
//                            DFAStateReference(0, dfaC) to Symbol.LPAREN,
//                            DFAStateReference(2, dfaC) to Symbol.A,
//                        ),
//                Symbol.RPAREN to
//                        mapOf(
//                            DFAStateReference(3, dfaC) to Symbol.RPAREN,
//                        ),
//            )
    }
}
