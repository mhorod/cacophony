package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.CFGNode
import cacophony.controlflow.add
import cacophony.controlflow.cfg
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.controlflow.integer
import cacophony.controlflow.minus
import cacophony.controlflow.mod
import cacophony.controlflow.mul
import cacophony.controlflow.sub
import cacophony.controlflow.writeRegister
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Tests if arithmetic/logical expressions are correctly converted to a single CFG node
 */
class SingleCFGNodeGenerationTest {
    private fun assertGeneratedSingleNode(programCFG: ProgramCFG, expectedNode: CFGNode) {
        val function = programCFG.keys.first()
        val expectedCFG =
            cfg {
                fragment(function, listOf(argStack(0)), 8) {
                    "bodyEntry" does jump("exit") { writeRegister(getResultRegister(), expectedNode) }
                }
            }
        assertEquivalent(programCFG, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("binaryExpressions")
    fun `binary operator generates single cfg node`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val fDef = functionDeclaration("f", makeExpr(lit(1), lit(2)))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = makeNode(integer(1), integer(2))
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @ParameterizedTest
    @MethodSource("unaryExpressions")
    fun `unary operator generates single cfg node`(makeExpr: MakeUnaryExpression, makeNode: MakeUnaryNode) {
        // given
        val fDef = functionDeclaration("f", makeExpr(lit(1)))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = makeNode(integer(1))
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `complex arithmetic expression generates single cfg node`() {
        // given
        val fDef =
            functionDeclaration("f", expr)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = exprNode
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @ParameterizedTest
    @MethodSource("binaryExpressions")
    fun `binary operator with complex operands generates single cfg node`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val fDef = functionDeclaration("f", makeExpr(expr, expr))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = makeNode(exprNode, exprNode)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    companion object {
        @JvmStatic
        fun binaryExpressions() = TestOperators.binaryExpressions()

        @JvmStatic
        fun unaryExpressions() = TestOperators.unaryExpressions()
    }

    private val expr = (lit(1) add lit(2)) mul minus(lit(10) sub lit(5)) mod lit(6)
    private val exprNode = (integer(1) add integer(2)) mul minus(integer(10) sub integer(5)) mod integer(6)
}
