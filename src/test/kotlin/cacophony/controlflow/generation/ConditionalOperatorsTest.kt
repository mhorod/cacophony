package cacophony.controlflow.generation

import cacophony.block
import cacophony.controlflow.integer
import cacophony.controlflow.writeRegister
import cacophony.ifThenElse
import cacophony.intFunctionDefinition
import cacophony.lit
import cacophony.variableDeclaration
import cacophony.variableUse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ConditionalOperatorsTest {
    @ParameterizedTest
    @MethodSource("logicalOperators")
    fun `logical operators in conditional mode with non-extracted operands`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val fDef =
            intFunctionDefinition(
                "f",
                ifThenElse(
                    // if
                    makeExpr(lit(1), lit(2)),
                    // then
                    lit(3),
                    // else
                    lit(4),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    conditional("true", "false") {
                        makeNode(integer(1), integer(2))
                    }
                "true" does
                    jump("write result to rax") {
                        writeRegister("result", integer(3))
                    }
                "false" does
                    jump("write result to rax") {
                        writeRegister("result", integer(4))
                    }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(getResultRegister(), registerUse("result"))
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("logicalOperators")
    fun `logical operators in conditional mode with extracted lhs`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val fDef =
            intFunctionDefinition(
                "f",
                ifThenElse(
                    // if
                    makeExpr(
                        block(variableDeclaration("x", lit(1)), variableUse("x")),
                        lit(2),
                    ),
                    // then
                    lit(3),
                    // else
                    lit(4),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("condition") {
                        writeRegister("x", integer(1))
                    }
                "condition" does
                    conditional("true", "false") {
                        makeNode(registerUse("x"), integer(2))
                    }
                "true" does
                    jump("write result to rax") {
                        writeRegister("result", integer(3))
                    }
                "false" does
                    jump("write result to rax") {
                        writeRegister("result", integer(4))
                    }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(getResultRegister(), registerUse("result"))
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("logicalOperators")
    fun `logical operators in conditional mode with extracted rhs`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val fDef =
            intFunctionDefinition(
                "f",
                ifThenElse(
                    // if
                    makeExpr(
                        lit(2),
                        block(variableDeclaration("x", lit(1)), variableUse("x")),
                    ),
                    // then
                    lit(3),
                    // else
                    lit(4),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("condition") {
                        writeRegister("x", integer(1))
                    }
                "condition" does
                    conditional("true", "false") {
                        makeNode(integer(2), registerUse("x"))
                    }
                "true" does
                    jump("write result to rax") {
                        writeRegister("result", integer(3))
                    }
                "false" does
                    jump("write result to rax") {
                        writeRegister("result", integer(4))
                    }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(getResultRegister(), registerUse("result"))
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("logicalOperators")
    fun `logical operators in conditional mode with extracted operands`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val fDef =
            intFunctionDefinition(
                "f",
                ifThenElse(
                    // if
                    makeExpr(
                        block(variableDeclaration("x", lit(1)), variableUse("x")),
                        block(variableDeclaration("y", lit(2)), variableUse("y")),
                    ),
                    // then
                    lit(3),
                    // else
                    lit(4),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("write y") {
                        writeRegister("x", integer(1))
                    }
                "write y" does
                    jump("condition") {
                        writeRegister("y", integer(2))
                    }
                "condition" does
                    conditional("true", "false") {
                        makeNode(registerUse("x"), registerUse("y"))
                    }
                "true" does
                    jump("write result to rax") {
                        writeRegister("result", integer(3))
                    }
                "false" does
                    jump("write result to rax") {
                        writeRegister("result", integer(4))
                    }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(getResultRegister(), registerUse("result"))
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    companion object {
        @JvmStatic
        fun logicalOperators(): List<Arguments> = TestOperators.logicalOperators()
    }
}
