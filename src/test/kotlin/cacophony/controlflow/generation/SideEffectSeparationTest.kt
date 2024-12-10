package cacophony.controlflow.generation

import cacophony.block
import cacophony.controlflow.cfg
import cacophony.controlflow.integer
import cacophony.controlflow.unit
import cacophony.controlflow.writeRegister
import cacophony.empty
import cacophony.lit
import cacophony.testPipeline
import cacophony.unitFunctionDefinition
import cacophony.variableDeclaration
import cacophony.variableUse
import cacophony.variableWrite
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
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
            unitFunctionDefinition(
                "f",
                block(
                    variableDeclaration("x", lit(5)),
                    variableDeclaration("y", makeExpr(lhs, rhs)),
                    empty(),
                ),
            )

        // when
        val cfg = testPipeline().generateControlFlowGraph(fDef)

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
                        jump("empty") {
                            writeRegister(
                                "y",
                                makeNode(
                                    readRegister("lhs"),
                                    (writeRegister("x", integer(20))),
                                ),
                            )
                        }
                    "empty" does
                        jump("exit") {
                            writeRegister(getResultRegister(), unit)
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
            unitFunctionDefinition(
                "f",
                block(
                    variableDeclaration("x", lit(5)),
                    variableDeclaration("y", makeExpr(lhs, rhs)),
                    empty(),
                ),
            )

        // when
        val cfg = testPipeline().generateControlFlowGraph(fDef)

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
                        jump("empty") {
                            writeRegister(
                                "y",
                                makeNode(
                                    readRegister("lhs"),
                                    (writeRegister("x", integer(20))),
                                ),
                            )
                        }
                    "empty" does
                        jump("exit") {
                            writeRegister(getResultRegister(), unit)
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
            unitFunctionDefinition(
                "f",
                block(
                    variableDeclaration("x", lit(5)),
                    variableDeclaration("y", makeExpr(lhs, rhs)),
                    empty(),
                ),
            )

        // when
        val cfg = testPipeline().generateControlFlowGraph(fDef)

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
                        jump("empty") {
                            writeRegister(
                                "y",
                                makeNode(
                                    readRegister("lhs"),
                                    readRegister("x"),
                                ),
                            )
                        }
                    "empty" does
                        jump("exit") {
                            writeRegister(getResultRegister(), unit)
                        }
                }
            }
        assertEquivalent(cfg, expectedCFG)
    }

    companion object {
        @JvmStatic
        fun binaryExpressions(): List<Arguments> = TestOperators.binaryExpressions()
    }
}
