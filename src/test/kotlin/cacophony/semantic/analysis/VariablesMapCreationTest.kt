package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.types.BuiltinType
import cacophony.semantic.types.FunctionType
import cacophony.semantic.types.ReferentialType
import cacophony.semantic.types.TypeCheckingResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Map.entry

class VariablesMapCreationTest {
    @Test
    fun `primitive variable is created for let definitions`() {
        // given
        val xDef = variableDeclaration("x", lit(1))
        val xUse = variableUse("x")
        val ast = block(xDef, xUse)

        val resolvedVariables = mapOf(xUse to xDef)
        val types =
            TypeCheckingResult(
                mapOf(xDef to BuiltinType.UnitType, xUse to BuiltinType.IntegerType),
                mapOf(xDef to BuiltinType.IntegerType),
            )

        // when
        val variables = createVariablesMap(ast, resolvedVariables, types)

        // then
        assertThat(variables.definitions).containsKeys(xDef)
        assertThat(variables.lvalues).contains(entry(xUse, variables.definitions[xDef]))
    }

    @Test
    fun `primitive variable is created for primitive function argument`() {
        // given
        val xArg = intArg("x")
        val xUse = variableUse("x")
        val fDef = intFunctionDefinition("f", listOf(xArg), xUse)

        val resolvedVariables = mapOf(xUse to xArg)
        val types =
            TypeCheckingResult(
                mapOf(xArg to BuiltinType.IntegerType, xUse to BuiltinType.IntegerType),
                mapOf(xArg to BuiltinType.IntegerType),
            )

        // when
        val variables = createVariablesMap(fDef, resolvedVariables, types)

        // then
        assertThat(variables.definitions).containsKeys(xArg)
        assertThat(variables.lvalues).contains(entry(xUse, variables.definitions[xArg]))
    }

    @Test
    fun `function variable is created for functional function argument`() {
        // given
        val xArg = typedArg("x", functionalType(listOf(intType()), intType()))
        val xUse = variableUse("x")
        val xCall = call(xUse, lit(1))
        val fDef = intFunctionDefinition("f", listOf(xArg), xCall)

        val resolvedVariables = mapOf(xUse to xArg)

        val fType = functionTypeExpr(BuiltinType.IntegerType, result = BuiltinType.IntegerType)
        val types =
            TypeCheckingResult(
                mapOf(xArg to fType, xUse to fType, xCall to BuiltinType.IntegerType),
                mapOf(xArg to fType),
            )

        // when
        val variables = createVariablesMap(fDef, resolvedVariables, types)

        // then
        assertThat(variables.definitions).containsKeys(xArg)
        assertThat(variables.definitions[xArg]).isExactlyInstanceOf(Variable.FunctionVariable::class.java)
        assertThat(variables.lvalues).contains(entry(xUse, variables.definitions[xArg]))
    }

    @Test
    fun `function variable is created for lambda definition`() {
        // given
        val lam = lambda(emptyList(), lit(1))
        val xDef = variableDeclaration("x", lam)
        val xUse = variableUse("x")
        val ast = block(xDef, xUse)

        val resolvedVariables = mapOf(xUse to xDef)
        val types =
            TypeCheckingResult(
                mapOf(xDef to BuiltinType.UnitType, xUse to FunctionType(emptyList(), BuiltinType.IntegerType)),
                mapOf(xDef to FunctionType(emptyList(), BuiltinType.IntegerType)),
            )

        // when
        val variables = createVariablesMap(ast, resolvedVariables, types)

        // then
        assertThat(variables.definitions).containsKeys(xDef)
        assertThat(variables.definitions[xDef]).isExactlyInstanceOf(Variable.FunctionVariable::class.java)
        assertThat(variables.lvalues).contains(entry(xUse, variables.definitions[xDef]))
    }

    @Test
    fun `struct variable is created for struct function argument`() {
        // given
        val xArg = typedArg("x", structType("a" to intType()))
        val xUse = variableUse("x")
        val aUse = lvalueFieldRef(xUse, "a")
        val fDef = intFunctionDefinition("f", listOf(xArg), aUse)

        val resolvedVariables = mapOf(xUse to xArg)
        val types =
            TypeCheckingResult(
                mapOf(xArg to structTypeExpr("a" to BuiltinType.IntegerType), xUse to BuiltinType.IntegerType),
                mapOf(xArg to structTypeExpr("a" to BuiltinType.IntegerType)),
            )

        // when
        val variables = createVariablesMap(fDef, resolvedVariables, types)

        // then
        assertThat(variables.definitions).containsKeys(xArg)
        assertThat(variables.lvalues).contains(entry(xUse, variables.definitions[xArg]))
        assertThat(variables.lvalues).contains(entry(aUse, field(variables.definitions[xArg], "a")))
    }

    @Test
    fun `struct variable is created for simple struct`() {
        // given
        val xDef = variableDeclaration("x", struct("a" to lit(1)))
        val xUse1 = variableUse("x")
        val xUse2 = variableUse("x")
        val aUse = lvalueFieldRef(xUse2, "a")
        val ast = block(xDef, xUse1, aUse)

        val resolvedVariables = mapOf(xUse1 to xDef, xUse2 to xDef)
        val types =
            TypeCheckingResult(
                mapOf(
                    xDef to structTypeExpr("a" to BuiltinType.IntegerType),
                    xUse1 to BuiltinType.IntegerType,
                    xUse2 to BuiltinType.IntegerType,
                ),
                mapOf(xDef to structTypeExpr("a" to BuiltinType.IntegerType)),
            )

        // when
        val variables = createVariablesMap(ast, resolvedVariables, types)

        // then
        assertThat(variables.definitions).containsKeys(xDef)
        assertThat(variables.lvalues).contains(entry(xUse1, variables.definitions[xDef]))
        assertThat(variables.lvalues).contains(entry(xUse2, variables.definitions[xDef]))
        assertThat(variables.lvalues).contains(entry(aUse, field(variables.definitions[xDef], "a")))
    }

    @Test
    fun `struct variable is created for nested struct`() {
        // given
        val struct = struct("a" to struct("b" to lit(1)))
        val xDef = variableDeclaration("x", struct)
        val xUse1 = variableUse("x")
        val xUse2 = variableUse("x")
        val aUse = lvalueFieldRef(xUse2, "a")
        val bUse = lvalueFieldRef(aUse, "b")
        val ast = block(xDef, xUse1, bUse)

        val resolvedVariables = mapOf(xUse1 to xDef, xUse2 to xDef)
        val types =
            TypeCheckingResult(
                mapOf(
                    struct to structTypeExpr("a" to structTypeExpr("b" to BuiltinType.IntegerType)),
                    xDef to structTypeExpr("a" to structTypeExpr("b" to BuiltinType.IntegerType)),
                    xUse1 to BuiltinType.IntegerType,
                    xUse2 to BuiltinType.IntegerType,
                ),
                mapOf(xDef to structTypeExpr("a" to structTypeExpr("b" to BuiltinType.IntegerType))),
            )

        // when
        val variables = createVariablesMap(ast, resolvedVariables, types)

        // then
        assertThat(variables.definitions).containsKeys(xDef)
        assertThat(variables.lvalues).contains(entry(xUse1, variables.definitions[xDef]))
        assertThat(variables.lvalues).contains(entry(xUse2, variables.definitions[xDef]))
        assertThat(variables.lvalues).contains(entry(aUse, field(variables.definitions[xDef], "a")))
        assertThat(variables.lvalues).contains(entry(bUse, field(field(variables.definitions[xDef], "a"), "b")))
    }

    @Test
    fun `struct variable is created for variables nested in blocks`() {
        // given
        // (let x = {a = 1}; let y = {b = x.a}).b
        val aStruct = struct("a" to lit(1))
        val xDef = variableDeclaration("x", aStruct)
        val xUse = variableUse("x")
        val aUse = lvalueFieldRef(xUse, "a")

        val bStruct = struct("b" to aUse)
        val yDef = variableDeclaration("y", bStruct)
        val yUse = variableUse("y")
        val block = block(xDef, aUse, yDef, yUse)
        val bUse = rvalueFieldRef(block, "b")

        val resolvedVariables = mapOf(xUse to xDef, yUse to yDef)
        val types =
            TypeCheckingResult(
                mapOf(
                    aStruct to structTypeExpr("a" to BuiltinType.IntegerType),
                    bStruct to structTypeExpr("b" to BuiltinType.IntegerType),
                    xDef to structTypeExpr("a" to BuiltinType.IntegerType),
                    xUse to BuiltinType.IntegerType,
                    yDef to structTypeExpr("b" to BuiltinType.IntegerType),
                    yUse to BuiltinType.IntegerType,
                    bUse to BuiltinType.IntegerType,
                ),
                mapOf(xDef to structTypeExpr("a" to BuiltinType.IntegerType), yDef to structTypeExpr("b" to BuiltinType.IntegerType)),
            )

        // when
        val variables = createVariablesMap(bUse, resolvedVariables, types)

        // then
        assertThat(variables.definitions).containsKeys(xDef, yDef)
        assertThat(variables.lvalues).contains(entry(xUse, variables.definitions[xDef]))
        assertThat(variables.lvalues).contains(entry(aUse, field(variables.definitions[xDef], "a")))
        assertThat(variables.lvalues).contains(entry(yUse, variables.definitions[yDef]))
    }

    @Test
    fun `variable is not created for rvalue struct field access`() {
        // given
        val struct = struct("a" to lit(1))
        val access = rvalueFieldRef(struct, "a")

        val resolvedVariables: ResolvedVariables = emptyMap()
        val types =
            TypeCheckingResult(
                mapOf(
                    struct to structTypeExpr("a" to BuiltinType.IntegerType),
                    access to BuiltinType.IntegerType,
                ),
                emptyMap(),
            )

        // when
        val variables = createVariablesMap(access, resolvedVariables, types)

        // then
        assertThat(variables.definitions).isEmpty()
        assertThat(variables.lvalues).isEmpty()
    }

    @Test
    fun `all dereferences are mapped to heap`() {
        // given
        val pDef = variableDeclaration("p")
        val pUseStandalone = variableUse("p")
        val standaloneDeref = deref(pUseStandalone)
        val pUseInBlock = variableUse("p")
        val blockDeref = deref(pUseInBlock)
        // let p; @p; @(1; p)
        val ast = block(pDef, standaloneDeref, blockDeref)

        val resolvedVariables = mapOf(pUseStandalone to pDef, pUseInBlock to pDef)
        val pType = ReferentialType(BuiltinType.IntegerType)
        val types =
            TypeCheckingResult(
                mapOf(
                    pDef to pType,
                    pUseStandalone to pType,
                    standaloneDeref to BuiltinType.IntegerType,
                    pUseInBlock to pType,
                    blockDeref to BuiltinType.IntegerType,
                ),
                mapOf(pDef to ReferentialType(BuiltinType.IntegerType)),
            )

        // when
        val variables = createVariablesMap(ast, resolvedVariables, types)

        // then
        assertThat(variables.definitions).containsKey(pDef)
        assertThat(variables.lvalues).containsAllEntriesOf(mapOf(standaloneDeref to Variable.Heap, blockDeref to Variable.Heap))
    }

    private fun field(variable: Variable?, vararg names: String): Variable {
        var current = variable!!
        for (name in names) {
            current = (current as Variable.StructVariable).fields[name]!!
        }
        return current
    }
}
