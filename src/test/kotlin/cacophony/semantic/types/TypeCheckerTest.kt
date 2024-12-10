package cacophony.semantic.types

import cacophony.*
import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.TypeCheckerDiagnostics
import cacophony.semantic.syntaxtree.*
import cacophony.utils.Location
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TypeCheckerTest {
    private fun assertTypeEquals(type1: TypeExpr?, type2: TypeExpr?) = assertEquals(type1.toString(), type2.toString())

    @Test
    fun `equality check`() {
        val type1 = BuiltinType.BooleanType
        val type2 = BuiltinType.BooleanType
        assertEquals(type1, type2)
    }

    @Test
    fun `non-equality check`() {
        val type1 = BuiltinType.BooleanType
        val type2 = BuiltinType.IntegerType
        assertNotEquals(type1, type2)
    }

    @Test
    fun `function type string representation`() {
        val functionType = FunctionType(listOf(BuiltinType.BooleanType, BuiltinType.IntegerType), BuiltinType.UnitType)
        assertEquals("[Bool, Int] -> Unit", functionType.toString())
        assertEquals("[Bool, Int] -> Unit", functionType.name)
    }

    @MockK
    lateinit var diagnostics: Diagnostics

    @BeforeEach
    fun setUpMocks() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { diagnostics.report(any(), any<Location>()) } just runs
    }

    private fun testUnit() = basicType("Unit")

    private fun testInt() = basicType("Int")

    private fun testBoolean() = basicType("Bool")

    private val intLiteral = lit(7)
    private val booleanLiteral = lit(true)

    @Test
    fun `ok - empty block`() {
        val ast = block()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - block with empty expression`() {
        val ast = block(empty())
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - variable definition with type`() {
        val varDef = typedVariableDeclaration("x", testInt(), intLiteral)
        val ast = block(varDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[varDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - variable declaration without type`() {
        val varDef = typedVariableDeclaration("x", null, intLiteral)
        val ast = block(varDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[varDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - simple variable use with type`() {
        val varDef = typedVariableDeclaration("x", testInt(), intLiteral)
        val varUse = variableUse("x")
        val ast = block(varDef, varUse)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.UnitType, result[varDef])
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - simple variable use without type`() {
        val varDef = typedVariableDeclaration("x", null, intLiteral)
        val varUse = variableUse("x")
        val ast = block(varDef, varUse)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.UnitType, result[varDef])
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - simple variable use with semicolon`() {
        val varDef = typedVariableDeclaration("x", testInt(), intLiteral)
        val varUse = variableUse("x")
        val ast = block(varDef, varUse, empty())
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.UnitType, result[varDef])
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - empty function declaration without type - () to Unit`() {
        val funDef = typedFunctionDefinition("f", null, emptyList(), testUnit(), empty())
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - empty function declaration with type - () to Unit`() {
        val funDef =
            typedFunctionDefinition(
                "f",
                functionalType(emptyList(), testUnit()),
                emptyList(),
                testUnit(),
                empty(),
            )
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - empty function declaration without type - (Int) to Unit`() {
        val funDef =
            typedFunctionDefinition(
                "f",
                null,
                listOf(typedArg("x", testInt())),
                testUnit(),
                empty(),
            )
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - empty function declaration with type - (Int) to Unit`() {
        val funDef =
            typedFunctionDefinition(
                "f",
                functionalType(listOf(testInt()), testUnit()),
                listOf(typedArg("x", testInt())),
                testUnit(),
                empty(),
            )
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - nonempty function declaration - () to Int`() {
        val funDef = typedFunctionDefinition("f", null, emptyList(), testInt(), intLiteral)
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - nonempty function declaration - (Int) to Int`() {
        val funArg = typedArg("x", testInt())
        val funDef = typedFunctionDefinition("f", null, listOf(funArg), testInt(), intLiteral)
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.IntegerType, result[funArg])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - nonempty function declaration - (Int) to Int with VariableUse`() {
        val funArg = typedArg("x", testInt())
        val varUse = variableUse("x")
        val funDef = typedFunctionDefinition("f", null, listOf(funArg), testInt(), varUse)
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to funArg))
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.IntegerType, result[funArg])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - function declaration - () to Int with outer scope `() {
        val varDec = typedVariableDeclaration("x", null, intLiteral)
        val varUse = variableUse("x")
        val funDef = typedFunctionDefinition("f", null, emptyList(), testInt(), varUse)
        val ast = block(varDec, funDef)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDec))
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[varDec])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - function declaration - (Int, Boolean) to Int no type`() {
        val arg1 = typedArg("x", testInt())
        val arg2 = typedArg("y", testBoolean())
        val funDef = typedFunctionDefinition("f", null, listOf(arg1, arg2), testInt(), intLiteral)
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.IntegerType, result[arg1])
        assertTypeEquals(BuiltinType.BooleanType, result[arg2])
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - function declaration - (Int, Bool) to Int with type`() {
        val arg1 = typedArg("x", testInt())
        val arg2 = typedArg("y", testBoolean())
        val funDef =
            typedFunctionDefinition(
                "f",
                functionalType(listOf(testInt(), testBoolean()), testInt()),
                listOf(arg1, arg2),
                testInt(),
                intLiteral,
            )
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.IntegerType, result[arg1])
        assertTypeEquals(BuiltinType.BooleanType, result[arg2])
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - function declaration - () to Unit with type`() {
        val funDef =
            typedFunctionDefinition(
                "f",
                functionalType(emptyList(), basicType("Unit")),
                emptyList(),
                testUnit(),
                empty(),
            )
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - function call () to Int`() {
        val funDef = typedFunctionDefinition("f", null, emptyList(), testInt(), intLiteral)
        val varUse = variableUse("f")
        val funCall = call(varUse)
        val ast = block(funDef, funCall)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to funDef))
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(FunctionType(emptyList(), BuiltinType.IntegerType), result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[funCall])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - function call (Unit) to Int`() {
        val funArg = typedArg("x", testUnit())
        val funDef = typedFunctionDefinition("f", null, listOf(funArg), testInt(), intLiteral)
        val varUse = variableUse("f")
        val funCall = call(varUse, empty())
        val ast = block(funDef, funCall)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to funDef))
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(FunctionType(listOf(BuiltinType.UnitType), BuiltinType.IntegerType), result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[funCall])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - function call (Int) to Int with VariableUse`() {
        val varDef = typedVariableDeclaration("a", null, intLiteral)
        val varUse = variableUse("a")
        val funArg = typedArg("x", testInt())
        val funDef = typedFunctionDefinition("f", null, listOf(funArg), testInt(), intLiteral)
        val funUse = variableUse("f")
        val funCall = call(funUse, varUse)
        val ast = block(varDef, funDef, funCall)
        val result = checkTypes(ast, diagnostics, mapOf(funUse to funDef, varUse to varDef))
        assertTypeEquals(FunctionType(listOf(BuiltinType.IntegerType), BuiltinType.IntegerType), result[funUse])
        assertTypeEquals(BuiltinType.IntegerType, result[funCall])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - foreign function declaration - (Int) to Int`() {
        val funDec = foreignFunctionDeclaration("f", listOf(testInt()), testInt())
        val ast = block(funDec)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDec])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - foreign function call () to Int`() {
        val funDec = foreignFunctionDeclaration("f", emptyList(), testInt())
        val varUse = variableUse("f")
        val funCall = call(varUse)
        val ast = block(funDec, funCall)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to funDec))
        assertTypeEquals(BuiltinType.UnitType, result[funDec])
        assertTypeEquals(FunctionType(emptyList(), BuiltinType.IntegerType), result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[funCall])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - int literal`() {
        val literal = lit(1)
        val ast = block(literal)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.IntegerType, result[literal])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - boolean literal`() {
        val literal = lit(true)
        val ast = block(literal)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.BooleanType, result[literal])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - nested block simple`() {
        val inside = empty()
        val block1 = block(inside)
        val block2 = block()
        val ast = block(block1, block2)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[inside])
        assertTypeEquals(BuiltinType.UnitType, result[block1])
        assertTypeEquals(BuiltinType.UnitType, result[block2])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - if with else, both Unit`() {
        val em1 = empty()
        val em2 = empty()
        val statement = ifThenElse(booleanLiteral, em1, em2)
        val ast = block(statement)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.BooleanType, result[booleanLiteral])
        assertTypeEquals(BuiltinType.UnitType, result[em1])
        assertTypeEquals(BuiltinType.UnitType, result[em2])
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - if with else, both Int literal`() {
        val branch1 = lit(1)
        val branch2 = lit(2)
        val statement = ifThenElse(booleanLiteral, branch1, branch2)
        val ast = block(statement)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.BooleanType, result[booleanLiteral])
        assertTypeEquals(BuiltinType.IntegerType, result[branch1])
        assertTypeEquals(BuiltinType.IntegerType, result[branch2])
        assertTypeEquals(BuiltinType.IntegerType, result[statement])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - if with else, both Int - Literal and VariableUse`() {
        val branch1 = lit(1)
        val varDef = typedVariableDeclaration("x", null, intLiteral)
        val branch2 = variableUse("x")
        val statement = ifThenElse(booleanLiteral, branch1, branch2)
        val ast = block(varDef, statement)
        val result = checkTypes(ast, diagnostics, mapOf(branch2 to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[booleanLiteral])
        assertTypeEquals(BuiltinType.IntegerType, result[branch1])
        assertTypeEquals(BuiltinType.IntegerType, result[branch2])
        assertTypeEquals(BuiltinType.IntegerType, result[statement])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - if without else, true branch Unit`() {
        val branch = empty()
        val statement = ifThenElse(booleanLiteral, branch, empty())
        val ast = block(statement)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.BooleanType, result[booleanLiteral])
        assertTypeEquals(BuiltinType.UnitType, result[branch])
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - if without else, true branch Unit, VariableUse at test`() {
        val varDef = typedVariableDeclaration("flag", null, lit(true))
        val varUse = variableUse("flag")
        val branch = empty()
        val statement = ifThenElse(varUse, branch, empty())
        val ast = block(varDef, statement)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[varUse])
        assertTypeEquals(BuiltinType.UnitType, result[branch])
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - if without else, true branch Int then Empty, VariableUse at test and branch`() {
        val flagDef = typedVariableDeclaration("flag", null, booleanLiteral)
        val flagUse = variableUse("flag")
        val branchDef = typedVariableDeclaration("x", null, intLiteral)
        val branchUse = variableUse("x")
        val statement = ifThenElse(flagUse, block(branchUse, empty()), empty())
        val ast = block(flagDef, branchDef, statement)
        val result = checkTypes(ast, diagnostics, mapOf(flagUse to flagDef, branchUse to branchDef))
        assertTypeEquals(BuiltinType.BooleanType, result[flagUse])
        assertTypeEquals(BuiltinType.IntegerType, result[branchUse])
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - empty while, literal test`() {
        val statement = whileLoop(booleanLiteral, empty())
        val ast = block(statement)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - empty while, variable test`() {
        val varDef = typedVariableDeclaration("x", null, booleanLiteral)
        val varUse = variableUse("x")
        val statement = whileLoop(varUse, empty())
        val ast = block(varDef, statement)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - non-empty while, variable test`() {
        val varDef = typedVariableDeclaration("x", null, booleanLiteral)
        val varUse1 = variableUse("x")
        val varUse2 = variableUse("x")
        val body = block(varUse2)
        val statement = whileLoop(varUse1, body)
        val ast = block(varDef, statement)
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - return with Unit`() {
        val body = returnStatement(empty())
        val funDef = typedFunctionDefinition("f", null, emptyList(), testUnit(), body)
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - Void propagates`() {
        val body = block(returnStatement(empty()), empty())
        val funDef = typedFunctionDefinition("f", null, emptyList(), testUnit(), body)
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - return with Int literal`() {
        val body = returnStatement(intLiteral)
        val funDef = typedFunctionDefinition("f", null, emptyList(), testInt(), body)
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - return with argument`() {
        val argDef = typedArg("x", testInt())
        val argUse = variableUse("x")
        val body = returnStatement(argUse)
        val funDef = typedFunctionDefinition("f", null, listOf(argDef), testInt(), body)
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, mapOf(argUse to argDef))
        assertTypeEquals(BuiltinType.IntegerType, result[argUse])
        assertTypeEquals(TypeExpr.VoidType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - return inside if branch`() {
        val body =
            ifThenElse(
                booleanLiteral,
                returnStatement(lit(2)),
                lit(3),
            )
        val funDef = typedFunctionDefinition("f", null, emptyList(), testInt(), body)
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - return inside both branches`() {
        val body =
            ifThenElse(
                booleanLiteral,
                returnStatement(lit(2)),
                returnStatement(lit(3)),
            )
        val funDef = typedFunctionDefinition("f", null, emptyList(), testInt(), body)
        val ast = block(funDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - return in nested function`() {
        val innerFunDef =
            typedFunctionDefinition(
                "f",
                null,
                emptyList(),
                testInt(),
                returnStatement(intLiteral),
            )
        val outerFunDef =
            typedFunctionDefinition(
                "g",
                null,
                emptyList(),
                testBoolean(),
                block(innerFunDef, booleanLiteral),
            )
        val ast = block(outerFunDef)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[innerFunDef])
        assertTypeEquals(BuiltinType.UnitType, result[outerFunDef])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - break is Void`() {
        val body1 = block(breakStatement())
        val while1 = whileLoop(lit(false), body1)
        val body2 = block(while1, breakStatement())
        val while2 = whileLoop(lit(false), body2)
        val ast = block(while2)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[body1])
        assertTypeEquals(BuiltinType.UnitType, result[while1])
        assertTypeEquals(TypeExpr.VoidType, result[body2])
        assertTypeEquals(BuiltinType.UnitType, result[while2])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - break in else branch`() {
        val breakStatement = breakStatement()
        val ifElseStatement = ifThenElse(booleanLiteral, intLiteral, breakStatement)
        val testLiteral = lit(true)
        val whileStatement = whileLoop(testLiteral, ifElseStatement)
        val ast = block(whileStatement)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[breakStatement])
        assertTypeEquals(BuiltinType.IntegerType, result[ifElseStatement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `error - bare break statement`() {
        val statement = breakStatement()
        val ast = block(statement)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(TypeCheckerDiagnostics.BreakOutsideWhile, any<Pair<Location, Location>>())
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - break outside loop`() {
        val while1 = whileLoop(lit(false), empty())
        val while2 = whileLoop(lit(false), while1)
        val ast = block(while2, breakStatement())
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(TypeCheckerDiagnostics.BreakOutsideWhile, any<Pair<Location, Location>>())
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - break inside loop test condition`() {
        val testBlock = block(breakStatement(), lit(false))
        val whileStatement = whileLoop(testBlock, empty())
        val ast = block(whileStatement)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(TypeCheckerDiagnostics.BreakOutsideWhile, any<Pair<Location, Location>>())
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `ok - negation of literal`() {
        val body = lnot(booleanLiteral)
        val ast = block(body)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - negation of variable`() {
        val varDef = typedVariableDeclaration("x", null, booleanLiteral)
        val varUse = variableUse("x")
        val body = lnot(varUse)
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - unary minus of literal`() {
        val body = minus(intLiteral)
        val ast = block(body)
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - unary minus of variable`() {
        val varDef = typedVariableDeclaration("x", null, intLiteral)
        val varUse = variableUse("x")
        val body = minus(varUse)
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - add`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse add intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - sub`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse sub intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - mul`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse mul intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - div`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse div intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - mod`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse mod intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - add=`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse1 = variableUse("x")
        val varUse2 = variableUse("x")
        val body = varUse1 addeq varUse2
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - sub=`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse1 = variableUse("x")
        val varUse2 = variableUse("x")
        val body = varUse1 subeq varUse2
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - mul=`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse1 = variableUse("x")
        val varUse2 = variableUse("x")
        val body = varUse1 muleq varUse2
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - div=`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse1 = variableUse("x")
        val varUse2 = variableUse("x")
        val body = varUse1 diveq varUse2
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - mod=`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse1 = variableUse("x")
        val varUse2 = variableUse("x")
        val body = varUse1 modeq varUse2
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - or`() {
        val varDef = typedVariableDeclaration("x", null, lit(true))
        val varUse = variableUse("x")
        val body = varUse lor booleanLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - and`() {
        val varDef = typedVariableDeclaration("x", null, lit(true))
        val varUse = variableUse("x")
        val body = varUse land booleanLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - less`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse lt intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - less=`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse leq intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - greater`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse gt intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - greater=`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse geq intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - eq Int`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse eq intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - eq Bool`() {
        val varDef = typedVariableDeclaration("x", null, lit(true))
        val varUse = variableUse("x")
        val body = varUse eq booleanLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - neq Int`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = varUse neq intLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - neq Bool`() {
        val varDef = typedVariableDeclaration("x", null, lit(true))
        val varUse = variableUse("x")
        val body = varUse neq booleanLiteral
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - assignment from variable`() {
        val var1Def = typedVariableDeclaration("x", null, lit(3))
        val var1Use = variableUse("x")
        val var2Def = typedVariableDeclaration("y", null, lit(2))
        val var2Use = variableUse("y")
        val body = variableWrite(var1Use, var2Use)
        val ast = block(var1Def, var2Def, body)
        val result = checkTypes(ast, diagnostics, mapOf(var1Use to var1Def, var2Use to var2Def))
        assertTypeEquals(BuiltinType.IntegerType, result[var1Use])
        assertTypeEquals(BuiltinType.IntegerType, result[var2Use])
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - assignment from literal`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val body = variableWrite(varUse, intLiteral)
        val ast = block(varDef, body)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `ok - assignment from Void`() {
        val varDef = typedVariableDeclaration("x", null, lit(3))
        val varUse = variableUse("x")
        val res = returnStatement(empty())
        val body = variableWrite(varUse, res)
        val funDef =
            typedFunctionDefinition("f", null, emptyList(), testUnit(), block(body, empty()))
        val ast = block(varDef, funDef)
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(TypeExpr.VoidType, result[res])
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        verify { diagnostics wasNot called }
    }

    @Test
    fun `error - unknown type at variable declaration`() {
        val varDec = typedVariableDeclaration("x", basicType("Type"), empty())
        val ast = block(varDec)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) { diagnostics.report(TypeCheckerDiagnostics.UnknownType, any<Pair<Location, Location>>()) }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - unknown type at argument declaration`() {
        val funDec =
            typedFunctionDefinition(
                "f",
                null,
                listOf(typedArg("a", basicType("Type"))),
                testUnit(),
                empty(),
            )
        val ast = block(funDec)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) { diagnostics.report(TypeCheckerDiagnostics.UnknownType, any<Pair<Location, Location>>()) }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - function declaration type mismatch - types`() {
        val arg1 = typedArg("x", testInt())
        val arg2 = typedArg("y", testBoolean())
        val funDef =
            typedFunctionDefinition(
                "f",
                functionalType(listOf(testBoolean(), testInt()), testInt()),
                listOf(arg1, arg2),
                testInt(),
                intLiteral,
            )
        val ast = block(funDef)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch(
                    "[Bool, Int] -> Int",
                    "[Int, Bool] -> Int",
                ),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `ok - function declaration type mismatch - number of args`() {
        val funDef =
            typedFunctionDefinition(
                "f",
                functionalType(listOf(testInt()), basicType("Unit")),
                emptyList(),
                testUnit(),
                empty(),
            )
        val ast = block(funDef)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch(
                    "[Int] -> Unit",
                    "[] -> Unit",
                ),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - mismatch init vs declared`() {
        val varDec = typedVariableDeclaration("x", testBoolean(), intLiteral)
        val ast = block(varDec)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Bool", "Int"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - mismatch body vs return type`() {
        val funDec = typedFunctionDefinition("f", null, emptyList(), testInt(), booleanLiteral)
        val ast = block(funDec)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Int", "Bool"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - calling non function`() {
        val body = FunctionCall(mockRange(), intLiteral, emptyList())
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.ExpectedFunction,
                any<Pair<Location, Location>>(),
            )
        }
    }

    @Test
    fun `error - wrong argument type`() {
        val funDec =
            typedFunctionDefinition(
                "f",
                null,
                listOf(typedArg("a", testInt())),
                testUnit(),
                empty(),
            )
        val funUse = variableUse("f")
        val body = call(funUse, booleanLiteral)
        val ast = block(funDec, body)
        checkTypes(ast, diagnostics, mapOf(funUse to funDec))
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Int", "Bool"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - wrong argument type in foreign function call`() {
        val funDec = foreignFunctionDeclaration("f", listOf(testInt()), testInt())
        val funUse = variableUse("f")
        val body = call(funUse, booleanLiteral)
        val ast = block(funDec, body)
        checkTypes(ast, diagnostics, mapOf(funUse to funDec))
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Int", "Bool"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - assignment to non lvalue reference`() {
        val body = OperatorBinary.Assignment(mockRange(), empty(), booleanLiteral)
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.ExpectedLValueReference,
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - mismatch assignment`() {
        val varDec = typedVariableDeclaration("x", testBoolean(), booleanLiteral)
        val varUse = variableUse("x")
        val body = variableWrite(varUse, intLiteral)
        val ast = block(varDec, body)
        checkTypes(ast, diagnostics, mapOf(varUse to varDec))
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Bool", "Int"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - mismatch equals`() {
        val body = booleanLiteral eq intLiteral
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Bool", "Int"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - mismatch not equals`() {
        val body = booleanLiteral neq intLiteral
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Bool", "Int"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - equals on Unit`() {
        val body = empty() eq empty()
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.UnsupportedOperation("Unit", "== operator"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - not equals on Unit`() {
        val body = empty() neq empty()
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.UnsupportedOperation("Unit", "!= operator"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - unary minus on wrong type`() {
        val body = minus(booleanLiteral)
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.UnsupportedOperation("Bool", "unary - operator"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - unary negation on wrong type`() {
        val body = OperatorUnary.Negation(mockRange(), intLiteral)
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.UnsupportedOperation("Int", "unary ! operator"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - test in if statement`() {
        val body = ifThenElse(intLiteral, empty(), empty())
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Bool", "Int"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - mismatch in non empty branches`() {
        val body = ifThenElse(booleanLiteral, intLiteral, booleanLiteral)
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.NoCommonType("Int", "Bool"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - mismatch in empty branches`() {
        val body = ifThenElse(booleanLiteral, intLiteral, empty())
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.NoCommonType("Int", "Unit"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - return outside function body`() {
        val body = returnStatement(empty())
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.MisplacedReturn,
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - return with wrong type`() {
        val funDec =
            typedFunctionDefinition(
                "f",
                null,
                emptyList(),
                testInt(),
                returnStatement(booleanLiteral),
            )
        val ast = block(funDec)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Int", "Bool"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - non Bool in while test`() {
        val body = whileLoop(empty(), empty())
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Bool", "Unit"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - operator assignment on non lvalue`() {
        val body = intLiteral addeq lit(4)
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.ExpectedLValueReference,
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - operator assignment on wrong type rhs`() {
        val varDec = typedVariableDeclaration("x", null, intLiteral)
        val varUse = variableUse("x")
        val body = varUse addeq booleanLiteral
        val ast = block(varDec, body)
        checkTypes(ast, diagnostics, mapOf(varUse to varDec))
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Int", "Bool"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - operator assignment on wrong type lhs`() {
        val varDec = typedVariableDeclaration("x", null, booleanLiteral)
        val varUse = variableUse("x")
        val body = varUse add intLiteral
        val ast = block(varDec, body)
        checkTypes(ast, diagnostics, mapOf(varUse to varDec))
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Int", "Bool"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - add on wrong value rhs`() {
        val body = intLiteral add booleanLiteral
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Int", "Bool"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - add on wrong value lhs`() {
        val body = booleanLiteral add intLiteral
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Int", "Bool"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - or on wrong value lhs`() {
        val body = booleanLiteral lor intLiteral
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Bool", "Int"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }

    @Test
    fun `error - or on wrong value rhs`() {
        val body = intLiteral lor booleanLiteral
        val ast = block(body)
        checkTypes(ast, diagnostics, emptyMap())
        verify(exactly = 1) {
            diagnostics.report(
                TypeCheckerDiagnostics.TypeMismatch("Bool", "Int"),
                any<Pair<Location, Location>>(),
            )
        }
        confirmVerified(diagnostics)
    }
}
