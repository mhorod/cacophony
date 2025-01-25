package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
import cacophony.semantic.types.BuiltinType
import cacophony.semantic.types.FunctionType
import cacophony.semantic.types.StructType
import cacophony.semantic.types.TypeCheckingResult
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

        val types =
            TypeCheckingResult(
                mapOf(),
                mapOf(
                    xDef to BuiltinType.IntegerType,
                    yDef to BuiltinType.IntegerType,
                    fDef to FunctionType(listOf(), FunctionType(listOf(), BuiltinType.IntegerType)),
                    gDef to FunctionType(listOf(), BuiltinType.IntegerType),
                ),
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

        val xAVar = AnalyzedVariable(xVar, fDef.value, VariableUseType.READ_WRITE)
        val yAVar = AnalyzedVariable(yVar, gDef.value, VariableUseType.READ_WRITE)
        val gAVar = AnalyzedVariable(gVar, fDef.value, VariableUseType.READ_WRITE)

        val gA = AnalyzedFunction(gDef.value, null, setOf(xAVar, yAVar), listOf(), mutableSetOf(), 1, setOf())
        val fA = AnalyzedFunction(fDef.value, null, setOf(xAVar, gAVar), listOf(), mutableSetOf(), 0, setOf(xVar))

        val functionAnalysis =
            mapOf(
                gDef.value to gA,
                fDef.value to fA,
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
        val result =
            escapeAnalysis(
                ast,
                resolvedVariables,
                functionAnalysis,
                variablesMap,
                types,
                emptyMap(),
            )

        // then
        assertThat(result.escapedVariables).containsExactlyInAnyOrder(gVar, xVar)
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

        val types =
            TypeCheckingResult(
                mapOf(),
                mapOf(
                    xDef to BuiltinType.IntegerType,
                    hDef to FunctionType(listOf(), FunctionType(listOf(), BuiltinType.IntegerType)),
                    fDef to FunctionType(listOf(), FunctionType(listOf(), BuiltinType.IntegerType)),
                    gDef to FunctionType(listOf(), BuiltinType.IntegerType),
                ),
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

        val xAVar = AnalyzedVariable(xVar, fDef.value, VariableUseType.READ_WRITE)
        val gAVar = AnalyzedVariable(gVar, fDef.value, VariableUseType.READ_WRITE)
        val hAVar = AnalyzedVariable(hVar, fDef.value, VariableUseType.READ_WRITE)

        val gA = AnalyzedFunction(gDef.value, null, setOf(xAVar), listOf(), mutableSetOf(), 1, setOf())
        val fA = AnalyzedFunction(fDef.value, null, setOf(xAVar, gAVar, hAVar), listOf(), mutableSetOf(), 0, setOf(xVar))

        val functionAnalysis =
            mapOf(
                gDef.value to gA,
                fDef.value to fA,
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
        val result =
            escapeAnalysis(
                ast,
                resolvedVariables,
                functionAnalysis,
                variablesMap,
                types,
                emptyMap(),
            )

        // then
        assertThat(result.escapedVariables).containsExactlyInAnyOrder(gVar, hVar, xVar)
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

        val sType = StructType(mapOf("h" to FunctionType(listOf(), BuiltinType.IntegerType)))
        val types =
            TypeCheckingResult(
                mapOf(),
                mapOf(
                    xDef to BuiltinType.IntegerType,
                    sDef to sType,
                    fDef to
                        FunctionType(
                            listOf(),
                            FunctionType(listOf(), sType),
                        ),
                    gDef to FunctionType(listOf(), BuiltinType.IntegerType),
                ),
            )

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

        val xAVar = AnalyzedVariable(xVar, fDef.value, VariableUseType.READ_WRITE)
        val gAVar = AnalyzedVariable(gVar, fDef.value, VariableUseType.READ_WRITE)
        val sAVar = AnalyzedVariable(sVar, fDef.value, VariableUseType.READ_WRITE)

        val gA = AnalyzedFunction(gDef.value, null, setOf(xAVar), listOf(), mutableSetOf(), 1, setOf())
        val fA = AnalyzedFunction(fDef.value, null, setOf(xAVar, gAVar, sAVar), listOf(), mutableSetOf(), 0, setOf(xVar))

        val functionAnalysis =
            mapOf(
                gDef.value to gA,
                fDef.value to fA,
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
        val result =
            escapeAnalysis(
                ast,
                resolvedVariables,
                functionAnalysis,
                variablesMap,
                types,
                emptyMap(),
            )

        // then
        assertThat(result.escapedVariables).containsExactlyInAnyOrder(sVar, gVar, xVar)
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

        val types =
            TypeCheckingResult(
                mapOf(
                    hUse to FunctionType(listOf(), BuiltinType.IntegerType),
                    gUse to FunctionType(listOf(), BuiltinType.IntegerType),
                ),
                mapOf(
                    xDef to BuiltinType.IntegerType,
                    hDef to FunctionType(listOf(), BuiltinType.IntegerType),
                    fDef to FunctionType(listOf(), BuiltinType.UnitType),
                    gDef to FunctionType(listOf(), BuiltinType.IntegerType),
                ),
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

        val xAVar = AnalyzedVariable(xVar, fDef.value, VariableUseType.READ_WRITE)
        val gAVar = AnalyzedVariable(gVar, fDef.value, VariableUseType.READ_WRITE)

        val hA = AnalyzedFunction(hDef.value, null, setOf(), listOf(), mutableSetOf(), 0, setOf())
        val gA = AnalyzedFunction(gDef.value, null, setOf(xAVar), listOf(), mutableSetOf(), 1, setOf())
        val fA = AnalyzedFunction(fDef.value, null, setOf(xAVar, gAVar), listOf(), mutableSetOf(), 0, setOf(xVar))

        val functionAnalysis =
            mapOf(
                gDef.value to gA,
                fDef.value to fA,
                hDef.value to hA,
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
        val result =
            escapeAnalysis(
                ast,
                resolvedVariables,
                functionAnalysis,
                variablesMap,
                types,
                emptyMap(),
            )

        // then
        assertThat(result.escapedVariables).containsExactlyInAnyOrder(gVar, xVar)
    }

    /*
     * let f = [] -> [] -> Int => (
     *   let x = 10;
     *   [] -> Int => (let y = 15; x)
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

        val types =
            TypeCheckingResult(
                mapOf(
                    lambda to FunctionType(listOf(), BuiltinType.IntegerType),
                ),
                mapOf(
                    yDef to BuiltinType.IntegerType,
                    xDef to BuiltinType.IntegerType,
                    fDef to FunctionType(listOf(), FunctionType(listOf(), BuiltinType.IntegerType)),
                ),
            )

        val resolvedVariables =
            mapOf(
                xUse to xDef,
            )

        val xVar = Variable.PrimitiveVariable()
        val yVar = Variable.PrimitiveVariable()
        val fVar = Variable.PrimitiveVariable()

        val xAVar = AnalyzedVariable(xVar, fDef.value, VariableUseType.READ_WRITE)

        val fA = AnalyzedFunction(fDef.value, null, setOf(xAVar), listOf(), mutableSetOf(), 0, setOf(xVar))

        val functionAnalysis =
            mapOf(
                fDef.value to fA,
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
        val result =
            escapeAnalysis(
                ast,
                resolvedVariables,
                functionAnalysis,
                variablesMap,
                types,
                emptyMap(),
            )

        // then
        assertThat(result.escapedVariables).containsExactlyInAnyOrder(xVar)
    }

    /*
     * let f = [x: Int] -> Int => x;
     * EXPECTED: {} escape
     */
    @Test
    fun `arguments do not escape if not necessary`() {
        // given
        val xDef = intArg("x")
        val xUse = variableUse("x")
        val fDef = functionDefinition("f", listOf(xDef), xUse)
        val ast = block(fDef)

        val types =
            TypeCheckingResult(
                mapOf(),
                mapOf(
                    fDef to FunctionType(listOf(BuiltinType.IntegerType), BuiltinType.IntegerType),
                ),
            )

        val resolvedVariables = mapOf(xUse to xDef)

        val xVar = Variable.PrimitiveVariable()

        val xAVar = AnalyzedVariable(xVar, fDef.value, VariableUseType.READ_WRITE)

        val fA = AnalyzedFunction(fDef.value, null, setOf(xAVar), listOf(), mutableSetOf(), 0, emptySet())

        val functionAnalysis = mapOf(fDef.value to fA)

        val variablesMap =
            VariablesMap(
                mapOf(),
                mapOf(xDef to xVar),
            )

        // when
        val result =
            escapeAnalysis(
                ast,
                resolvedVariables,
                functionAnalysis,
                variablesMap,
                types,
                emptyMap(),
            )

        // then
        assertThat(result.escapedVariables).isEmpty()
    }

    /*
     * let f = [] -> Int => (
     *   let x = 10;
     *   x
     * )
     * EXPECTED: {} escape
     */
    @Test
    fun `local variables do not escape if not necessary`() {
        // given
        val xDef = variableDeclaration("x")
        val xUse = variableUse("x")
        val fDef = functionDefinition("f", listOf(), block(xDef, xUse))
        val ast = block(fDef)

        val types =
            TypeCheckingResult(
                mapOf(),
                mapOf(
                    xDef to BuiltinType.IntegerType,
                    fDef to FunctionType(listOf(), BuiltinType.IntegerType),
                ),
            )

        val resolvedVariables = mapOf(xUse to xDef)

        val xVar = Variable.PrimitiveVariable()

        val xAVar = AnalyzedVariable(xVar, fDef.value, VariableUseType.READ_WRITE)

        val fA = AnalyzedFunction(fDef.value, null, setOf(xAVar), listOf(), mutableSetOf(), 0, emptySet())

        val functionAnalysis = mapOf(fDef.value to fA)

        val variablesMap =
            VariablesMap(
                mapOf(),
                mapOf(xDef to xVar),
            )

        // when
        val result =
            escapeAnalysis(
                ast,
                resolvedVariables,
                functionAnalysis,
                variablesMap,
                types,
                emptyMap(),
            )

        // then
        assertThat(result.escapedVariables).isEmpty()
    }

    /*
     * let y = 10;
     * let f = [] -> Unit => (
     *   let x = 15;
     *   y = x;
     * )
     * EXPECTED: {} escape
     */
    @Test
    fun `variables do not escape through assignment to non-functional type`() {
        // given
        val xDef = variableDeclaration("x")
        val xUse = variableUse("x")
        val yDef = variableDeclaration("y")
        val yUse = variableUse("y")
        val fDef = functionDefinition("f", listOf(), block(xDef, yUse assign xUse))
        val ast = block(fDef)

        val types =
            TypeCheckingResult(
                mapOf(
                    yUse to BuiltinType.IntegerType,
                    xUse to BuiltinType.IntegerType,
                ),
                mapOf(
                    yDef to BuiltinType.IntegerType,
                    xDef to BuiltinType.IntegerType,
                    fDef to FunctionType(listOf(BuiltinType.IntegerType), BuiltinType.IntegerType),
                ),
            )

        val resolvedVariables = mapOf(xUse to xDef, yUse to yDef)

        val yVar = Variable.PrimitiveVariable()
        val xVar = Variable.PrimitiveVariable()

        val xAVar = AnalyzedVariable(xVar, fDef.value, VariableUseType.READ_WRITE)

        val fA = AnalyzedFunction(fDef.value, null, setOf(xAVar), listOf(), mutableSetOf(), 0, emptySet())

        val functionAnalysis = mapOf(fDef.value to fA)

        val variablesMap =
            VariablesMap(
                mapOf(),
                mapOf(xDef to xVar, yDef to yVar),
            )

        // when
        val result =
            escapeAnalysis(
                ast,
                resolvedVariables,
                functionAnalysis,
                variablesMap,
                types,
                emptyMap(),
            )

        // then
        assertThat(result.escapedVariables).isEmpty()
    }
}
