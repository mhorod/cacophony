package cacophony.semantic.analysis

import cacophony.*
import cacophony.diagnostics.Diagnostics
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
        val fDef = unitFunctionDefinition("f", empty())
        val fUse = variableUse("f")
        val gDef =
            unitFunctionDefinition(
                "g",
                call(fUse),
            )

        val ast = block(fDef, gDef)
        val resolvedVariables = mapOf(fUse to fDef)

        assertThat(generateCallGraph(ast, resolvedVariables, diagnostics)).isEqualTo(
            mapOf(
                gDef to setOf(fDef),
            ),
        )
    }

    @Test
    fun `finds deeply nested call`() {
        // let f = [] -> B => (); let g = [] -> C => (((((f[])))))
        val fDef = unitFunctionDefinition("f", empty())
        val fUse = variableUse("f")
        val gDef =
            unitFunctionDefinition(
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

        assertThat(generateCallGraph(ast, resolvedVariables, diagnostics)).isEqualTo(
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
            unitFunctionDefinition(
                "f",
                call(fUse),
            )

        val ast = block(fDef)
        val resolvedVariables = mapOf(fUse to fDef)

        assertThat(generateCallGraph(ast, resolvedVariables, diagnostics)).isEqualTo(
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
            typedFunctionDefinition("g", null, listOf(), basicType("C"), call(fUseInG))
        val fDef =
            typedFunctionDefinition("f", null, listOf(), basicType("B"), block(gDef, FunctionCall(loc(7, 8), gUse, listOf())))

        val hDef =
            typedFunctionDefinition("h", null, listOf(), basicType("B"), call(fUseInH))

        val ast = block(fDef, hDef)
        val resolvedVariables = mapOf(fUseInG to fDef, fUseInH to fDef, gUse to gDef)

        assertThat(generateCallGraph(ast, resolvedVariables, diagnostics)).isEqualTo(
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
            unitFunctionDefinition(
                "f",
                block(gDef, gUse),
            )

        val ast = block(fDef)
        val resolvedVariables = mapOf(fUse to fDef, gUse to gDef)

        assertThat(generateCallGraph(ast, resolvedVariables, diagnostics)).isEqualTo(
            mapOf(fDef to setOf(fDef)),
        )
    }

    @Test
    fun `finds calls in struct`() {
        // let f = [] -> B => (
        //    let g = [] => {res = f[]};
        //    g[]
        // )
        val fUse = variableUse("f")
        val gUse = variableUse("g")

        val gDef = unitFunctionDefinition("g", structDeclaration(structField("res") to call(fUse)))
        val fDef = unitFunctionDefinition("f", block(gDef, call(gUse)))

        val ast = block(fDef)
        val resolvedVariables = mapOf(fUse to fDef, gUse to gDef)

        assertThat(generateCallGraph(ast, resolvedVariables, diagnostics)).isEqualTo(
            mapOf(gDef to setOf(fDef), fDef to setOf(gDef)),
        )
    }
}
