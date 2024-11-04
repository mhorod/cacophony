package cacophony.semantic.syntaxtree

import cacophony.pipeline.CacophonyPipeline
import cacophony.utils.*
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ASTGenerationTests {
    private fun assertEquivalentAST(
        expected: AST,
        actual: AST,
    ) {
        if (!expected.isEquivalent(actual)) {
            println(expected)
            println(actual)
            assertTrue(false)
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

    private fun computeASTAndDiagnostics(content: String): Pair<AST?, SimpleDiagnostics> {
        val input = StringInput(content)
        val diagnostics = SimpleDiagnostics(input)
        return try {
            Pair(CacophonyPipeline(diagnostics).generateAST(input), diagnostics)
        } catch (t: CompileException) {
            Pair(null, diagnostics)
        }
    }

    private fun computeAST(content: String): AST = computeASTAndDiagnostics(content).first!!

    private fun computeFailDiagnostics(content: String): SimpleDiagnostics {
        val (ast, diagnostics) = computeASTAndDiagnostics(content)
        assertThat(ast).isNull()
        return diagnostics
    }

    @Test
    fun `bool literals`() {
        val actual = computeAST("true; false; true; true")
        val expected =
            AST(
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
            AST(
                anyLocation(),
                listOf(
                    Definition.VariableDeclaration(
                        anyLocation(),
                        "x",
                        Type.Basic(
                            anyLocation(),
                            "Bool",
                        ),
                        Statement.IfElseStatement(
                            anyLocation(),
                            Literal.BoolLiteral(
                                anyLocation(),
                                false,
                            ),
                            Literal.BoolLiteral(
                                anyLocation(),
                                true,
                            ),
                            Literal.BoolLiteral(
                                anyLocation(),
                                false,
                            ),
                        ),
                    ),
                    Empty(anyLocation()),
                ),
            )
        assertEquivalentAST(expected, actual)
    }

    @Test
    fun `too big int literal`() {
        val diagnostics = computeFailDiagnostics("111111111111111111111111111111111111111111111111111111111111111111")
        assertThat(diagnostics.getErrors()).anyMatch {
            it.message.contains("out of range", true)
        }
    }
}
