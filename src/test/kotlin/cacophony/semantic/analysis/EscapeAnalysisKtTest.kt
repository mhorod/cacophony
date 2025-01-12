package cacophony.semantic.analysis

import cacophony.block
import cacophony.controlflow.Variable
import cacophony.functionDefinition
import cacophony.variableDeclaration
import cacophony.variableUse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EscapeAnalysisKtTest {
    /*
     * let f = [] -> [] -> Int => (
     *   let x = 5;
     *   let g = [] -> Int => (
     *     let y = 10;
     *     x
     *   );
     *   g
     * )
     * EXPECTED: {g, x} escape
     */
    @Test
    fun `detects variables escaping via return expression`() {
        // given
        val xDef = variableDeclaration("x")
        val yDef = variableDeclaration("y")
        val xUse = variableUse("x")
        val gUse = variableUse("g")
        val gDef = functionDefinition("g", listOf(), block(yDef, xUse))
        val fDef = functionDefinition("f", listOf(), block(xDef, gDef, gUse))
        val ast = block(fDef)

        val resolvedVariables =
            mapOf(
                xUse to xDef,
                gUse to gDef,
            )

        val xVar = Variable.PrimitiveVariable()
        val yVar = Variable.PrimitiveVariable()
        val gVar = Variable.PrimitiveVariable()
        val fVar = Variable.PrimitiveVariable()

        val xAVar = AnalyzedVariable(xVar, fDef, VariableUseType.READ_WRITE)
        val yAVar = AnalyzedVariable(yVar, gDef, VariableUseType.READ_WRITE)
        val gAVar = AnalyzedVariable(gVar, fDef, VariableUseType.READ_WRITE)

        val gA = AnalyzedFunction(gDef, null, setOf(xAVar, yAVar), mutableSetOf(), 1, setOf())
        val fA = AnalyzedFunction(fDef, null, setOf(xAVar, gAVar), mutableSetOf(), 0, setOf(xVar))

        val functionAnalysis =
            mapOf(
                gDef to gA,
                fDef to fA,
            )

        val variablesMap =
            VariablesMap(
                mapOf(),
                mapOf(
                    xDef to xVar,
                    yDef to yVar,
                    fDef to fVar,
                    gDef to gVar,
                ),
            )

        // when
        val result = escapeAnalysis(ast, resolvedVariables, functionAnalysis, variablesMap)

        // then
        assertThat(result).containsExactlyInAnyOrder(gVar, xVar)
    }
}
