package cacophony.controlflow.generation

import cacophony.add
import cacophony.cfg
import cacophony.controlflow.CFGNode
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.div
import cacophony.eq
import cacophony.functionDeclaration
import cacophony.geq
import cacophony.gt
import cacophony.integer
import cacophony.leq
import cacophony.lit
import cacophony.lnot
import cacophony.lt
import cacophony.minus
import cacophony.mod
import cacophony.mul
import cacophony.neq
import cacophony.not
import cacophony.rax
import cacophony.returnNode
import cacophony.sub
import cacophony.trueValue
import cacophony.writeRegister
import org.junit.jupiter.api.Test

/**
 * Tests if arithmetic/logical expressions are correctly converted to a single CFG node
 */
class SingleCFGNodeGenerationTest {
    private fun assertGeneratedSingleNode(programCFG: ProgramCFG, expectedNode: CFGNode) {
        val function = programCFG.keys.first()
        val expectedCFG =
            cfg {
                fragment(function) {
                    "entry" does jump("return") { writeRegister(rax, expectedNode) }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(programCFG, expectedCFG)
    }

    @Test
    fun `addition generates single addition node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) add lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) add integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `subtraction generates single subtraction node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) sub lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) sub integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `multiplication generates single multiplication node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) mul lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) mul integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `division generates single division node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) div lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) div integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `modulo generates single modulo node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) mod lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) mod integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `unary minus generates single minus node`() {
        // given
        val fDef = functionDeclaration("f", minus(lit(1)))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = minus(integer(1))
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `equals generates single equals node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) eq lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) eq integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `not equals generates single equals node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) neq lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) neq integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `less than generates single less than node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) lt lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) lt integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `less or equal generates single less or equal node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) leq lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) leq integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `greater than generates single greater node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) gt lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) gt integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `greater or equal generates single greater or equal node`() {
        // given
        val fDef = functionDeclaration("f", lit(1) geq lit(2))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = integer(1) geq integer(2)
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `negation generates single negation node`() {
        // given
        val fDef = functionDeclaration("f", lnot(lit(true)))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = not(trueValue)
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

    @Test
    fun `equals with complex operands generates single cfg node`() {
        // given
        val fDef = functionDeclaration("f", expr eq expr)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = exprNode eq exprNode
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `not equals with complex operands generates single cfg node`() {
        // given
        val fDef = functionDeclaration("f", expr neq expr)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = exprNode neq exprNode
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `less than with complex operands generates single cfg node`() {
        // given
        val fDef = functionDeclaration("f", expr lt expr)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = exprNode lt exprNode
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `less or equal with complex operands generates single cfg node`() {
        // given
        val fDef = functionDeclaration("f", expr leq expr)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = exprNode leq exprNode
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `greater than with complex operands generates single cfg node`() {
        // given
        val fDef = functionDeclaration("f", expr gt expr)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = exprNode gt exprNode
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    @Test
    fun `greater or equal with complex operands generates single cfg node`() {
        // given
        val fDef = functionDeclaration("f", expr geq expr)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedNode = exprNode geq exprNode
        assertGeneratedSingleNode(actualCFG, expectedNode)
    }

    private val expr = (lit(1) add lit(2)) mul minus(lit(10) sub lit(5)) mod lit(6)
    private val exprNode = (integer(1) add integer(2)) mul minus(integer(10) sub integer(5)) mod integer(6)
}
