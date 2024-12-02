package cacophony.semantic.analysis

import cacophony.*
import cacophony.diagnostics.Diagnostics
import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.FunctionCall
import cacophony.utils.Location
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.collections.setOf
import cacophony.semantic.syntaxtree.Type as ASTType

class CallGraphTest {
    @MockK
    lateinit var diagnostics: Diagnostics

    private fun loc(l: Int, r: Int) = Pair(Location(l), Location(r))

    @BeforeEach
    fun setUpMocks() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { diagnostics.report(any(), any<Pair<Location, Location>>()) } just runs
    }

    @Test
    fun `finds basic call`() {
        // let f = [] -> B => (); let g = [] -> C => f[]
        val fDef = functionDeclaration("f", empty())
        val fUse = variableUse("f")
        val gDef =
            functionDeclaration(
                "g",
                call(fUse),
            )

        val ast = block(fDef, gDef)
        val resolvedVariables = mapOf(fUse to fDef)

        assertThat(generateCallGraph(ast, diagnostics, resolvedVariables)).isEqualTo(
            mapOf(
                gDef to setOf(fDef),
            ),
        )
    }

    @Test
    fun `finds deeply nested call`() {
        // let f = [] -> B => (); let g = [] -> C => (((((f[])))))
        val fDef = functionDeclaration("f", empty())
        val fUse = variableUse("f")
        val gDef =
            functionDeclaration(
                "g",
                block(
                    block(
                        block(
                            block(
                                block(
                                    call(fUse),
                                ),
                            ),
                        ),
                    ),
                ),
            )

        val ast =
            block(fDef, gDef)
        val resolvedVariables = mapOf(fUse to fDef)

        assertThat(generateCallGraph(ast, diagnostics, resolvedVariables)).isEqualTo(
            mapOf(
                gDef to setOf(fDef),
            ),
        )
    }

    @Test
    fun `finds recursion`() {
        // let f = [] -> B => f[]
        val fUse = variableUse("f")
        val fDef =
            functionDeclaration(
                "f",
                call(fUse),
            )

        val ast = block(fDef)
        val resolvedVariables = mapOf(fUse to fDef)

        assertThat(generateCallGraph(ast, diagnostics, resolvedVariables)).isEqualTo(
            mapOf(
                fDef to setOf(fDef),
            ),
        )
    }

    @Test
    fun `does not find indirect calls`() {
        // let f = [] -> B => (
        //    let g = [] -> B => f[];
        //    g[]
        // );
        // let h = [] -> B => f[]
        val fUseInG = variableUse("f")
        val fUseInH = variableUse("f")
        val gUse = variableUse("g")

        val gDef =
            Definition.FunctionDeclaration(
                loc(4, 8),
                "g",
                null,
                listOf(),
                ASTType.Basic(loc(7, 8), "C"),
                FunctionCall(loc(8, 9), fUseInG, listOf()),
            )
        val fDef =
            Definition.FunctionDeclaration(
                loc(0, 4),
                "f",
                null,
                listOf(),
                ASTType.Basic(loc(3, 4), "B"),
                Block(
                    loc(0, 2),
                    listOf(
                        gDef,
                        FunctionCall(loc(7, 8), gUse, listOf()),
                    ),
                ),
            )

        val hDef =
            Definition.FunctionDeclaration(
                loc(10, 20),
                "h",
                null,
                listOf(),
                ASTType.Basic(loc(10, 13), "B"),
                FunctionCall(loc(14, 20), fUseInH, listOf()),
            )

        val ast =
            Block(
                loc(0, 9),
                listOf(fDef, hDef),
            )
        val resolvedVariables = mapOf(fUseInG to fDef, fUseInH to fDef, gUse to gDef)

        assertThat(generateCallGraph(ast, diagnostics, resolvedVariables)).isEqualTo(
            mapOf(
                fDef to setOf(gDef),
                gDef to setOf(fDef),
                hDef to setOf(fDef),
            ),
        )
    }

    @Test
    fun `finds calls in variable declarations`() {
        // let f = [] -> B => (
        //    let g = f[];
        //    g
        // )
        val fUse = variableUse("f")
        val gUse = variableUse("g")

        val gDef =
            variableDeclaration(
                "g",
                call(fUse),
            )
        val fDef =
            functionDeclaration(
                "f",
                block(gDef, gUse),
            )

        val ast = block(fDef)
        val resolvedVariables = mapOf(fUse to fDef, gUse to gDef)

        assertThat(generateCallGraph(ast, diagnostics, resolvedVariables)).isEqualTo(
            mapOf(fDef to setOf(fDef)),
        )
    }
}
