package cacophony.parser

import cacophony.automata.SimpleDFA
import cacophony.automata.minimalization.via
import cacophony.grammars.DFAStateReference
import cacophony.grammars.ParseTree
import cacophony.grammars.Production
import cacophony.token.Token
import cacophony.utils.AlgebraicRegex
import cacophony.utils.Diagnostics
import cacophony.utils.Location
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.collections.listOf
import kotlin.collections.mapOf

typealias Re = AlgebraicRegex<LLOneParserTest.Symbol>
typealias Cat = AlgebraicRegex.ConcatenationRegex<LLOneParserTest.Symbol>
typealias Star = AlgebraicRegex.StarRegex<LLOneParserTest.Symbol>

class LLOneParserTest {
    enum class Symbol {
        // nonterminals
        A,
        B,
        C,

        // terminals
        X,
        Y,
        SUM,
        PROD,
        LPAREN,
        RPAREN,
    }

    private fun terminal(
        symbol: Symbol,
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
    fun `parser returns correct tree for simple grammar`() {
        // A -> B
        // B -> C
        // C -> X

        val atob = Production(Symbol.A, Re.atomic(Symbol.B))
        val btoc = Production(Symbol.B, Re.atomic(Symbol.C))
        val ctox = Production(Symbol.C, Re.atomic(Symbol.X))
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
                Symbol.A,
                Cat(Re.atomic(Symbol.B), Star(Cat(Re.atomic(Symbol.SUM), Re.atomic(Symbol.B)))),
            )
        val btoc = Production(Symbol.B, Re.atomic(Symbol.C))
        val btoprod =
            Production(
                Symbol.B,
                Cat(Re.atomic(Symbol.C), Re.atomic(Symbol.PROD), Re.atomic(Symbol.B)),
            )
        val ctox = Production(Symbol.C, Re.atomic(Symbol.X))
        val ctogroup =
            Production(
                Symbol.C,
                Cat(Re.atomic(Symbol.LPAREN), Re.atomic(Symbol.A), Re.atomic(Symbol.RPAREN)),
            )
        val dfaA =
            SimpleDFA(
                0,
                mapOf(
                    0 via Symbol.B to 1,
                    1 via Symbol.SUM to 2,
                    2 via Symbol.B to 1,
                ),
                mapOf(1 to atob),
            )
        val dfaB =
            SimpleDFA(
                0,
                mapOf(
                    0 via Symbol.C to 1,
                    1 via Symbol.PROD to 2,
                    2 via Symbol.B to 3,
                ),
                mapOf(3 to btoprod, 1 to btoc),
            )
        val dfaC =
            SimpleDFA(
                0,
                mapOf(
                    0 via Symbol.X to 1,
                    0 via Symbol.LPAREN to 2,
                    2 via Symbol.A to 3,
                    3 via Symbol.RPAREN to 4,
                ),
                mapOf(4 to ctogroup, 1 to ctox),
            )
        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB, Symbol.C to dfaC)

        val nextAction =
            mapOf(
                Symbol.X to
                    mapOf(
                        DFAStateReference(0, dfaA) to Symbol.B,
                        DFAStateReference(2, dfaA) to Symbol.B,
                        DFAStateReference(0, dfaB) to Symbol.C,
                        DFAStateReference(2, dfaB) to Symbol.B,
                        DFAStateReference(0, dfaC) to Symbol.X,
                        DFAStateReference(2, dfaC) to Symbol.A,
                    ),
                Symbol.SUM to
                    mapOf(
                        DFAStateReference(1, dfaA) to Symbol.SUM,
                    ),
                Symbol.PROD to
                    mapOf(
                        DFAStateReference(1, dfaB) to Symbol.PROD,
                    ),
                Symbol.LPAREN to
                    mapOf(
                        DFAStateReference(0, dfaA) to Symbol.B,
                        DFAStateReference(2, dfaA) to Symbol.B,
                        DFAStateReference(0, dfaB) to Symbol.C,
                        DFAStateReference(2, dfaB) to Symbol.B,
                        DFAStateReference(0, dfaC) to Symbol.LPAREN,
                        DFAStateReference(2, dfaC) to Symbol.A,
                    ),
                Symbol.RPAREN to
                    mapOf(
                        DFAStateReference(3, dfaC) to Symbol.RPAREN,
                    ),
            )

        // x + x * (x + x + x) * x
        val terminals =
            listOf(
                Symbol.X,
                Symbol.SUM,
                Symbol.X,
                Symbol.PROD,
                Symbol.LPAREN,
                Symbol.X,
                Symbol.SUM,
                Symbol.X,
                Symbol.SUM,
                Symbol.X,
                Symbol.RPAREN,
                Symbol.PROD,
                Symbol.X,
            ).mapIndexed { idx, symbol -> terminal(symbol, idx) }

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
                Symbol.A,
                Cat(Re.atomic(Symbol.B), Star(Cat(Re.atomic(Symbol.SUM), Re.atomic(Symbol.B)))),
            )
        val btoc = Production(Symbol.B, Re.atomic(Symbol.C))
        val btoprod =
            Production(
                Symbol.B,
                Cat(Re.atomic(Symbol.C), Re.atomic(Symbol.PROD), Re.atomic(Symbol.B)),
            )
        val ctox = Production(Symbol.C, Re.atomic(Symbol.X))
        val ctogroup =
            Production(
                Symbol.C,
                Cat(Re.atomic(Symbol.LPAREN), Re.atomic(Symbol.A), Re.atomic(Symbol.RPAREN)),
            )
        val dfaA =
            SimpleDFA(
                0,
                mapOf(
                    0 via Symbol.B to 1,
                    1 via Symbol.SUM to 2,
                    2 via Symbol.B to 1,
                ),
                mapOf(1 to atob),
            )
        val dfaB =
            SimpleDFA(
                0,
                mapOf(
                    0 via Symbol.C to 1,
                    1 via Symbol.PROD to 2,
                    2 via Symbol.B to 3,
                ),
                mapOf(3 to btoprod, 1 to btoc),
            )
        val dfaC =
            SimpleDFA(
                0,
                mapOf(
                    0 via Symbol.X to 1,
                    0 via Symbol.LPAREN to 2,
                    2 via Symbol.A to 3,
                    3 via Symbol.RPAREN to 4,
                ),
                mapOf(4 to ctogroup, 1 to ctox),
            )
        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB, Symbol.C to dfaC)

        val nextAction =
            mapOf(
                Symbol.X to
                    mapOf(
                        DFAStateReference(0, dfaA) to Symbol.B,
                        DFAStateReference(2, dfaA) to Symbol.B,
                        DFAStateReference(0, dfaB) to Symbol.C,
                        DFAStateReference(2, dfaB) to Symbol.B,
                        DFAStateReference(0, dfaC) to Symbol.X,
                        DFAStateReference(2, dfaC) to Symbol.A,
                    ),
                Symbol.SUM to
                    mapOf(
                        DFAStateReference(1, dfaA) to Symbol.SUM,
                    ),
                Symbol.PROD to
                    mapOf(
                        DFAStateReference(1, dfaB) to Symbol.PROD,
                    ),
                Symbol.LPAREN to
                    mapOf(
                        DFAStateReference(0, dfaA) to Symbol.B,
                        DFAStateReference(2, dfaA) to Symbol.B,
                        DFAStateReference(0, dfaB) to Symbol.C,
                        DFAStateReference(2, dfaB) to Symbol.B,
                        DFAStateReference(0, dfaC) to Symbol.LPAREN,
                        DFAStateReference(2, dfaC) to Symbol.A,
                    ),
                Symbol.RPAREN to
                    mapOf(
                        DFAStateReference(3, dfaC) to Symbol.RPAREN,
                    ),
            )

        // (x +) + x * x
        // Parses as () + x * x
        val terminals =
            listOf(
                Symbol.LPAREN,
                Symbol.X,
                Symbol.SUM,
                Symbol.RPAREN,
                Symbol.SUM,
                Symbol.X,
                Symbol.PROD,
                Symbol.X,
            ).mapIndexed { idx, symbol -> terminal(symbol, idx) }

        val parser =
            LLOneParser(
                nextAction,
                Symbol.A,
                automata,
                listOf(Symbol.RPAREN),
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
                                listOf( // Nothing in between parentheses because of errors
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
                eq("Unexpected token RPAREN while parsing A"),
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
                Symbol.A,
                Cat(Re.atomic(Symbol.B), Star(Cat(Re.atomic(Symbol.SUM), Re.atomic(Symbol.B)))),
            )
        val btoc = Production(Symbol.B, Re.atomic(Symbol.C))
        val btoprod =
            Production(
                Symbol.B,
                Cat(Re.atomic(Symbol.C), Re.atomic(Symbol.PROD), Re.atomic(Symbol.B)),
            )
        val ctox = Production(Symbol.C, Re.atomic(Symbol.X))
        val ctogroup =
            Production(
                Symbol.C,
                Cat(Re.atomic(Symbol.LPAREN), Re.atomic(Symbol.A), Re.atomic(Symbol.RPAREN)),
            )
        val dfaA =
            SimpleDFA(
                0,
                mapOf(
                    0 via Symbol.B to 1,
                    1 via Symbol.SUM to 2,
                    2 via Symbol.B to 1,
                ),
                mapOf(1 to atob),
            )
        val dfaB =
            SimpleDFA(
                0,
                mapOf(
                    0 via Symbol.C to 1,
                    1 via Symbol.PROD to 2,
                    2 via Symbol.B to 3,
                ),
                mapOf(3 to btoprod, 1 to btoc),
            )
        val dfaC =
            SimpleDFA(
                0,
                mapOf(
                    0 via Symbol.X to 1,
                    0 via Symbol.LPAREN to 2,
                    2 via Symbol.A to 3,
                    3 via Symbol.RPAREN to 4,
                ),
                mapOf(4 to ctogroup, 1 to ctox),
            )
        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB, Symbol.C to dfaC)

        val nextAction =
            mapOf(
                Symbol.X to
                    mapOf(
                        DFAStateReference(0, dfaA) to Symbol.B,
                        DFAStateReference(2, dfaA) to Symbol.B,
                        DFAStateReference(0, dfaB) to Symbol.C,
                        DFAStateReference(2, dfaB) to Symbol.B,
                        DFAStateReference(0, dfaC) to Symbol.X,
                        DFAStateReference(2, dfaC) to Symbol.A,
                    ),
                Symbol.SUM to
                    mapOf(
                        DFAStateReference(1, dfaA) to Symbol.SUM,
                    ),
                Symbol.PROD to
                    mapOf(
                        DFAStateReference(1, dfaB) to Symbol.PROD,
                    ),
                Symbol.LPAREN to
                    mapOf(
                        DFAStateReference(0, dfaA) to Symbol.B,
                        DFAStateReference(2, dfaA) to Symbol.B,
                        DFAStateReference(0, dfaB) to Symbol.C,
                        DFAStateReference(2, dfaB) to Symbol.B,
                        DFAStateReference(0, dfaC) to Symbol.LPAREN,
                        DFAStateReference(2, dfaC) to Symbol.A,
                    ),
                Symbol.RPAREN to
                    mapOf(
                        DFAStateReference(3, dfaC) to Symbol.RPAREN,
                    ),
            )

        // x + + x * x
        val terminals =
            listOf(
                Symbol.X,
                Symbol.SUM,
                Symbol.SUM,
                Symbol.X,
                Symbol.PROD,
                Symbol.X,
            ).mapIndexed { idx, symbol -> terminal(symbol, idx) }

        val parser =
            LLOneParser(
                nextAction,
                Symbol.A,
                automata,
                listOf(Symbol.RPAREN),
            )

        assertThatExceptionOfType(ParsingErrorException::class.java).isThrownBy({
            parser.process(terminals, diagnostics)
        })

        verify(exactly = 1) {
            diagnostics.report(
                eq("Unexpected token SUM while parsing A"),
                eq(Pair(Location(2), Location(3))),
            )
        }
    }
}
