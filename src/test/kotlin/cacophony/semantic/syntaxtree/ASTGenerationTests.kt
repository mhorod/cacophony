package cacophony.semantic.syntaxtree

import cacophony.pipeline.CacophonyPipeline
import cacophony.utils.*
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ASTGenerationTests {
    private fun assertEquivalentAST(
        expected: AST,
        actual: AST,
    ) {
        if (expected.isEquivalent(actual)) {
            return
        }
        val expectedStr = TreePrinter().printTree(expected)
        val actualStr = TreePrinter().printTree(actual)
        if (expectedStr != actualStr) {
            fail("Expected AST:\n$expectedStr\nActual AST:\n$actualStr")
        } else {
            fail("printTree(expected) is equal to printTree(actual), but expected and actual are not equivalent")
        }
    }

    private fun locationPair(
        from: Int,
        to: Int,
    ): Pair<Location, Location> = Pair(Location(from), Location(to))

    private fun anyLocation(): Pair<Location, Location> {
        val mock = mockk<Pair<Location, Location>>()
        every { mock.toString() } returns "(any)"
        every { mock == any() } returns true
        every { mock.first } returns Location(-1)
        every { mock.second } returns Location(-1)
        return mock
    }

    private fun SimpleDiagnostics.extractErrors(): List<String> {
        return getErrors().map { it.message }
    }

    private fun computeASTAndDiagnostics(content: String): Triple<AST?, List<String>, CompileException?> {
        val input = StringInput(content)
        val diagnostics = SimpleDiagnostics(input)
        return try {
            Triple(CacophonyPipeline(diagnostics).generateAST(input), diagnostics.extractErrors(), null)
        } catch (t: CompileException) {
            Triple(null, diagnostics.extractErrors(), t)
        }
    }

    private fun computeAST(content: String): AST {
        val (ast, _, exc) = computeASTAndDiagnostics(content)
        return ast ?: throw exc!!
    }

    private fun computeFailDiagnostics(content: String): List<String> {
        val (ast, diagnostics, _) = computeASTAndDiagnostics(content)
        assertThat(ast).isNull()
        return diagnostics
    }

    private fun basicType(value: String) = Type.Basic(anyLocation(), value)

    private fun literal(value: Int) = Literal.IntLiteral(anyLocation(), value)

    private fun literal(value: Boolean) = Literal.BoolLiteral(anyLocation(), value)

    @Test
    fun `too big int literal`() {
        val diagnostics = computeFailDiagnostics("111111111111111111111111")
        assertThat(diagnostics).anyMatch {
            it.contains("out of range", true)
        }
    }

    // TODO fix diagnostics
    @Disabled
    @Test
    fun `lexer fail causes ast to not generate`() {
        val diagnostics = computeFailDiagnostics("?1")
        assertThat(diagnostics).isNotEmpty()
    }

    @Test
    fun `bool literal`() {
        val actual = computeAST("true")
        val expected = Literal.BoolLiteral(locationPair(0, 3), true)
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
    }

    @Test
    fun `bool literals in blocks`() {
        val actual = computeAST("(false; ();;); true")
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Block( // false; ();;
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
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
    }

    @Test
    fun `if as initialization`() {
        val actual = computeAST("let x: Bool = if false then true else false;")
        val expected =
            Block(
                anyLocation(),
                listOf(
                    Definition.VariableDeclaration(
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
        assertEquivalentAST(expected, actual)
    }

    @Test
    fun `recursive function`() {
        val actual = computeAST("let f = [x: Int] -> Int => f[f[x]]")
        val expected =
            Definition.FunctionDeclaration(
                anyLocation(),
                "f",
                null,
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
            )
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
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
                    Definition.VariableDeclaration(
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
        assertEquivalentAST(expected, actual)
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
                    Definition.VariableDeclaration(
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
        assertEquivalentAST(expected, actual)
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
                    Definition.VariableDeclaration(
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
        assertEquivalentAST(expected, actual)
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
                    Definition.VariableDeclaration(
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
        assertEquivalentAST(expected, actual)
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
                    Definition.VariableDeclaration(
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
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
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
        assertEquivalentAST(expected, actual)
    }

    @Test
    fun `if else expression`() {
        val actual = computeAST("let x = if false then 1 else 2")
        val expected =
            Definition.VariableDeclaration(
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
        assertEquivalentAST(expected, actual)
    }

    @Test
    fun `return statement`() {
        val actual = computeAST("let f = [x: Int] -> Int => return x")
        val expected =
            Definition.FunctionDeclaration(
                anyLocation(),
                "f",
                null,
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
            )
        assertEquivalentAST(expected, actual)
    }
}