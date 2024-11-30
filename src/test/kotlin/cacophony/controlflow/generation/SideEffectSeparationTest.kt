package cacophony.controlflow.generation

import cacophony.block
import cacophony.controlflow.cfg
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.controlflow.generation.TestOperators.Companion.add
import cacophony.controlflow.generation.TestOperators.Companion.addNode
import cacophony.controlflow.generation.TestOperators.Companion.div
import cacophony.controlflow.generation.TestOperators.Companion.divNode
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
import cacophony.controlflow.generation.TestOperators.Companion.mod
import cacophony.controlflow.generation.TestOperators.Companion.modNode
import cacophony.controlflow.generation.TestOperators.Companion.mul
import cacophony.controlflow.generation.TestOperators.Companion.mulNode
import cacophony.controlflow.generation.TestOperators.Companion.neq
import cacophony.controlflow.generation.TestOperators.Companion.neqNode
import cacophony.controlflow.generation.TestOperators.Companion.sub
import cacophony.controlflow.generation.TestOperators.Companion.subNode
import cacophony.controlflow.integer
import cacophony.controlflow.writeRegister
import cacophony.lit
import cacophony.semantic.functionDeclaration
import cacophony.variableDeclaration
import cacophony.variableUse
import cacophony.variableWrite
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

/**
* Test if side effects are separated correctly into separate vertices.
* In case of clash the values should be evaluated from left to right.
*/
class SideEffectSeparationTest {
    @ParameterizedTest
    @MethodSource("binaryExpressions")
    fun `write-write clash is separated`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val lhs = variableWrite(variableUse("x"), lit(10))
        val rhs = variableWrite(variableUse("x"), lit(20))

        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(5)),
                    makeExpr(lhs, rhs),
                ),
            )

        // when
        val cfg = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("compute lhs") {
                            writeRegister("x", integer(5))
                        }
                    "compute lhs" does
                        jump("add") {
                            writeRegister("lhs", writeRegister("x", integer(10)))
                        }
                    "add" does
                        jump("exit") {
                            writeRegister(
                                getResultRegister(),
                                makeNode(
                                    readRegister("lhs"),
                                    (writeRegister("x", integer(20))),
                                ),
                            )
                        }
                }
            }
        assertEquivalent(cfg, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("binaryExpressions")
    fun `read-write clash is separated`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val lhs = variableUse("x")
        val rhs = variableWrite(variableUse("x"), lit(20))

        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(5)),
                    makeExpr(lhs, rhs),
                ),
            )

        // when
        val cfg = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("compute lhs") {
                            writeRegister("x", integer(5))
                        }
                    "compute lhs" does
                        jump("add") {
                            writeRegister("lhs", readRegister("x"))
                        }
                    "add" does
                        jump("exit") {
                            writeRegister(
                                getResultRegister(),
                                makeNode(
                                    readRegister("lhs"),
                                    (writeRegister("x", integer(20))),
                                ),
                            )
                        }
                }
            }
        assertEquivalent(cfg, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("binaryExpressions")
    fun `write-read clash is separated`(makeExpr: MakeBinaryExpression, makeNode: MakeBinaryNode) {
        // given
        val lhs = variableWrite(variableUse("x"), lit(10))
        val rhs = variableUse("x")

        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(5)),
                    makeExpr(lhs, rhs),
                ),
            )

        // when
        val cfg = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("compute lhs") {
                            writeRegister("x", integer(5))
                        }
                    "compute lhs" does
                        jump("add") {
                            writeRegister("lhs", writeRegister("x", integer(10)))
                        }
                    "add" does
                        jump("exit") {
                            writeRegister(
                                getResultRegister(),
                                makeNode(
                                    readRegister("lhs"),
                                    readRegister("x"),
                                ),
                            )
                        }
                }
            }
        assertEquivalent(cfg, expectedCFG)
    }

    companion object {
        @JvmStatic
        fun binaryExpressions(): List<Arguments> =
            listOf(
                argumentSet("add", add, addNode),
                argumentSet("sub", sub, subNode),
                argumentSet("mul", mul, mulNode),
                argumentSet("div", div, divNode),
                argumentSet("mod", mod, modNode),
                argumentSet("eq", eq, eqNode),
                argumentSet("neq", neq, neqNode),
                argumentSet("lt", lt, ltNode),
                argumentSet("leq", leq, leqNode),
                argumentSet("gt", gt, gtNode),
                argumentSet("geq", geq, geqNode),
            )
    }
}
