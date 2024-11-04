@file:Suppress("ktlint")

package cacophony.semantic

import cacophony.semantic.syntaxtree.*
import cacophony.utils.Diagnostics
import cacophony.utils.Location
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TypeCheckerTest {
    private fun assertTypeEquals(
        type1: TypeExpr?,
        type2: TypeExpr?,
    ) = assertEquals(type1.toString(), type2.toString())

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
        assertEquals("[Boolean, Int] -> Unit", functionType.toString())
        assertEquals("[Boolean, Int] -> Unit", functionType.name)
    }

    private val lc = Pair(Location(0), Location(0))

    class DiagnosticsMock : Diagnostics {
        var msg: String? = null

        override fun report(
            message: String,
            location: Location,
        ) {}

        override fun report(
            message: String,
            range: Pair<Location, Location>,
        ) {
            msg = message
        }
    }

    private fun getDiagnostic() = object : Diagnostics {
        var msg: String? = null

        override fun report(message: String, location: Location) {}

        override fun report(message: String, range: Pair<Location, Location>) {
            msg = message
        }
    }

    private val testUnit = Type.Basic(lc, "Unit")
    private val testInt = Type.Basic(lc, "Int")
    private val testBoolean = Type.Basic(lc, "Boolean")
    private val intLiteral = Literal.IntLiteral(lc, 7)
    private val booleanLiteral = Literal.BoolLiteral(lc, true)

    @Test
    fun `ok - empty block`() {
        val ast = Block(lc, emptyList())
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - block with empty expression`() {
        val ast = Block(lc, listOf(Empty(lc)))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - variable definition with type`() {
        val varDef = Definition.VariableDeclaration(lc, "x", testInt, intLiteral)
        val ast = Block(lc, listOf(varDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[varDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - variable declaration without type`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, intLiteral)
        val ast = Block(lc, listOf(varDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[varDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - simple variable use with type`() {
        val varDef = Definition.VariableDeclaration(lc, "x", testInt, intLiteral)
        val varUse = VariableUse(lc, "x")
        val ast = Block(lc, listOf(varDef, varUse))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.UnitType, result[varDef])
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - simple variable use without type`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, intLiteral)
        val varUse = VariableUse(lc, "x")
        val ast = Block(lc, listOf(varDef, varUse))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.UnitType, result[varDef])
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - simple variable use with semicolon`() {
        val varDef = Definition.VariableDeclaration(lc, "x", testInt, intLiteral)
        val varUse = VariableUse(lc, "x")
        val ast = Block(lc, listOf(varDef, varUse, Empty(lc)))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.UnitType, result[varDef])
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - empty function declaration without type - () to Unit`() {
        val funDef = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testUnit, Empty(lc))
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - empty function declaration with type - () to Unit`() {
        val funDef = Definition.FunctionDeclaration(lc, "f", Type.Functional(lc, emptyList(), testUnit), emptyList(), testUnit, Empty(lc))
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - empty function declaration without type - (Int) to Unit`() {
        val funDef =
            Definition.FunctionDeclaration(
                lc,
                "f",
                null,
                listOf(Definition.FunctionArgument(lc, "x", testInt)),
                testUnit,
                Empty(lc),
            )
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - empty function declaration with type - (Int) to Unit`() {
        val funDef =
            Definition.FunctionDeclaration(
                lc,
                "f",
                Type.Functional(lc, listOf(testInt), testUnit),
                listOf(Definition.FunctionArgument(lc, "x", testInt)),
                testUnit,
                Empty(lc),
            )
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - nonempty function declaration - () to Int`() {
        val funDef = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testInt, intLiteral)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - nonempty function declaration - (Int) to Int`() {
        val funArg = Definition.FunctionArgument(lc, "x", testInt)
        val funDef = Definition.FunctionDeclaration(lc, "f", null, listOf(funArg), testInt, intLiteral)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.IntegerType, result[funArg])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - nonempty function declaration - (Int) to Int with VariableUse`() {
        val funArg = Definition.FunctionArgument(lc, "x", testInt)
        val varUse = VariableUse(lc, "x")
        val funDef = Definition.FunctionDeclaration(lc, "f", null, listOf(funArg), testInt, varUse)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to funArg))
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.IntegerType, result[funArg])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - function declaration - () to Int with outer scope `() {
        val varDec = Definition.VariableDeclaration(lc, "x", null, intLiteral)
        val varUse = VariableUse(lc, "x")
        val funDef = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testInt, varUse)
        val ast = Block(lc, listOf(varDec, funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDec))
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[varDec])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - function declaration - (Int, Boolean) to Int no type`() {
        val arg1 = Definition.FunctionArgument(lc, "x", testInt)
        val arg2 = Definition.FunctionArgument(lc, "y", testBoolean)
        val funDef = Definition.FunctionDeclaration(lc, "f", null, listOf(arg1, arg2), testInt, intLiteral)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.IntegerType, result[arg1])
        assertTypeEquals(BuiltinType.BooleanType, result[arg2])
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - function declaration - (Int, Boolean) to Int with type`() {
        val arg1 = Definition.FunctionArgument(lc, "x", testInt)
        val arg2 = Definition.FunctionArgument(lc, "y", testBoolean)
        val funDef = Definition.FunctionDeclaration(lc, "f", Type.Functional(lc, listOf(Type.Basic(lc, "Int"), Type.Basic(lc, "Boolean")), Type.Basic(lc, "Int")), listOf(arg1, arg2), testInt, intLiteral)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.IntegerType, result[arg1])
        assertTypeEquals(BuiltinType.BooleanType, result[arg2])
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - function declaration - () to Unit with type`() {
        val funDef = Definition.FunctionDeclaration(lc, "f", Type.Functional(lc, emptyList(), Type.Basic(lc, "Unit")), emptyList(), testUnit, Empty(lc))
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - function call () to Int`() {
        val funDef = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testInt, intLiteral)
        val varUse = VariableUse(lc, "f")
        val funCall = FunctionCall(lc, varUse, emptyList())
        val ast = Block(lc, listOf(funDef, funCall))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to funDef))
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(FunctionType(emptyList(), BuiltinType.IntegerType), result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[funCall])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - function call (Unit) to Int`() {
        val funArg = Definition.FunctionArgument(lc, "x", testUnit)
        val funDef = Definition.FunctionDeclaration(lc, "f", null, listOf(funArg), testInt, intLiteral)
        val varUse = VariableUse(lc, "f")
        val funCall = FunctionCall(lc, varUse, listOf(Empty(lc)))
        val ast = Block(lc, listOf(funDef, funCall))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to funDef))
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(FunctionType(listOf(BuiltinType.UnitType), BuiltinType.IntegerType), result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[funCall])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - function call (Int) to Int with VariableUse`() {
        val varDef = Definition.VariableDeclaration(lc, "a", null, intLiteral)
        val varUse = VariableUse(lc, "a")
        val funArg = Definition.FunctionArgument(lc, "x", testInt)
        val funDef = Definition.FunctionDeclaration(lc, "f", null, listOf(funArg), testInt, intLiteral)
        val funUse = VariableUse(lc, "f")
        val funCall = FunctionCall(lc, funUse, listOf(varUse))
        val ast = Block(lc, listOf(varDef, funDef, funCall))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(funUse to funDef, varUse to varDef))
        assertTypeEquals(FunctionType(listOf(BuiltinType.IntegerType), BuiltinType.IntegerType), result[funUse])
        assertTypeEquals(BuiltinType.IntegerType, result[funCall])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - int literal`() {
        val literal = Literal.IntLiteral(lc, 1)
        val ast = Block(lc, listOf(literal))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.IntegerType, result[literal])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - boolean literal`() {
        val literal = Literal.BoolLiteral(lc, true)
        val ast = Block(lc, listOf(literal))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.BooleanType, result[literal])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - nested block simple`() {
        val inside = Empty(lc)
        val block1 = Block(lc, listOf(inside))
        val block2 = Block(lc, listOf())
        val ast = Block(lc, listOf(block1, block2))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[inside])
        assertTypeEquals(BuiltinType.UnitType, result[block1])
        assertTypeEquals(BuiltinType.UnitType, result[block2])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - if with else, both Unit`() {
        val em1 = Empty(lc)
        val em2 = Empty(lc)
        val statement = Statement.IfElseStatement(lc, booleanLiteral, em1, em2)
        val ast = Block(lc, listOf(statement))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.BooleanType, result[booleanLiteral])
        assertTypeEquals(BuiltinType.UnitType, result[em1])
        assertTypeEquals(BuiltinType.UnitType, result[em2])
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - if with else, both Int literal`() {
        val branch1 = Literal.IntLiteral(lc, 1)
        val branch2 = Literal.IntLiteral(lc, 2)
        val statement = Statement.IfElseStatement(lc, booleanLiteral, branch1, branch2)
        val ast = Block(lc, listOf(statement))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.BooleanType, result[booleanLiteral])
        assertTypeEquals(BuiltinType.IntegerType, result[branch1])
        assertTypeEquals(BuiltinType.IntegerType, result[branch2])
        assertTypeEquals(BuiltinType.IntegerType, result[statement])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - if with else, both Int - Literal and VariableUse`() {
        val branch1 = Literal.IntLiteral(lc, 1)
        val varDef = Definition.VariableDeclaration(lc, "x", null, intLiteral)
        val branch2 = VariableUse(lc, "x")
        val statement = Statement.IfElseStatement(lc, booleanLiteral, branch1, branch2)
        val ast = Block(lc, listOf(varDef, statement))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(branch2 to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[booleanLiteral])
        assertTypeEquals(BuiltinType.IntegerType, result[branch1])
        assertTypeEquals(BuiltinType.IntegerType, result[branch2])
        assertTypeEquals(BuiltinType.IntegerType, result[statement])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - if without else, true branch Unit`() {
        val branch = Empty(lc)
        val statement = Statement.IfElseStatement(lc, booleanLiteral, branch, null)
        val ast = Block(lc, listOf(statement))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.BooleanType, result[booleanLiteral])
        assertTypeEquals(BuiltinType.UnitType, result[branch])
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }


    @Test
    fun `ok - if without else, true branch Unit, VariableUse at test`() {
        val varDef = Definition.VariableDeclaration(lc, "flag", null, Literal.BoolLiteral(lc, true))
        val varUse = VariableUse(lc, "flag")
        val branch = Empty(lc)
        val statement = Statement.IfElseStatement(lc, varUse, branch, null)
        val ast = Block(lc, listOf(varDef, statement))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[varUse])
        assertTypeEquals(BuiltinType.UnitType, result[branch])
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - if without else, true branch Int then Empty, VariableUse at test and branch`() {
        val flagDef = Definition.VariableDeclaration(lc, "flag", null, booleanLiteral)
        val flagUse = VariableUse(lc, "flag")
        val branchDef = Definition.VariableDeclaration(lc, "x", null, intLiteral)
        val branchUse = VariableUse(lc, "x")
        val statement = Statement.IfElseStatement(lc, flagUse, Block(lc, listOf(branchUse, Empty(lc))), null)
        val ast = Block(lc, listOf(flagDef, branchDef, statement))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(flagUse to flagDef, branchUse to branchDef))
        assertTypeEquals(BuiltinType.BooleanType, result[flagUse])
        assertTypeEquals(BuiltinType.IntegerType, result[branchUse])
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - empty while, literal test`() {
        val statement = Statement.WhileStatement(lc, booleanLiteral, Empty(lc))
        val ast = Block(lc, listOf(statement))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - empty while, variable test`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, booleanLiteral)
        val varUse = VariableUse(lc, "x")
        val statement = Statement.WhileStatement(lc, varUse, Empty(lc))
        val ast = Block(lc, listOf(varDef, statement))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - non-empty while, variable test`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, booleanLiteral)
        val varUse1 = VariableUse(lc, "x")
        val varUse2 = VariableUse(lc, "x")
        val body = Block(lc, listOf(varUse2))
        val statement = Statement.WhileStatement(lc, varUse1, body)
        val ast = Block(lc, listOf(varDef, statement))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[statement])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - return with Unit`() {
        val body = Statement.ReturnStatement(lc, Empty(lc))
        val funDef = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testUnit, body)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - return with Int literal`() {
        val body = Statement.ReturnStatement(lc, intLiteral)
        val funDef = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testInt, body)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - return with argument`() {
        val argDef = Definition.FunctionArgument(lc, "x", testInt)
        val argUse = VariableUse(lc, "x")
        val body = Statement.ReturnStatement(lc, argUse)
        val funDef = Definition.FunctionDeclaration(lc, "f", null, listOf(argDef), testInt, body)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(argUse to argDef))
        assertTypeEquals(BuiltinType.IntegerType, result[argUse])
        assertTypeEquals(TypeExpr.VoidType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - return inside if branch`() {
        val body = Statement.IfElseStatement(lc, booleanLiteral, Statement.ReturnStatement(lc, Literal.IntLiteral(lc, 2)), Literal.IntLiteral(lc,3))
        val funDef = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testInt, body)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - return inside both branches`() {
        val body = Statement.IfElseStatement(lc, booleanLiteral, Statement.ReturnStatement(lc, Literal.IntLiteral(lc, 2)), Statement.ReturnStatement(lc, Literal.IntLiteral(lc, 3)))
        val funDef = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testInt, body)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - return in nested function`() {
        val innerFunDef = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testInt, Statement.ReturnStatement(lc, intLiteral))
        val outerFunDef = Definition.FunctionDeclaration(lc, "g", null, emptyList(), testBoolean, Block(lc, listOf(innerFunDef, booleanLiteral)))
        val ast = Block(lc, listOf(outerFunDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.UnitType, result[innerFunDef])
        assertTypeEquals(BuiltinType.UnitType, result[outerFunDef])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - break is Void`() {
        val statement = Statement.BreakStatement(lc)
        val ast = Block(lc, listOf(statement))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[statement])
        assertTypeEquals(TypeExpr.VoidType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - break in else branch`() {
        val breakStatement = Statement.BreakStatement(lc)
        val ast = Block(lc, listOf(Statement.IfElseStatement(lc, booleanLiteral, intLiteral, breakStatement)))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(TypeExpr.VoidType, result[breakStatement])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - negation of literal`() {
        val body = OperatorUnary.Negation(lc, booleanLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - negation of variable`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, booleanLiteral)
        val varUse = VariableUse(lc, "x")
        val body = OperatorUnary.Negation(lc, varUse)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - unary minus of literal`() {
        val body = OperatorUnary.Minus(lc, intLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, emptyMap())
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - unary minus of variable`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, intLiteral)
        val varUse = VariableUse(lc, "x")
        val body = OperatorUnary.Minus(lc, varUse)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        println(result[varDef])
        println(result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - add`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.Addition(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - sub`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.Subtraction(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - mul`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.Multiplication(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - div`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.Division(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - mod`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.Modulo(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - add=`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse1 = VariableUse(lc, "x")
        val varUse2 = VariableUse(lc, "x")
        val body = OperatorBinary.AdditionAssignment(lc, varUse1, varUse2)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }


    @Test
    fun `ok - sub=`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse1 = VariableUse(lc, "x")
        val varUse2 = VariableUse(lc, "x")
        val body = OperatorBinary.SubtractionAssignment(lc, varUse1, varUse2)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - mul=`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse1 = VariableUse(lc, "x")
        val varUse2 = VariableUse(lc, "x")
        val body = OperatorBinary.MultiplicationAssignment(lc, varUse1, varUse2)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - div=`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse1 = VariableUse(lc, "x")
        val varUse2 = VariableUse(lc, "x")
        val body = OperatorBinary.DivisionAssignment(lc, varUse1, varUse2)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - mod=`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse1 = VariableUse(lc, "x")
        val varUse2 = VariableUse(lc, "x")
        val body = OperatorBinary.ModuloAssignment(lc, varUse1, varUse2)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse1 to varDef, varUse2 to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - or`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.BoolLiteral(lc, true))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.LogicalOr(lc, varUse, booleanLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - and`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.BoolLiteral(lc, true))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.LogicalAnd(lc, varUse, booleanLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - less`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.Less(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - less=`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.LessEqual(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - greater`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.Greater(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - greater=`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.GreaterEqual(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - eq Int`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.Equals(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - eq Bool`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.BoolLiteral(lc, true))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.Equals(lc, varUse, booleanLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - neq Int`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.NotEquals(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - neq Bool`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.BoolLiteral(lc, true))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.NotEquals(lc, varUse, booleanLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.BooleanType, result[varUse])
        assertTypeEquals(BuiltinType.BooleanType, result[body])
        assertTypeEquals(BuiltinType.BooleanType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - assignment from variable`() {
        val var1Def = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val var1Use = VariableUse(lc, "x")
        val var2Def = Definition.VariableDeclaration(lc, "y", null, Literal.IntLiteral(lc, 2))
        val var2Use = VariableUse(lc, "y")
        val body = OperatorBinary.Assignment(lc, var1Use, var2Use)
        val ast = Block(lc, listOf(var1Def, var2Def, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(var1Use to var1Def, var2Use to var2Def))
        assertTypeEquals(BuiltinType.IntegerType, result[var1Use])
        assertTypeEquals(BuiltinType.IntegerType, result[var2Use])
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - assignment from literal`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.Assignment(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDef, body))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(BuiltinType.IntegerType, result[varUse])
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.IntegerType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `ok - assignment from Void`() {
        val varDef = Definition.VariableDeclaration(lc, "x", null, Literal.IntLiteral(lc, 3))
        val varUse = VariableUse(lc, "x")
        val res = Statement.ReturnStatement(lc, Empty(lc))
        val body = OperatorBinary.Assignment(lc, varUse, res)
        val funDef = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testUnit, Block(lc, listOf(body, Empty(lc))))
        val ast = Block(lc, listOf(varDef, funDef))
        val diagnostics = getDiagnostic()
        val result = checkTypes(ast, diagnostics, mapOf(varUse to varDef))
        assertTypeEquals(TypeExpr.VoidType, result[res])
        assertTypeEquals(BuiltinType.IntegerType, result[body])
        assertTypeEquals(BuiltinType.UnitType, result[funDef])
        assertTypeEquals(BuiltinType.UnitType, result[ast])
        assertNull(diagnostics.msg)
    }

    @Test
    fun `error - unknown type at variable declaration`() {
        val varDec = Definition.VariableDeclaration(lc, "x", Type.Basic(lc, "Type"), Empty(lc))
        val ast = Block(lc, listOf(varDec))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Unknown type", diagnostics.msg)
    }

    @Test
    fun `error - unknown type at argument declaration`() {
        val funDec = Definition.FunctionDeclaration(lc, "f", null, listOf(Definition.FunctionArgument(lc, "a", Type.Basic(lc, "Type"))), testUnit, Empty(lc))
        val ast = Block(lc, listOf(funDec))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Unknown type", diagnostics.msg)
    }

    @Test
    fun `error - function declaration type mismatch - types`() {
        val arg1 = Definition.FunctionArgument(lc, "x", testInt)
        val arg2 = Definition.FunctionArgument(lc, "y", testBoolean)
        val funDef = Definition.FunctionDeclaration(lc, "f", Type.Functional(lc, listOf(Type.Basic(lc, "Boolean"), Type.Basic(lc, "Int")), Type.Basic(lc, "Int")), listOf(arg1, arg2), testInt, intLiteral)
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected [Boolean, Int] -> Int, found [Int, Boolean] -> Int", diagnostics.msg)
    }

    @Test
    fun `ok - function declaration type mismatch - number of args`() {
        val funDef = Definition.FunctionDeclaration(lc, "f", Type.Functional(lc, listOf(testInt), Type.Basic(lc, "Unit")), emptyList(), testUnit, Empty(lc))
        val ast = Block(lc, listOf(funDef))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected [Int] -> Unit, found [] -> Unit", diagnostics.msg)
    }

    @Test
    fun `error - mismatch init vs declared`() {
        val varDec = Definition.VariableDeclaration(lc, "x", testBoolean, intLiteral)
        val ast = Block(lc, listOf(varDec))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Boolean, found Int", diagnostics.msg)
    }

    @Test
    fun `error - mismatch body vs return type`() {
        val funDec = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testInt, booleanLiteral)
        val ast = Block(lc, listOf(funDec))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Int, found Boolean", diagnostics.msg)
    }

    @Test
    fun `error - calling non function`() {
        val body = FunctionCall(lc, intLiteral, emptyList())
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Expected function", diagnostics.msg)
    }

    @Test
    fun `error - wrong argument type`() {
        val funDec = Definition.FunctionDeclaration(lc, "f", null, listOf(Definition.FunctionArgument(lc, "a", testInt)), testUnit, Empty(lc))
        val funUse = VariableUse(lc, "f")
        val body = FunctionCall(lc, funUse, listOf(booleanLiteral))
        val ast = Block(lc, listOf(funDec, body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, mapOf(funUse to funDec))
        assertEquals("Type mismatch: expected Int, found Boolean", diagnostics.msg)
    }

    @Test
    fun `error - assignment to non lvalue reference`() {
        val body = OperatorBinary.Assignment(lc, Empty(lc), booleanLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Expected lvalue reference", diagnostics.msg)
    }

    @Test
    fun `error - mismatch assignment`() {
        val varDec = Definition.VariableDeclaration(lc, "x", testBoolean, booleanLiteral)
        val varUse = VariableUse(lc ,"x")
        val body = OperatorBinary.Assignment(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDec, body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, mapOf(varUse to varDec))
        assertEquals("Type mismatch: expected Boolean, found Int", diagnostics.msg)
    }

    @Test
    fun `error - mismatch equals`() {
        val body = OperatorBinary.Equals(lc, booleanLiteral, intLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Boolean, found Int", diagnostics.msg)
    }

    @Test
    fun `error - mismatch not equals`() {
        val body = OperatorBinary.NotEquals(lc, booleanLiteral, intLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Boolean, found Int", diagnostics.msg)
    }


    @Test
    fun `error - equals on Unit`() {
        val body = OperatorBinary.Equals(lc, Empty(lc), Empty(lc))
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type Unit does not support == operator", diagnostics.msg)
    }

    @Test
    fun `error - not equals on Unit`() {
        val body = OperatorBinary.NotEquals(lc, Empty(lc), Empty(lc))
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type Unit does not support != operator", diagnostics.msg)
    }

    @Test
    fun `error - unary minus on wrong type`() {
        val body = OperatorUnary.Minus(lc, booleanLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type Boolean does not support unary - operator", diagnostics.msg)
    }

    @Test
    fun `error - unary negation on wrong type`() {
        val body = OperatorUnary.Negation(lc, intLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type Int does not support unary ! operator", diagnostics.msg)
    }

    @Test
    fun `error - test in if statement`() {
        val body = Statement.IfElseStatement(lc, intLiteral, Empty(lc), null)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Boolean, found Int", diagnostics.msg)
    }

    @Test
    fun `error - mismatch in non empty branches`() {
        val body = Statement.IfElseStatement(lc, booleanLiteral, intLiteral, booleanLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Could not find common type for Int and Boolean", diagnostics.msg)
    }

    @Test
    fun `error - mismatch in empty branches`() {
        val body = Statement.IfElseStatement(lc, booleanLiteral, intLiteral, null)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Could not find common type for Int and Unit", diagnostics.msg)
    }

    @Test
    fun `error - return outside function body`() {
        val body = Statement.ReturnStatement(lc, Empty(lc))
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Return outside function body", diagnostics.msg)
    }

    @Test
    fun `error - return with wrong type`() {
        val funDec = Definition.FunctionDeclaration(lc, "f", null, emptyList(), testInt, Statement.ReturnStatement(lc, booleanLiteral))
        val ast = Block(lc, listOf(funDec))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Int, found Boolean", diagnostics.msg)
    }

    @Test
    fun `error - non Boolean in while test`() {
        val body = Statement.WhileStatement(lc, Empty(lc), Empty(lc))
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Boolean, found Unit", diagnostics.msg)
    }

    @Test
    fun `error - operator assignment on non lvalue`() {
        val body = OperatorBinary.AdditionAssignment(lc, intLiteral, Literal.IntLiteral(lc, 4))
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Expected lvalue reference", diagnostics.msg)
    }

    @Test
    fun `error - operator assignment on wrong type rhs`() {
        val varDec = Definition.VariableDeclaration(lc, "x", null, intLiteral)
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.AdditionAssignment(lc, varUse, booleanLiteral)
        val ast = Block(lc, listOf(varDec, body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, mapOf(varUse to varDec))
        assertEquals("Type mismatch: expected Int, found Boolean", diagnostics.msg)
    }

    @Test
    fun `error - operator assignment on wrong type lhs`() {
        val varDec = Definition.VariableDeclaration(lc, "x", null, booleanLiteral)
        val varUse = VariableUse(lc, "x")
        val body = OperatorBinary.AdditionAssignment(lc, varUse, intLiteral)
        val ast = Block(lc, listOf(varDec, body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, mapOf(varUse to varDec))
        assertEquals("Type mismatch: expected Int, found Boolean", diagnostics.msg)
    }

    @Test
    fun `error - add on wrong value rhs`() {
        val body = OperatorBinary.Addition(lc, intLiteral, booleanLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Int, found Boolean", diagnostics.msg)
    }

    @Test
    fun `error - add on wrong value lhs`() {
        val body = OperatorBinary.Addition(lc, booleanLiteral, intLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Int, found Boolean", diagnostics.msg)
    }

    @Test
    fun `error - or on wrong value lhs`() {
        val body = OperatorBinary.LogicalOr(lc, booleanLiteral, intLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Boolean, found Int", diagnostics.msg)
    }

    @Test
    fun `error - or on wrong value rhs`() {
        val body = OperatorBinary.LogicalOr(lc, intLiteral, booleanLiteral)
        val ast = Block(lc, listOf(body))
        val diagnostics = getDiagnostic()
        checkTypes(ast, diagnostics, emptyMap())
        assertEquals("Type mismatch: expected Boolean, found Int", diagnostics.msg)
    }
}