package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
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
        val xDef = variableDeclaration("x", lit(5))
        val yDef = variableDeclaration("y", lit(10))
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

    /*
     * let f = [] -> [] -> Int => (
     *   let x = 5;
     *   let g = [] -> Int => x;
     *   let h = g;
     *   h
     * )
     * EXPECTED: {h, g, x} escape
     */
    @Test
    fun `detects variables escaping transitively`() {
        // given
        val xDef = variableDeclaration("x", lit(5))
        val xUse = variableUse("x")
        val gUse = variableUse("g")
        val hUse = variableUse("h")
        val gDef = functionDefinition("g", listOf(), xUse)
        val hDef = variableDeclaration("h", gUse)
        val fDef = functionDefinition("f", listOf(), block(xDef, gDef, hDef, hUse))
        val ast = block(fDef)

        val resolvedVariables =
            mapOf(
                xUse to xDef,
                gUse to gDef,
                hUse to hDef,
            )

        val xVar = Variable.PrimitiveVariable()
        val hVar = Variable.PrimitiveVariable()
        val gVar = Variable.PrimitiveVariable()
        val fVar = Variable.PrimitiveVariable()

        val xAVar = AnalyzedVariable(xVar, fDef, VariableUseType.READ_WRITE)
        val gAVar = AnalyzedVariable(gVar, fDef, VariableUseType.READ_WRITE)
        val hAVar = AnalyzedVariable(hVar, fDef, VariableUseType.READ_WRITE)

        val gA = AnalyzedFunction(gDef, null, setOf(xAVar), mutableSetOf(), 1, setOf())
        val fA = AnalyzedFunction(fDef, null, setOf(xAVar, gAVar, hAVar), mutableSetOf(), 0, setOf(xVar))

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
                    fDef to fVar,
                    gDef to gVar,
                    hDef to hVar,
                ),
            )

        // when
        val result = escapeAnalysis(ast, resolvedVariables, functionAnalysis, variablesMap)

        // then
        assertThat(result).containsExactlyInAnyOrder(gVar, hVar, xVar)
    }

    /*
     * let f = [] -> {h: [] -> Int} => (
     *   let x = 5;
     *   let g = [] -> Int => x;
     *   let s = {h: g};
     *   s
     * )
     * EXPECTED: {s, g, x} escape
     */
    @Test
    fun `detects variables escaping via return of struct`() {
        // given
        val xDef = variableDeclaration("x", lit(5))
        val xUse = variableUse("x")
        val gUse = variableUse("g")
        val gDef = functionDefinition("g", listOf(), xUse)
        val sUse = variableUse("s")
        val sDef = variableDeclaration("s", structDeclaration(structField("h") to gUse))
        val fDef = functionDefinition("f", listOf(), block(xDef, gDef, sDef, sUse))
        val ast = block(fDef)

        val resolvedVariables =
            mapOf(
                xUse to xDef,
                gUse to gDef,
                sUse to sDef,
            )

        val xVar = Variable.PrimitiveVariable()
        val gVar = Variable.PrimitiveVariable()
        val sVar = Variable.PrimitiveVariable()
        val fVar = Variable.PrimitiveVariable()

        val xAVar = AnalyzedVariable(xVar, fDef, VariableUseType.READ_WRITE)
        val gAVar = AnalyzedVariable(gVar, fDef, VariableUseType.READ_WRITE)
        val sAVar = AnalyzedVariable(sVar, fDef, VariableUseType.READ_WRITE)

        val gA = AnalyzedFunction(gDef, null, setOf(xAVar), mutableSetOf(), 1, setOf())
        val fA = AnalyzedFunction(fDef, null, setOf(xAVar, gAVar, sAVar), mutableSetOf(), 0, setOf(xVar))

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
                    sDef to sVar,
                    fDef to fVar,
                    gDef to gVar,
                ),
            )

        // when
        val result = escapeAnalysis(ast, resolvedVariables, functionAnalysis, variablesMap)

        // then
        assertThat(result).containsExactlyInAnyOrder(sVar, gVar, xVar)
    }

    /*
     * let h = [] -> Int => 10;
     * let f = [] -> () => (
     *   let x = 5;
     *   let g = [] -> Int => x;
     *   h = g;
     * )
     * EXPECTED: {g, x} escape
     */
    @Test
    fun `detects variables escaping via assignment to variable in upper scope`() {
        // given
        val hDef = functionDefinition("h", listOf(), lit(10))
        val hUse = variableUse("h")
        val xDef = variableDeclaration("x", lit(5))
        val xUse = variableUse("x")
        val gDef = functionDefinition("g", listOf(), xUse)
        val gUse = variableUse("g")
        val fDef = functionDefinition("f", listOf(), block(xDef, gDef, hUse assign gUse))
        val ast = block(hDef, fDef)

        val resolvedVariables =
            mapOf(
                xUse to xDef,
                gUse to gDef,
                hUse to hDef,
            )

        val xVar = Variable.PrimitiveVariable()
        val hVar = Variable.PrimitiveVariable()
        val gVar = Variable.PrimitiveVariable()
        val fVar = Variable.PrimitiveVariable()

        val xAVar = AnalyzedVariable(xVar, fDef, VariableUseType.READ_WRITE)
        val gAVar = AnalyzedVariable(gVar, fDef, VariableUseType.READ_WRITE)

        val hA = AnalyzedFunction(hDef, null, setOf(), mutableSetOf(), 0, setOf())
        val gA = AnalyzedFunction(gDef, null, setOf(xAVar), mutableSetOf(), 1, setOf())
        val fA = AnalyzedFunction(fDef, null, setOf(xAVar, gAVar), mutableSetOf(), 0, setOf(xVar))

        val functionAnalysis =
            mapOf(
                gDef to gA,
                fDef to fA,
                hDef to hA,
            )

        val variablesMap =
            VariablesMap(
                mapOf(),
                mapOf(
                    xDef to xVar,
                    hDef to hVar,
                    fDef to fVar,
                    gDef to gVar,
                ),
            )

        // when
        val result = escapeAnalysis(ast, resolvedVariables, functionAnalysis, variablesMap)

        // then
        assertThat(result).containsExactlyInAnyOrder(gVar, xVar)
    }

    /*
     * let f = [] -> [] -> Int => (
     *   let x = 10;
     *   [] -> (let y = 15; x)
     * )
     * EXPECTED: {x} escapes
     */
    @Test
    fun `detects variables escaping via returned lambda expression`() {
        // given
        val xDef = variableDeclaration("x", lit(5))
        val yDef = variableDeclaration("y", lit(15))
        val xUse = variableUse("x")
        val lambda = lambda(listOf(), block(yDef, xUse))
        val fDef = functionDefinition("f", listOf(), block(xDef, lambda))
        val ast = block(fDef)

        val resolvedVariables =
            mapOf(
                xUse to xDef,
            )

        val xVar = Variable.PrimitiveVariable()
        val yVar = Variable.PrimitiveVariable()
        val fVar = Variable.PrimitiveVariable()

        val xAVar = AnalyzedVariable(xVar, fDef, VariableUseType.READ_WRITE)
        val yAVar = AnalyzedVariable(yVar, fDef, VariableUseType.READ_WRITE)

        val fA = AnalyzedFunction(fDef, null, setOf(xAVar), mutableSetOf(), 0, setOf(xVar))

        val functionAnalysis =
            mapOf(
                fDef to fA,
            )

        val variablesMap =
            VariablesMap(
                mapOf(),
                mapOf(
                    xDef to xVar,
                    yDef to yVar,
                    fDef to fVar,
                ),
            )

        // when
        val result = escapeAnalysis(ast, resolvedVariables, functionAnalysis, variablesMap)

        // then
        assertThat(result).containsExactlyInAnyOrder(xVar)
    }
}
