package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
import cacophony.semantic.types.BuiltinType
import cacophony.semantic.types.FunctionType
import cacophony.semantic.types.StructType
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

        val definitionTypes = mapOf(
            fDef to FunctionType(listOf(), FunctionType(listOf(), BuiltinType.IntegerType)),
            xDef to BuiltinType.IntegerType,
            gDef to FunctionType(listOf(), BuiltinType.IntegerType),
            yDef to BuiltinType.IntegerType,
        )

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
        val result = escapeAnalysis(ast, resolvedVariables, functionAnalysis, variablesMap, definitionTypes)

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

        val definitionTypes = mapOf(
            fDef to FunctionType(listOf(), FunctionType(listOf(), BuiltinType.IntegerType)),
            xDef to BuiltinType.IntegerType,
            gDef to FunctionType(listOf(), BuiltinType.IntegerType),
            hDef to FunctionType(listOf(), BuiltinType.IntegerType),
        )

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
        val result = escapeAnalysis(ast, resolvedVariables, functionAnalysis, variablesMap, definitionTypes)

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
     * EXPECTED: {g, x} escape
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

        val definitionTypes = mapOf(
            fDef to FunctionType(listOf(), StructType(mapOf("h" to FunctionType(listOf(), BuiltinType.IntegerType)))),
            xDef to BuiltinType.IntegerType,
            gDef to FunctionType(listOf(), BuiltinType.IntegerType),
            sDef to StructType(mapOf("h" to FunctionType(listOf(), BuiltinType.IntegerType))),
        )

        val resolvedVariables =
            mapOf(
                xUse to xDef,
                gUse to gDef,
                sUse to sDef
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
        val result = escapeAnalysis(ast, resolvedVariables, functionAnalysis, variablesMap, definitionTypes)

        // then
        assertThat(result).containsExactlyInAnyOrder(gVar, xVar)
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

        val definitionTypes = mapOf(
            hDef to FunctionType(listOf(), BuiltinType.IntegerType),
            fDef to FunctionType(listOf(), BuiltinType.UnitType),
            xDef to BuiltinType.IntegerType,
            gDef to FunctionType(listOf(), BuiltinType.IntegerType),
        )

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
        val result = escapeAnalysis(ast, resolvedVariables, functionAnalysis, variablesMap, definitionTypes)

        // then
        assertThat(result).containsExactlyInAnyOrder(gVar, xVar)
    }
}
