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
import cacophony.utils.StringInput
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.collections.listOf
import kotlin.collections.mapOf

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

    @MockK
    lateinit var diagnostics: Diagnostics

    @BeforeEach
    fun setUpMocks() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { diagnostics.report(any(), any(), any()) } just runs
    }

    @Test
    fun `parser returns correct tree for simple grammar`() {
//        A -> B
//        B -> C
//        C -> X

        val input = StringInput("x")
        val atob = Production(Symbol.A, AlgebraicRegex.atomic(Symbol.B))
        val btoc = Production(Symbol.B, AlgebraicRegex.atomic(Symbol.C))
        val ctox = Production(Symbol.C, AlgebraicRegex.atomic(Symbol.X))
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
                ParseTree.Leaf(Token(Symbol.X, "x", Location(0), Location(1))),
            )
        val parser =
            LLOneParser(
                nextAction,
                Symbol.A,
                automata,
                listOf(),
                input,
            )

        val tree = parser.process(terminals, diagnostics)
        assertEquals(
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
            tree,
        )
    }
}
