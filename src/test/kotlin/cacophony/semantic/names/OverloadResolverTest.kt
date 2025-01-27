package cacophony.semantic.names

import cacophony.*
import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.TypeCheckerDiagnostics
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.types.*
import cacophony.utils.Location
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OverloadResolverTest {
    private fun checkTypes(ast: AST): TypeCheckingResult {
        val nr = resolveNames(ast, diagnostics)
        return checkTypes(ast, nr, diagnostics)
    }

    @MockK
    lateinit var diagnostics: Diagnostics

    @BeforeEach
    fun setUpMocks() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { diagnostics.report(any(), any<Location>()) } just runs
    }

    @Test
    fun `ok - single overload`() {
        val def = functionDefinition("f", emptyList(), block())
        val use = variableUse("f")
        val ast =
            block(
                def,
                use,
            )
        val result = checkTypes(ast)
        assertThat(result.resolvedVariables[use]).isEqualTo(def)
        confirmVerified(diagnostics)
    }

    @Test
    fun `wrong - multiple overloads`() {
        val def1 = functionDefinition("f", emptyList(), block())
        val def2 = functionDefinition("f", listOf(arg("x")), block())
        val use = variableUse("f")
        val ast =
            block(
                def1,
                def2,
                use,
            )
        checkTypes(ast)
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TooManyOverloads,
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `ok - multiple overloads with hint 1`() {
        val def1 = functionDefinition("f", emptyList(), block())
        val def2 = functionDefinition("f", listOf(arg("x")), block())
        val use = variableUse("f")
        val ast =
            block(
                def1,
                def2,
                typedVariableDefinition("x", functionalType(emptyList(), basicType("Unit")), use),
            )
        val result = checkTypes(ast)
        assertThat(result.resolvedVariables[use]).isEqualTo(def1)
        confirmVerified(diagnostics)
    }

    @Test
    fun `ok - multiple overloads with hint 2`() {
        val def1 = functionDefinition("f", emptyList(), block())
        val def2 = functionDefinition("f", listOf(arg("x")), block())
        val use = variableUse("f")
        val ast =
            block(
                def1,
                def2,
                typedVariableDefinition("x", functionalType(listOf(basicType("Unit")), basicType("Unit")), use),
            )
        val result = checkTypes(ast)
        assertThat(result.resolvedVariables[use]).isEqualTo(def2)
        confirmVerified(diagnostics)
    }

    @Test
    fun `wrong - multiple overloads with not matching hint`() {
        val def1 = functionDefinition("f", emptyList(), block())
        val def2 = functionDefinition("f", listOf(arg("x")), block())
        val use = variableUse("f")
        val ast =
            block(
                def1,
                def2,
                typedVariableDefinition("x", functionalType(listOf(basicType("Unit"), basicType("Unit")), basicType("Unit")), use),
            )
        checkTypes(ast)
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TooFewOverloads,
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `wrong - assignment to static function`() {
        val def1 = functionDefinition("f", emptyList(), block())
        val def2 = functionDefinition("g", emptyList(), block())
        val ast =
            block(
                def1,
                def2,
                variableUse("f") assign variableUse("g"),
            )
        checkTypes(ast)
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.ExpectedLValueReference,
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `ok - nested resolution`() {
        // let h: [] -> () = if true then f else g;
        val defF0 = functionDefinition("f", emptyList(), block())
        val defF1 = functionDefinition("f", listOf(arg("x")), block())
        val defG0 = functionDefinition("g", emptyList(), block())
        val defG1 = functionDefinition("g", listOf(arg("x")), block())

        val useF = variableUse("f")
        val useG = variableUse("g")

        val ast =
            block(
                defF0,
                defF1,
                defG0,
                defG1,
                typedVariableDefinition(
                    "h",
                    functionalType(emptyList(), basicType("Unit")),
                    ifThenElse(lit(true), useF, useG),
                ),
            )
        val result = checkTypes(ast)
        assertThat(result.resolvedVariables[useF]).isEqualTo(defF0)
        assertThat(result.resolvedVariables[useG]).isEqualTo(defG0)
        confirmVerified(diagnostics)
    }
}
