package cacophony.parser

import cacophony.automata.SimpleDFA
import cacophony.automata.via
import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.ParserDiagnostics
import cacophony.grammars.*
import cacophony.token.Token
import cacophony.utils.AlgebraicRegex
import cacophony.utils.Location
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.collections.listOf
import kotlin.collections.mapOf

typealias ReS = AlgebraicRegex<LLOneParserTest.Symbol>
typealias CatS = AlgebraicRegex.ConcatenationRegex<LLOneParserTest.Symbol>
typealias ReSA = AlgebraicRegex<LLOneParserTest.SymbolAryt>
typealias CatSA = AlgebraicRegex.ConcatenationRegex<LLOneParserTest.SymbolAryt>
typealias StarSA = AlgebraicRegex.StarRegex<LLOneParserTest.SymbolAryt>

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

    private fun <S : Enum<S>> terminal(
        symbol: S,
        loc: Int,
    ) = ParseTree.Leaf(Token(symbol, "a", Location(loc), Location(loc + 1)))

    @MockK
    lateinit var diagnostics: Diagnostics

    @BeforeEach
    fun setUpMocks() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { diagnostics.report(any(), any<Pair<Location, Location>>()) } just runs
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
                0 via dfaA to setOf(Symbol.B, Symbol.C, Symbol.X),
                1 via dfaA to emptySet(),
                0 via dfaB to setOf(Symbol.C, Symbol.X),
                1 via dfaB to emptySet(),
                0 via dfaC to setOf(Symbol.X),
                1 via dfaC to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                0 via dfaA to emptySet(),
                1 via dfaA to emptySet(),
                0 via dfaB to emptySet(),
                1 via dfaB to emptySet(),
                0 via dfaC to emptySet(),
                1 via dfaC to emptySet(),
            )

        val analyzedGrammar = AnalyzedGrammar(Symbol.A, listOf(), automata, nullable, first, follow)

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
                0 via dfaA to setOf(Symbol.B, Symbol.C, Symbol.X),
                1 via dfaA to emptySet(),
                2 via dfaA to emptySet(),
                0 via dfaB to setOf(Symbol.C, Symbol.X),
                1 via dfaB to emptySet(),
                0 via dfaC to setOf(Symbol.X),
                1 via dfaC to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                0 via dfaA to emptySet(),
                1 via dfaA to emptySet(),
                2 via dfaA to emptySet(),
                0 via dfaB to emptySet(),
                1 via dfaB to emptySet(),
                0 via dfaC to emptySet(),
                1 via dfaC to emptySet(),
            )

        val analyzedGrammar = AnalyzedGrammar(Symbol.A, listOf(), automata, nullable, first, follow)

        assertThrows(ParserConstructorError::class.java) {
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
                0 via dfaA to setOf(Symbol.X, Symbol.Y),
                1 via dfaA to setOf(Symbol.X),
                2 via dfaA to emptySet(),
                0 via dfaB to setOf(Symbol.Y),
                1 via dfaB to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                0 via dfaA to emptySet(),
                1 via dfaA to emptySet(),
                2 via dfaA to emptySet(),
                0 via dfaB to setOf(Symbol.X),
                1 via dfaB to setOf(Symbol.X),
            )

        val analyzedGrammar = AnalyzedGrammar(Symbol.A, listOf(), automata, nullable, first, follow)

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
                0 via dfaA to setOf(Symbol.B, Symbol.C, Symbol.X),
                1 via dfaA to setOf(Symbol.Y),
                2 via dfaA to emptySet(),
                3 via dfaA to setOf(Symbol.Z),
                4 via dfaA to emptySet(),
                0 via dfaB to setOf(Symbol.X),
                1 via dfaB to emptySet(),
                0 via dfaC to setOf(Symbol.X),
                1 via dfaC to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                0 via dfaA to emptySet(),
                1 via dfaA to emptySet(),
                2 via dfaA to emptySet(),
                3 via dfaA to emptySet(),
                4 via dfaA to emptySet(),
                0 via dfaB to setOf(Symbol.Y),
                1 via dfaB to setOf(Symbol.Y),
                0 via dfaC to setOf(Symbol.Z),
                1 via dfaC to setOf(Symbol.Z),
            )

        val analyzedGrammar = AnalyzedGrammar(Symbol.A, listOf(), automata, nullable, first, follow)

        assertThrows(ParserConstructorError::class.java) {
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
                0 via dfaA to setOf(Symbol.B, Symbol.X),
                1 via dfaA to setOf(Symbol.C, Symbol.X),
                2 via dfaA to emptySet(),
                0 via dfaB to setOf(Symbol.X),
                1 via dfaB to setOf(Symbol.Y),
                2 via dfaB to emptySet(),
                0 via dfaC to setOf(Symbol.X),
                1 via dfaC to setOf(Symbol.Z),
                2 via dfaC to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, Symbol, Production<Symbol>>, Set<Symbol>>(
                0 via dfaA to emptySet(),
                1 via dfaA to emptySet(),
                2 via dfaA to emptySet(),
                0 via dfaB to setOf(Symbol.C, Symbol.X),
                1 via dfaB to setOf(Symbol.C, Symbol.X),
                2 via dfaB to setOf(Symbol.C, Symbol.X),
                0 via dfaC to emptySet(),
                1 via dfaC to emptySet(),
                2 via dfaC to emptySet(),
            )

        val analyzedGrammar = AnalyzedGrammar(Symbol.A, listOf(), automata, nullable, first, follow)

        assertThrows(ParserConstructorError::class.java) {
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
                0 via dfaA to setOf(SymbolAryt.B, SymbolAryt.C, SymbolAryt.X, SymbolAryt.LPAREN),
                1 via dfaA to setOf(SymbolAryt.SUM),
                2 via dfaA to setOf(SymbolAryt.A, SymbolAryt.B, SymbolAryt.C, SymbolAryt.X, SymbolAryt.LPAREN),
                3 via dfaA to emptySet(),
                0 via dfaB to setOf(SymbolAryt.C, SymbolAryt.X, SymbolAryt.LPAREN),
                1 via dfaB to setOf(SymbolAryt.PROD),
                2 via dfaB to setOf(SymbolAryt.B, SymbolAryt.C, SymbolAryt.X, SymbolAryt.LPAREN),
                3 via dfaB to emptySet(),
                0 via dfaC to setOf(SymbolAryt.X, SymbolAryt.LPAREN),
                1 via dfaC to setOf(),
                2 via dfaC to setOf(SymbolAryt.A, SymbolAryt.B, SymbolAryt.C, SymbolAryt.X, SymbolAryt.LPAREN),
                3 via dfaC to setOf(SymbolAryt.RPAREN),
                4 via dfaC to emptySet(),
            )
        val follow =
            mapOf<DFAStateReference<Int, SymbolAryt, Production<SymbolAryt>>, Set<SymbolAryt>>(
                0 via dfaA to setOf(SymbolAryt.RPAREN),
                1 via dfaA to setOf(SymbolAryt.RPAREN),
                2 via dfaA to setOf(SymbolAryt.RPAREN),
                3 via dfaA to setOf(SymbolAryt.RPAREN),
                0 via dfaB to setOf(SymbolAryt.SUM, SymbolAryt.RPAREN),
                1 via dfaB to setOf(SymbolAryt.SUM, SymbolAryt.RPAREN),
                2 via dfaB to setOf(SymbolAryt.SUM, SymbolAryt.RPAREN),
                3 via dfaB to setOf(SymbolAryt.SUM, SymbolAryt.RPAREN),
                0 via dfaC to setOf(SymbolAryt.PROD, SymbolAryt.SUM, SymbolAryt.RPAREN),
                1 via dfaC to setOf(SymbolAryt.PROD, SymbolAryt.SUM, SymbolAryt.RPAREN),
                2 via dfaC to setOf(SymbolAryt.PROD, SymbolAryt.SUM, SymbolAryt.RPAREN),
                3 via dfaC to setOf(SymbolAryt.PROD, SymbolAryt.SUM, SymbolAryt.RPAREN),
                4 via dfaC to setOf(SymbolAryt.PROD, SymbolAryt.SUM, SymbolAryt.RPAREN),
            )

        val analyzedGrammar = AnalyzedGrammar(SymbolAryt.A, listOf(), automata, nullable, first, follow)

        assertDoesNotThrow {
            LLOneParser.fromAnalyzedGrammar(analyzedGrammar)
        }

        // The following nextAction for terminals should be generated:
        // val nextAction =
        //     mapOf(
        //         Symbol.X to
        //                 mapOf(
        //                     DFAStateReference(0, dfaA) to Symbol.B,
        //                     DFAStateReference(2, dfaA) to Symbol.A,
        //                     DFAStateReference(0, dfaB) to Symbol.C,
        //                     DFAStateReference(2, dfaB) to Symbol.B,
        //                     DFAStateReference(0, dfaC) to Symbol.X,
        //                     DFAStateReference(2, dfaC) to Symbol.A,
        //                 ),
        //         Symbol.SUM to
        //                 mapOf(
        //                     DFAStateReference(1, dfaA) to Symbol.SUM,
        //                 ),
        //         Symbol.PROD to
        //                 mapOf(
        //                     DFAStateReference(1, dfaB) to Symbol.PROD,
        //                 ),
        //         Symbol.LPAREN to
        //                 mapOf(
        //                     DFAStateReference(0, dfaA) to Symbol.B,
        //                     DFAStateReference(2, dfaA) to Symbol.A,
        //                     DFAStateReference(0, dfaB) to Symbol.C,
        //                     DFAStateReference(2, dfaB) to Symbol.B,
        //                     DFAStateReference(0, dfaC) to Symbol.LPAREN,
        //                     DFAStateReference(2, dfaC) to Symbol.A,
        //                 ),
        //         Symbol.RPAREN to
        //                 mapOf(
        //                     DFAStateReference(3, dfaC) to Symbol.RPAREN,
        //                 ),
        //     )
    }

    @Test
    fun `parser throws if input is empty`() {
        val parser = LLOneParser<Int, Symbol>(mapOf(), Symbol.A, mapOf(), listOf())
        assertThrows(ParsingException::class.java) {
            parser.process(listOf(), diagnostics)
        }
    }

    @Test
    fun `parser returns correct tree for simple grammar`() {
        // A -> B
        // B -> C
        // C -> X

        val atob = Production(Symbol.A, ReS.atomic(Symbol.B))
        val btoc = Production(Symbol.B, ReS.atomic(Symbol.C))
        val ctox = Production(Symbol.C, ReS.atomic(Symbol.X))
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.B to 1),
                mapOf(1 to atob),
            )
        val dfaB = SimpleDFA(0, mapOf(0 via Symbol.C to 1), mapOf(1 to btoc))
        val dfaC = SimpleDFA(0, mapOf(0 via Symbol.X to 1), mapOf(1 to ctox))
        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB, Symbol.C to dfaC)

        val nextAction =
            mapOf(
                Symbol.X to
                    mapOf(
                        DFAStateReference(0, dfaA) to Symbol.B,
                        DFAStateReference(0, dfaB) to Symbol.C,
                        DFAStateReference(0, dfaC) to Symbol.X,
                    ),
            )

        val terminals =
            listOf(
                terminal(Symbol.X, 0),
            )
        val parser =
            LLOneParser(
                nextAction,
                Symbol.A,
                automata,
                listOf(),
            )

        val tree = parser.process(terminals, diagnostics)
        assertThat(tree).isEqualTo(
            ParseTree.Branch(
                Location(0) to Location(1),
                atob,
                listOf(
                    ParseTree.Branch(
                        Location(0) to Location(1),
                        btoc,
                        listOf(
                            ParseTree.Branch(
                                Location(0) to Location(1),
                                ctox,
                                terminals,
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `parser returns correct tree for arithmetic grammar`() {
        // A -> B ('+' B)*
        // B -> C | C '*' B
        // C -> X | '(' A ')'

        val atob =
            Production(
                SymbolAryt.A,
                CatSA(ReSA.atomic(SymbolAryt.B), StarSA(CatSA(ReSA.atomic(SymbolAryt.SUM), ReSA.atomic(SymbolAryt.B)))),
            )
        val btoc = Production(SymbolAryt.B, ReSA.atomic(SymbolAryt.C))
        val btoprod =
            Production(
                SymbolAryt.B,
                CatSA(ReSA.atomic(SymbolAryt.C), ReSA.atomic(SymbolAryt.PROD), ReSA.atomic(SymbolAryt.B)),
            )
        val ctox = Production(SymbolAryt.C, ReSA.atomic(SymbolAryt.X))
        val ctogroup =
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
                    2 via SymbolAryt.B to 1,
                ),
                mapOf(1 to atob),
            )
        val dfaB =
            SimpleDFA(
                0,
                mapOf(
                    0 via SymbolAryt.C to 1,
                    1 via SymbolAryt.PROD to 2,
                    2 via SymbolAryt.B to 3,
                ),
                mapOf(3 to btoprod, 1 to btoc),
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
                mapOf(4 to ctogroup, 1 to ctox),
            )
        val automata = mapOf(SymbolAryt.A to dfaA, SymbolAryt.B to dfaB, SymbolAryt.C to dfaC)

        val nextAction =
            mapOf(
                SymbolAryt.X to
                    mapOf(
                        DFAStateReference(0, dfaA) to SymbolAryt.B,
                        DFAStateReference(2, dfaA) to SymbolAryt.B,
                        DFAStateReference(0, dfaB) to SymbolAryt.C,
                        DFAStateReference(2, dfaB) to SymbolAryt.B,
                        DFAStateReference(0, dfaC) to SymbolAryt.X,
                        DFAStateReference(2, dfaC) to SymbolAryt.A,
                    ),
                SymbolAryt.SUM to
                    mapOf(
                        DFAStateReference(1, dfaA) to SymbolAryt.SUM,
                    ),
                SymbolAryt.PROD to
                    mapOf(
                        DFAStateReference(1, dfaB) to SymbolAryt.PROD,
                    ),
                SymbolAryt.LPAREN to
                    mapOf(
                        DFAStateReference(0, dfaA) to SymbolAryt.B,
                        DFAStateReference(2, dfaA) to SymbolAryt.B,
                        DFAStateReference(0, dfaB) to SymbolAryt.C,
                        DFAStateReference(2, dfaB) to SymbolAryt.B,
                        DFAStateReference(0, dfaC) to SymbolAryt.LPAREN,
                        DFAStateReference(2, dfaC) to SymbolAryt.A,
                    ),
                SymbolAryt.RPAREN to
                    mapOf(
                        DFAStateReference(3, dfaC) to SymbolAryt.RPAREN,
                    ),
            )

        // x + x * (x + x + x) * x
        val terminals =
            listOf(
                SymbolAryt.X,
                SymbolAryt.SUM,
                SymbolAryt.X,
                SymbolAryt.PROD,
                SymbolAryt.LPAREN,
                SymbolAryt.X,
                SymbolAryt.SUM,
                SymbolAryt.X,
                SymbolAryt.SUM,
                SymbolAryt.X,
                SymbolAryt.RPAREN,
                SymbolAryt.PROD,
                SymbolAryt.X,
            ).mapIndexed { idx, symbol -> terminal(symbol, idx) }

        val parser =
            LLOneParser(
                nextAction,
                SymbolAryt.A,
                automata,
                listOf(),
            )

        val tree = parser.process(terminals, diagnostics)
        assertThat(tree).isEqualTo(
            ParseTree.Branch(
                Location(0) to Location(13),
                atob,
                listOf(
                    ParseTree.Branch(
                        Location(0) to Location(1),
                        btoc,
                        listOf(
                            ParseTree.Branch(
                                Location(0) to Location(1),
                                ctox,
                                listOf(terminals[0]),
                            ),
                        ),
                    ),
                    terminals[1],
                    ParseTree.Branch(
                        Location(2) to Location(13),
                        btoprod,
                        listOf(
                            ParseTree.Branch(
                                Location(2) to Location(3),
                                ctox,
                                listOf(terminals[2]),
                            ),
                            terminals[3],
                            ParseTree.Branch(
                                Location(4) to Location(13),
                                btoprod,
                                listOf(
                                    ParseTree.Branch(
                                        Location(4) to Location(9),
                                        ctogroup,
                                        listOf(
                                            terminals[4],
                                            ParseTree.Branch(
                                                Location(5) to Location(10),
                                                atob,
                                                listOf(
                                                    ParseTree.Branch(
                                                        Location(5) to Location(6),
                                                        btoc,
                                                        listOf(
                                                            ParseTree.Branch(
                                                                Location(5) to Location(6),
                                                                ctox,
                                                                listOf(terminals[5]),
                                                            ),
                                                        ),
                                                    ),
                                                    terminals[6],
                                                    ParseTree.Branch(
                                                        Location(7) to Location(8),
                                                        btoc,
                                                        listOf(
                                                            ParseTree.Branch(
                                                                Location(7) to Location(8),
                                                                ctox,
                                                                listOf(terminals[7]),
                                                            ),
                                                        ),
                                                    ),
                                                    terminals[8],
                                                    ParseTree.Branch(
                                                        Location(9) to Location(10),
                                                        btoc,
                                                        listOf(
                                                            ParseTree.Branch(
                                                                Location(9) to Location(10),
                                                                ctox,
                                                                listOf(terminals[9]),
                                                            ),
                                                        ),
                                                    ),
                                                ),
                                            ),
                                            terminals[10],
                                        ),
                                    ),
                                    terminals[11],
                                    ParseTree.Branch(
                                        Location(12) to Location(13),
                                        btoc,
                                        listOf(
                                            ParseTree.Branch(
                                                Location(12) to Location(13),
                                                ctox,
                                                listOf(terminals[12]),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `parser continues on error`() {
        // A -> B ('+' B)*
        // B -> C | C '*' B
        // C -> X | '(' A ')'

        val atob =
            Production(
                SymbolAryt.A,
                CatSA(ReSA.atomic(SymbolAryt.B), StarSA(CatSA(ReSA.atomic(SymbolAryt.SUM), ReSA.atomic(SymbolAryt.B)))),
            )
        val btoc = Production(SymbolAryt.B, ReSA.atomic(SymbolAryt.C))
        val btoprod =
            Production(
                SymbolAryt.B,
                CatSA(ReSA.atomic(SymbolAryt.C), ReSA.atomic(SymbolAryt.PROD), ReSA.atomic(SymbolAryt.B)),
            )
        val ctox = Production(SymbolAryt.C, ReSA.atomic(SymbolAryt.X))
        val ctogroup =
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
                    2 via SymbolAryt.B to 1,
                ),
                mapOf(1 to atob),
            )
        val dfaB =
            SimpleDFA(
                0,
                mapOf(
                    0 via SymbolAryt.C to 1,
                    1 via SymbolAryt.PROD to 2,
                    2 via SymbolAryt.B to 3,
                ),
                mapOf(3 to btoprod, 1 to btoc),
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
                mapOf(4 to ctogroup, 1 to ctox),
            )
        val automata = mapOf(SymbolAryt.A to dfaA, SymbolAryt.B to dfaB, SymbolAryt.C to dfaC)

        val nextAction =
            mapOf(
                SymbolAryt.X to
                    mapOf(
                        DFAStateReference(0, dfaA) to SymbolAryt.B,
                        DFAStateReference(2, dfaA) to SymbolAryt.B,
                        DFAStateReference(0, dfaB) to SymbolAryt.C,
                        DFAStateReference(2, dfaB) to SymbolAryt.B,
                        DFAStateReference(0, dfaC) to SymbolAryt.X,
                        DFAStateReference(2, dfaC) to SymbolAryt.A,
                    ),
                SymbolAryt.SUM to
                    mapOf(
                        DFAStateReference(1, dfaA) to SymbolAryt.SUM,
                    ),
                SymbolAryt.PROD to
                    mapOf(
                        DFAStateReference(1, dfaB) to SymbolAryt.PROD,
                    ),
                SymbolAryt.LPAREN to
                    mapOf(
                        DFAStateReference(0, dfaA) to SymbolAryt.B,
                        DFAStateReference(2, dfaA) to SymbolAryt.B,
                        DFAStateReference(0, dfaB) to SymbolAryt.C,
                        DFAStateReference(2, dfaB) to SymbolAryt.B,
                        DFAStateReference(0, dfaC) to SymbolAryt.LPAREN,
                        DFAStateReference(2, dfaC) to SymbolAryt.A,
                    ),
                SymbolAryt.RPAREN to
                    mapOf(
                        DFAStateReference(3, dfaC) to SymbolAryt.RPAREN,
                    ),
            )

        // (x +) + x * x
        // Parses as () + x * x
        val terminals =
            listOf(
                SymbolAryt.LPAREN,
                SymbolAryt.X,
                SymbolAryt.SUM,
                SymbolAryt.RPAREN,
                SymbolAryt.SUM,
                SymbolAryt.X,
                SymbolAryt.PROD,
                SymbolAryt.X,
            ).mapIndexed { idx, symbol -> terminal(symbol, idx) }

        val parser =
            LLOneParser(
                nextAction,
                SymbolAryt.A,
                automata,
                listOf(SymbolAryt.RPAREN),
            )

        val tree = parser.process(terminals, diagnostics)
        assertThat(tree).isEqualTo(
            ParseTree.Branch(
                Location(0) to Location(8),
                atob,
                listOf(
                    ParseTree.Branch(
                        Location(0) to Location(3),
                        btoc,
                        listOf(
                            ParseTree.Branch(
                                Location(0) to Location(3),
                                ctogroup,
                                listOf(
                                    // Nothing in between parentheses because of errors
                                    terminals[0],
                                    terminals[3],
                                ),
                            ),
                        ),
                    ),
                    terminals[4],
                    ParseTree.Branch(
                        Location(5) to Location(8),
                        btoprod,
                        listOf(
                            ParseTree.Branch(
                                Location(5) to Location(6),
                                ctox,
                                listOf(terminals[5]),
                            ),
                            terminals[6],
                            ParseTree.Branch(
                                Location(7) to Location(8),
                                btoc,
                                listOf(
                                    ParseTree.Branch(
                                        Location(7) to Location(8),
                                        ctox,
                                        listOf(terminals[7]),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        verify(exactly = 1) {
            diagnostics.report(
                eq(ParserDiagnostics.UnexpectedToken("RPAREN", "A")),
                eq(Pair(Location(3), Location(4))),
            )
        }
    }

    @Test
    fun `parser throws on unrecoverable input`() {
        // A -> B ('+' B)*
        // B -> C | C '*' B
        // C -> X | '(' A ')'

        val atob =
            Production(
                SymbolAryt.A,
                CatSA(ReSA.atomic(SymbolAryt.B), StarSA(CatSA(ReSA.atomic(SymbolAryt.SUM), ReSA.atomic(SymbolAryt.B)))),
            )
        val btoc = Production(SymbolAryt.B, ReSA.atomic(SymbolAryt.C))
        val btoprod =
            Production(
                SymbolAryt.B,
                CatSA(ReSA.atomic(SymbolAryt.C), ReSA.atomic(SymbolAryt.PROD), ReSA.atomic(SymbolAryt.B)),
            )
        val ctox = Production(SymbolAryt.C, ReSA.atomic(SymbolAryt.X))
        val ctogroup =
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
                    2 via SymbolAryt.B to 1,
                ),
                mapOf(1 to atob),
            )
        val dfaB =
            SimpleDFA(
                0,
                mapOf(
                    0 via SymbolAryt.C to 1,
                    1 via SymbolAryt.PROD to 2,
                    2 via SymbolAryt.B to 3,
                ),
                mapOf(3 to btoprod, 1 to btoc),
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
                mapOf(4 to ctogroup, 1 to ctox),
            )
        val automata = mapOf(SymbolAryt.A to dfaA, SymbolAryt.B to dfaB, SymbolAryt.C to dfaC)

        val nextAction =
            mapOf(
                SymbolAryt.X to
                    mapOf(
                        DFAStateReference(0, dfaA) to SymbolAryt.B,
                        DFAStateReference(2, dfaA) to SymbolAryt.B,
                        DFAStateReference(0, dfaB) to SymbolAryt.C,
                        DFAStateReference(2, dfaB) to SymbolAryt.B,
                        DFAStateReference(0, dfaC) to SymbolAryt.X,
                        DFAStateReference(2, dfaC) to SymbolAryt.A,
                    ),
                SymbolAryt.SUM to
                    mapOf(
                        DFAStateReference(1, dfaA) to SymbolAryt.SUM,
                    ),
                SymbolAryt.PROD to
                    mapOf(
                        DFAStateReference(1, dfaB) to SymbolAryt.PROD,
                    ),
                SymbolAryt.LPAREN to
                    mapOf(
                        DFAStateReference(0, dfaA) to SymbolAryt.B,
                        DFAStateReference(2, dfaA) to SymbolAryt.B,
                        DFAStateReference(0, dfaB) to SymbolAryt.C,
                        DFAStateReference(2, dfaB) to SymbolAryt.B,
                        DFAStateReference(0, dfaC) to SymbolAryt.LPAREN,
                        DFAStateReference(2, dfaC) to SymbolAryt.A,
                    ),
                SymbolAryt.RPAREN to
                    mapOf(
                        DFAStateReference(3, dfaC) to SymbolAryt.RPAREN,
                    ),
            )

        // x + + x * x
        val terminals =
            listOf(
                SymbolAryt.X,
                SymbolAryt.SUM,
                SymbolAryt.SUM,
                SymbolAryt.X,
                SymbolAryt.PROD,
                SymbolAryt.X,
            ).mapIndexed { idx, symbol -> terminal(symbol, idx) }

        val parser =
            LLOneParser(
                nextAction,
                SymbolAryt.A,
                automata,
                listOf(SymbolAryt.RPAREN),
            )

        assertThatExceptionOfType(ParsingException::class.java).isThrownBy {
            parser.process(terminals, diagnostics)
        }

        verify(exactly = 1) {
            diagnostics.report(
                eq(ParserDiagnostics.UnexpectedToken("SUM", "A")),
                eq(Pair(Location(2), Location(3))),
            )
        }
    }

    @Test
    fun `parser reports error when finished before eof`() {
        // A -> B
        // B -> C
        // C -> X

        val atob = Production(Symbol.A, ReS.atomic(Symbol.B))
        val btoc = Production(Symbol.B, ReS.atomic(Symbol.C))
        val ctox = Production(Symbol.C, ReS.atomic(Symbol.X))
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.B to 1),
                mapOf(1 to atob),
            )
        val dfaB = SimpleDFA(0, mapOf(0 via Symbol.C to 1), mapOf(1 to btoc))
        val dfaC = SimpleDFA(0, mapOf(0 via Symbol.X to 1), mapOf(1 to ctox))
        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB, Symbol.C to dfaC)

        val nextAction =
            mapOf(
                Symbol.X to
                    mapOf(
                        DFAStateReference(0, dfaA) to Symbol.B,
                        DFAStateReference(0, dfaB) to Symbol.C,
                        DFAStateReference(0, dfaC) to Symbol.X,
                    ),
            )

        val terminals =
            listOf(
                terminal(Symbol.X, 0),
                terminal(Symbol.X, 1),
            )
        val parser =
            LLOneParser(
                nextAction,
                Symbol.A,
                automata,
                listOf(),
            )

        val tree = parser.process(terminals, diagnostics)
        assertThat(tree).isEqualTo(
            ParseTree.Branch(
                Location(0) to Location(1),
                atob,
                listOf(
                    ParseTree.Branch(
                        Location(0) to Location(1),
                        btoc,
                        listOf(
                            ParseTree.Branch(
                                Location(0) to Location(1),
                                ctox,
                                listOf(terminals[0]),
                            ),
                        ),
                    ),
                ),
            ),
        )

        verify(exactly = 1) {
            diagnostics.report(
                eq(ParserDiagnostics.UnableToContinueParsing("X")),
                eq(Pair(Location(1), Location(2))),
            )
        }
    }
}
