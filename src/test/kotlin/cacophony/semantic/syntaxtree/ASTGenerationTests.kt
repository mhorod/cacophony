package cacophony.semantic.syntaxtree

import cacophony.*
import cacophony.controlflow.functions.Builtin
import cacophony.diagnostics.ASTDiagnostics
import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.pipeline.CacophonyPipeline
import cacophony.utils.*
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ASTGenerationTests {
    private fun assertEquivalentAST(expected: AST, actual: AST) {
        if (expected.isEquivalent(actual)) {
            return
        }
        val expectedStr = TreePrinter().printTree(expected)
        val actualStr = TreePrinter().printTree(actual)
        if (expectedStr != actualStr) {
            fail("Expected AST:\n$expectedStr\nActual AST:\n$actualStr")
        } else {
            fail(
                "printTree(expected) is equal to printTree(actual), but expected and actual are not equivalent",
            )
        }
    }

    private fun locationPair(from: Int, to: Int): Pair<Location, Location> = Pair(Location(from), Location(to))

    private fun anyLocation(): Pair<Location, Location> {
        val mock = mockk<Pair<Location, Location>>()
        every { mock.toString() } returns "(any)"
        every { mock == any() } returns true
        every { mock.first } returns Location(-1)
        every { mock.second } returns Location(-1)
        return mock
    }

    private fun computeASTAndDiagnostics(content: String): Triple<AST?, List<String>, CompileException?> {
        val input = StringInput(content)
        val diagnostics = CacophonyDiagnostics(input)
        return try {
            Triple(CacophonyPipeline(diagnostics).generateAst(input), diagnostics.getErrors(), null)
        } catch (t: CompileException) {
            Triple(null, diagnostics.getErrors(), t)
        }
    }

    private fun mockWrapInFunction(originalAST: AST): AST {
        val program =
            Definition.VariableDefinition(
                anyLocation(),
                MAIN_FUNCTION_IDENTIFIER,
                BaseType.Functional(
                    anyLocation(),
                    emptyList(),
                    BaseType.Basic(anyLocation(), "Int"),
                ),
                LambdaExpression(
                    anyLocation(),
                    emptyList(),
                    BaseType.Basic(anyLocation(), "Int"),
                    Block(
                        anyLocation(),
                        listOf(
                            originalAST,
                            Statement.ReturnStatement(anyLocation(), Literal.IntLiteral(anyLocation(), 0)),
                        ),
                    ),
                ),
            )
        val programCall =
            FunctionCall(
                anyLocation(),
                VariableUse(anyLocation(), MAIN_FUNCTION_IDENTIFIER),
                emptyList(),
            )
        return Block(
            anyLocation(),
            Builtin.all +
                listOf(
                    program,
                    programCall,
                ),
        )
    }

    private fun computeAST(content: String): AST {
        val (ast, _, exc) = computeASTAndDiagnostics(content)
        return ast ?: throw exc!!
    }

    // a bit convoluted, but gets the job done
    private fun getInnerBlock(ast: AST): AST { // unwrap AST
        val mainFunction = ast.children().filterIsInstance<Definition.VariableDefinition>().first()
        return mainFunction
            .children()
            .first()
            .children()
            .first() as Block
    }

    private fun computeType(type: String): Type? {
        val ast = computeAST("let x:$type=()") // that would fail type check, but here we don't care
        val definition = getInnerBlock(ast).children().first() as Definition.VariableDefinition
        return definition.type
    }

    private fun computeFailDiagnostics(content: String): List<String> {
        val (ast, diagnostics, _) = computeASTAndDiagnostics(content)
        assertThat(ast).isNull()
        return diagnostics
    }

    private fun basicType(value: String) = BaseType.Basic(anyLocation(), value)

    private fun literal(value: Int) = Literal.IntLiteral(anyLocation(), value)

    private fun literal(value: Boolean) = Literal.BoolLiteral(anyLocation(), value)

    @Test
    fun `too big int literal`() {
        val diagnostics = computeFailDiagnostics("111111111111111111111111")
        assertThat(diagnostics).anyMatch {
            it.contains("out of range", true)
        }
    }

    @Test
    fun `lexer fail causes ast to not generate`() {
        val diagnostics = computeFailDiagnostics("?1")
        assertThat(diagnostics).isNotEmpty
    }

    @Test
    fun `bool literal`() {
        val actual = computeAST("true")
        val expected = Literal.BoolLiteral(locationPair(0, 3), true)
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `empty block`() {
        val actual = computeAST("()")
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Empty(anyLocation()),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `bool literals in blocks`() {
        val actual = computeAST("(false; ();;); true")
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Block(
                        // false; ();;
                        anyLocation(),
                        listOf(
                            literal(false),
                            Block(
                                anyLocation(),
                                listOf(
                                    Empty(anyLocation()),
                                ),
                            ),
                            Empty(anyLocation()),
                            Empty(anyLocation()),
                        ),
                    ),
                    literal(true),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `bool literal and semicolon`() {
        val actual = computeAST("true;")
        val expected =
            Block(
                locationPair(0, 4),
                listOf(
                    Literal.BoolLiteral(locationPair(0, 3), true),
                    Empty(locationPair(3, 3)),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `bool literals`() {
        val actual = computeAST("true; false; true; true")
        val expected =
            Block(
                locationPair(0, 22),
                listOf(
                    Literal.BoolLiteral(locationPair(0, 3), true),
                    Literal.BoolLiteral(locationPair(6, 10), false),
                    Literal.BoolLiteral(locationPair(13, 16), true),
                    Literal.BoolLiteral(locationPair(19, 22), true),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `if as initialization`() {
        val actual = computeAST("let x: Bool = if false then true else false;")
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Definition.VariableDefinition(
                        anyLocation(),
                        "x",
                        basicType("Bool"),
                        Statement.IfElseStatement(
                            anyLocation(),
                            literal(false),
                            literal(true),
                            literal(false),
                        ),
                    ),
                    Empty(anyLocation()),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `recursive function`() {
        val actual = computeAST("let f = [x: Int] -> Int => f[f[x]]")
        val expected =
            Definition.VariableDefinition(
                anyLocation(),
                "f",
                null,
                LambdaExpression(
                    anyLocation(),
                    listOf(
                        Definition.FunctionArgument(
                            anyLocation(),
                            "x",
                            basicType("Int"),
                        ),
                    ),
                    basicType("Int"),
                    FunctionCall(
                        anyLocation(),
                        VariableUse(
                            anyLocation(),
                            "f",
                        ),
                        listOf(
                            FunctionCall(
                                anyLocation(),
                                VariableUse(
                                    anyLocation(),
                                    "f",
                                ),
                                listOf(
                                    VariableUse(
                                        anyLocation(),
                                        "x",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `operator precedence`() {
        val actual = computeAST("1 + 2 / 3 * 4 - 5")
        val expected =
            OperatorBinary.Subtraction(
                anyLocation(),
                OperatorBinary.Addition(
                    anyLocation(),
                    literal(1),
                    OperatorBinary.Multiplication(
                        anyLocation(),
                        OperatorBinary.Division(
                            anyLocation(),
                            literal(2),
                            literal(3),
                        ),
                        literal(4),
                    ),
                ),
                literal(5),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `parenthesis precedence`() {
        val actual = computeAST("(1 + 2) % 3")
        val expected =
            OperatorBinary.Modulo(
                anyLocation(),
                Block(
                    anyLocation(),
                    listOf(
                        OperatorBinary.Addition(
                            anyLocation(),
                            literal(1),
                            literal(2),
                        ),
                    ),
                ),
                literal(3),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `unary minus`() {
        val actual = computeAST("-0")
        val expected =
            OperatorUnary.Minus(
                locationPair(0, 1),
                Literal.IntLiteral(
                    locationPair(1, 1),
                    0,
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `unary negation`() {
        val actual = computeAST("!true")
        val expected =
            OperatorUnary.Negation(
                locationPair(0, 4),
                Literal.BoolLiteral(
                    locationPair(1, 4),
                    true,
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `addition operator`() {
        val actual = computeAST("let x = 1; x += x + x")
        val xUse =
            VariableUse(
                anyLocation(),
                "x",
            )
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Definition.VariableDefinition(
                        anyLocation(),
                        "x",
                        null,
                        literal(1),
                    ),
                    OperatorBinary.AdditionAssignment(
                        anyLocation(),
                        xUse,
                        OperatorBinary.Addition(
                            anyLocation(),
                            xUse,
                            xUse,
                        ),
                    ),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `subtraction operator`() {
        val actual = computeAST("let x = 1; x -= x - x")
        val xUse =
            VariableUse(
                anyLocation(),
                "x",
            )
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Definition.VariableDefinition(
                        anyLocation(),
                        "x",
                        null,
                        literal(1),
                    ),
                    OperatorBinary.SubtractionAssignment(
                        anyLocation(),
                        xUse,
                        OperatorBinary.Subtraction(
                            anyLocation(),
                            xUse,
                            xUse,
                        ),
                    ),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `multiplication operator`() {
        val actual = computeAST("let x = 1; x *= x * x")
        val xUse =
            VariableUse(
                anyLocation(),
                "x",
            )
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Definition.VariableDefinition(
                        anyLocation(),
                        "x",
                        null,
                        literal(1),
                    ),
                    OperatorBinary.MultiplicationAssignment(
                        anyLocation(),
                        xUse,
                        OperatorBinary.Multiplication(
                            anyLocation(),
                            xUse,
                            xUse,
                        ),
                    ),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `division operator`() {
        val actual = computeAST("let x = 1; x /= x / x")
        val xUse =
            VariableUse(
                anyLocation(),
                "x",
            )
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Definition.VariableDefinition(
                        anyLocation(),
                        "x",
                        null,
                        literal(1),
                    ),
                    OperatorBinary.DivisionAssignment(
                        anyLocation(),
                        xUse,
                        OperatorBinary.Division(
                            anyLocation(),
                            xUse,
                            xUse,
                        ),
                    ),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `modulo operator`() {
        val actual = computeAST("let x = 1; x %= x % x")
        val xUse =
            VariableUse(
                anyLocation(),
                "x",
            )
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Definition.VariableDefinition(
                        anyLocation(),
                        "x",
                        null,
                        literal(1),
                    ),
                    OperatorBinary.ModuloAssignment(
                        anyLocation(),
                        xUse,
                        OperatorBinary.Modulo(
                            anyLocation(),
                            xUse,
                            xUse,
                        ),
                    ),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `logical operators`() {
        val actual = computeAST("!(true && false) || true")
        val expected =
            OperatorBinary.LogicalOr(
                anyLocation(),
                OperatorUnary.Negation(
                    anyLocation(),
                    Block(
                        anyLocation(),
                        listOf(
                            OperatorBinary.LogicalAnd(
                                anyLocation(),
                                literal(true),
                                literal(false),
                            ),
                        ),
                    ),
                ),
                literal(true),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `while loops 1`() {
        val actual = computeAST("while (true) do break;")
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Statement.WhileStatement(
                        anyLocation(),
                        Block(
                            anyLocation(),
                            listOf(
                                literal(true),
                            ),
                        ),
                        Statement.BreakStatement(anyLocation()),
                    ),
                    Empty(anyLocation()),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `while loops 2`() {
        val actual = computeAST("while true do (break;);")
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Statement.WhileStatement(
                        anyLocation(),
                        literal(true),
                        Block(
                            anyLocation(),
                            listOf(
                                Statement.BreakStatement(anyLocation()),
                                Empty(anyLocation()),
                            ),
                        ),
                    ),
                    Empty(anyLocation()),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `if statement`() {
        val actual = computeAST("if true then false")
        val expected =
            Statement.IfElseStatement(
                anyLocation(),
                literal(true),
                literal(false),
                null,
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `if else expression`() {
        val actual = computeAST("let x = if false then 1 else 2")
        val expected =
            Definition.VariableDefinition(
                anyLocation(),
                "x",
                null,
                Statement.IfElseStatement(
                    anyLocation(),
                    literal(false),
                    literal(1),
                    literal(2),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `return statement`() {
        val actual = computeAST("let f = [x: Int] -> Int => return x")
        val expected =
            Definition.VariableDefinition(
                anyLocation(),
                "f",
                null,
                LambdaExpression(
                    anyLocation(),
                    listOf(
                        Definition.FunctionArgument(
                            anyLocation(),
                            "x",
                            basicType("Int"),
                        ),
                    ),
                    basicType("Int"),
                    Statement.ReturnStatement(
                        anyLocation(),
                        VariableUse(
                            anyLocation(),
                            "x",
                        ),
                    ),
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `simple struct`() {
        val actual = computeAST("{x = x}")
        val expected = structDeclaration(structField("x") to variableUse("x"))
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `nested structs`() {
        val actual = computeAST("let f = [x: {a: {b: Int}, c: Int}] -> {x: {c: Int, a: {b: Int}}} => {x = x}")
        val expected =
            typedFunctionDefinition(
                "f",
                null,
                listOf(
                    typedArg("x", structType("a" to structType("b" to basicType("Int")), "c" to basicType("Int"))),
                ),
                structType("x" to structType("a" to structType("b" to basicType("Int")), "c" to basicType("Int"))),
                structDeclaration(structField("x") to variableUse("x")),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `lvalue field access`() {
        val actual = computeAST("x . a . b . c += 2")
        val expected = lvalueFieldRef(lvalueFieldRef(lvalueFieldRef(variableUse("x"), "a"), "b"), "c") addeq literal(2)
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `rvalue field access`() {
        val actual = computeAST("{x = 2}.x.y")
        val expected = rvalueFieldRef(rvalueFieldRef(structDeclaration(structField("x") to literal(2)), "x"), "y")
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `complex expression field access`() {
        val actual = computeAST("(let c = f[a, b]; c).x.y")
        val expected =
            rvalueFieldRef(
                rvalueFieldRef(
                    block(variableDeclaration("c", call(variableUse("f"), variableUse("a"), variableUse("b"))), variableUse("c")),
                    "x",
                ),
                "y",
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `field access on function call`() {
        val actual = computeAST("f[].x")
        val expected =
            rvalueFieldRef(
                call(variableUse("f")),
                "x",
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `function call on field access`() {
        val actual = computeAST("s.f[]")
        val expected =
            call(
                lvalueFieldRef(
                    variableUse("s"),
                    "f",
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `foreign with non-functional type throws`() {
        val diagnostics = computeFailDiagnostics("foreign f: Int")
        assertThat(diagnostics).anyMatch { it.contains(ASTDiagnostics.NonFunctionalForeign.getMessage()) }
    }

    @Test
    fun `foreign statement`() {
        val actual = computeAST("foreign f: [Int] -> Int")
        val expected =
            Definition.ForeignFunctionDeclaration(
                anyLocation(),
                "f",
                BaseType.Functional(anyLocation(), listOf(basicType("Int")), basicType("Int")),
                basicType("Int"),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `simple allocation`() {
        val actual = computeAST("$2")
        val expected =
            Allocation(
                anyLocation(),
                literal(2),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `simple dereference`() {
        val actual = computeAST("@x")
        val expected =
            Dereference(
                anyLocation(),
                variableUse("x"),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `dereference assignment`() {
        val actual = computeAST("@x=2")
        val expected =
            OperatorBinary.Assignment(
                anyLocation(),
                Dereference(
                    anyLocation(),
                    variableUse("x"),
                ),
                literal(2),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `allocation of struct field`() {
        val actual = computeAST("\$x.a")
        val expected =
            Allocation(
                anyLocation(),
                lvalueFieldRef(
                    variableUse("x"),
                    "a",
                ),
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `field of dereference`() {
        val actual = computeAST("@x.a")
        val expected =
            lvalueFieldRef(
                Dereference(
                    anyLocation(),
                    variableUse("x"),
                ),
                "a",
            )
        assertEquivalentAST(mockWrapInFunction(expected), actual)
    }

    @Test
    fun `compute basic type`() {
        val actual = computeType("Type")
        val expected =
            BaseType.Basic(
                anyLocation(),
                "Type",
            )
        assertThat(areEquivalentTypes(expected, actual))
    }

    @Test
    fun `compute functional type`() {
        val actual = computeType("[Type1]->Type2")
        val expected =
            BaseType.Functional(
                anyLocation(),
                listOf(BaseType.Basic(anyLocation(), "Type1")),
                BaseType.Basic(anyLocation(), "Type2"),
            )
        assertThat(areEquivalentTypes(expected, actual))
    }

    @Test
    fun `compute structural type`() {
        val actual = computeType("{x:Type1, y:Type2}")
        val expected =
            BaseType.Structural(
                anyLocation(),
                mapOf(
                    "x" to BaseType.Basic(anyLocation(), "Type1"),
                    "y" to BaseType.Basic(anyLocation(), "Type2"),
                ),
            )
        assertThat(areEquivalentTypes(expected, actual))
    }
}
