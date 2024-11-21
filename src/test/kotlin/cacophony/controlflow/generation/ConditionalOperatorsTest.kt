package cacophony.controlflow.generation

import cacophony.block
import cacophony.controlflow.cfg
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.controlflow.generation.TestOperators.Companion.eq
import cacophony.controlflow.generation.TestOperators.Companion.eqNode
import cacophony.controlflow.generation.TestOperators.Companion.geq
import cacophony.controlflow.generation.TestOperators.Companion.geqNode
import cacophony.controlflow.generation.TestOperators.Companion.gt
import cacophony.controlflow.generation.TestOperators.Companion.gtNode
import cacophony.controlflow.generation.TestOperators.Companion.leq
import cacophony.controlflow.generation.TestOperators.Companion.leqNode
import cacophony.controlflow.generation.TestOperators.Companion.lt
import cacophony.controlflow.generation.TestOperators.Companion.ltNode
import cacophony.controlflow.generation.TestOperators.Companion.neq
import cacophony.controlflow.generation.TestOperators.Companion.neqNode
import cacophony.controlflow.integer
import cacophony.controlflow.rax
import cacophony.controlflow.returnNode
import cacophony.controlflow.writeRegister
import cacophony.functionDeclaration
import cacophony.ifThenElse
import cacophony.lit
import cacophony.variableDeclaration
import cacophony.variableUse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

class ConditionalOperatorsTest {
    @ParameterizedTest
    @MethodSource("logicalOperators")
    fun `logical operators in conditional mode with non-extracted operands`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val fDef =
            functionDeclaration(
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
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does
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
                        jump("return") {
                            writeRegister(rax, readRegister("result"))
                        }
                    "return" does
                        final {
                            returnNode
                        }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("logicalOperators")
    fun `logical operators in conditional mode with extracted lhs`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val fDef =
            functionDeclaration(
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
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does
                        jump("condition") {
                            writeRegister("x", integer(1))
                        }
                    "condition" does
                        conditional("true", "false") {
                            makeNode(readRegister("x"), integer(2))
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
                        jump("return") {
                            writeRegister(rax, readRegister("result"))
                        }
                    "return" does
                        final {
                            returnNode
                        }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("logicalOperators")
    fun `logical operators in conditional mode with extracted rhs`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val fDef =
            functionDeclaration(
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
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does
                        jump("condition") {
                            writeRegister("x", integer(1))
                        }
                    "condition" does
                        conditional("true", "false") {
                            makeNode(integer(2), readRegister("x"))
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
                        jump("return") {
                            writeRegister(rax, readRegister("result"))
                        }
                    "return" does
                        final {
                            returnNode
                        }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("logicalOperators")
    fun `logical operators in conditional mode with extracted operands`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val fDef =
            functionDeclaration(
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
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does
                        jump("write y") {
                            writeRegister("x", integer(1))
                        }
                    "write y" does
                        jump("condition") {
                            writeRegister("y", integer(2))
                        }
                    "condition" does
                        conditional("true", "false") {
                            makeNode(readRegister("x"), readRegister("y"))
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
                        jump("return") {
                            writeRegister(rax, readRegister("result"))
                        }
                    "return" does
                        final {
                            returnNode
                        }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    companion object {
        @JvmStatic
        fun logicalOperators(): List<Arguments> {
            return listOf(
                argumentSet("eq", eq, eqNode),
                argumentSet("neq", neq, neqNode),
                argumentSet("lt", lt, ltNode),
                argumentSet("leq", leq, leqNode),
                argumentSet("gt", gt, gtNode),
                argumentSet("geq", geq, geqNode),
            )
        }
    }
}
