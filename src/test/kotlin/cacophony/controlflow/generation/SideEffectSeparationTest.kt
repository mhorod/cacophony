package cacophony.controlflow.generation

import cacophony.add
import cacophony.block
import cacophony.cfg
import cacophony.controlflow.CFGNode
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.div
import cacophony.eq
import cacophony.geq
import cacophony.gt
import cacophony.integer
import cacophony.leq
import cacophony.lit
import cacophony.lt
import cacophony.mod
import cacophony.mul
import cacophony.neq
import cacophony.rax
import cacophony.returnNode
import cacophony.semantic.functionDeclaration
import cacophony.semantic.syntaxtree.Expression
import cacophony.sub
import cacophony.variableDeclaration
import cacophony.variableUse
import cacophony.variableWrite
import cacophony.writeRegister
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource

private typealias MakeBinaryExpression = (Expression, Expression) -> Expression
private typealias MakeBinaryNode = (CFGNode, CFGNode) -> CFGNode

/**
 * Test if side effects are separated correctly into separate vertices.
 * In case of clash the values should be evaluated from left to right.
 */
class SideEffectSeparationTest {
    @ParameterizedTest
    @MethodSource("binaryExpressions")
    fun `write-write clash is separated`() {
        // given
        val lhs = variableWrite(variableUse("x"), lit(10))
        val rhs = variableWrite(variableUse("x"), lit(20))

        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(5)),
                    lhs add rhs,
                ),
            )

        // when
        val cfg = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does
                        jump("compute lhs") {
                            writeRegister("x", integer(5))
                        }
                    "compute lhs" does
                        jump("add") {
                            writeRegister("lhs", writeRegister("x", integer(10)))
                        }
                    "add" does
                        jump("return") {
                            writeRegister(
                                rax,
                                // lhs is read from register
                                readRegister("lhs") add
                                    // rhs does not have to be extracted once lhs is separated
                                    (writeRegister("x", integer(20))),
                            )
                        }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(cfg, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("binaryExpressions")
    fun `read-write clash is separated`() {
        // given
        val lhs = variableUse("x")
        val rhs = variableWrite(variableUse("x"), lit(20))

        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(5)),
                    lhs add rhs,
                ),
            )

        // when
        val cfg = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does
                        jump("compute lhs") {
                            writeRegister("x", integer(5))
                        }
                    "compute lhs" does
                        jump("add") {
                            writeRegister("lhs", readRegister("x"))
                        }
                    "add" does
                        jump("return") {
                            writeRegister(
                                rax,
                                // lhs is read from register
                                readRegister("lhs") add
                                    // rhs does not have to be extracted once lhs is separated
                                    (writeRegister("x", integer(20))),
                            )
                        }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(cfg, expectedCFG)
    }

    @ParameterizedTest
    @MethodSource("binaryExpressions")
    fun `write-read clash is separated`() {
        // given
        val lhs = variableWrite(variableUse("x"), lit(10))
        val rhs = variableUse("x")

        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(5)),
                    lhs add rhs,
                ),
            )

        // when
        val cfg = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does
                        jump("compute lhs") {
                            writeRegister("x", integer(5))
                        }
                    "compute lhs" does
                        jump("add") {
                            writeRegister("lhs", writeRegister("x", integer(10)))
                        }
                    "add" does
                        jump("return") {
                            writeRegister(
                                rax,
                                // lhs is read from register
                                readRegister("lhs") add
                                    // rhs does not have to be extracted once lhs is separated
                                    readRegister("x"),
                            )
                        }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(cfg, expectedCFG)
    }

    companion object {
        private val add: MakeBinaryExpression = { lhs, rhs -> lhs add rhs }
        private val addNode: MakeBinaryNode = { lhs, rhs -> lhs add rhs }

        private val sub: MakeBinaryExpression = { lhs, rhs -> lhs sub rhs }
        private val subNode: MakeBinaryNode = { lhs, rhs -> lhs sub rhs }

        private val mul: MakeBinaryExpression = { lhs, rhs -> lhs mul rhs }
        private val mulNode: MakeBinaryNode = { lhs, rhs -> lhs mul rhs }

        private val div: MakeBinaryExpression = { lhs, rhs -> lhs div rhs }
        private val divNode: MakeBinaryNode = { lhs, rhs -> lhs div rhs }

        private val mod: MakeBinaryExpression = { lhs, rhs -> lhs mod rhs }
        private val modNode: MakeBinaryNode = { lhs, rhs -> lhs mod rhs }

        private val eq: MakeBinaryExpression = { lhs, rhs -> lhs eq rhs }
        private val eqNode: MakeBinaryNode = { lhs, rhs -> lhs eq rhs }

        private val neq: MakeBinaryExpression = { lhs, rhs -> lhs neq rhs }
        private val neqNode: MakeBinaryNode = { lhs, rhs -> lhs neq rhs }

        private val lt: MakeBinaryExpression = { lhs, rhs -> lhs lt rhs }
        private val ltNode: MakeBinaryNode = { lhs, rhs -> lhs lt rhs }

        private val leq: MakeBinaryExpression = { lhs, rhs -> lhs leq rhs }
        private val leqNode: MakeBinaryNode = { lhs, rhs -> lhs leq rhs }

        private val gt: MakeBinaryExpression = { lhs, rhs -> lhs gt rhs }
        private val gtNode: MakeBinaryNode = { lhs, rhs -> lhs gt rhs }

        private val geq: MakeBinaryExpression = { lhs, rhs -> lhs geq rhs }
        private val geqNode: MakeBinaryNode = { lhs, rhs -> lhs geq rhs }

        @JvmStatic
        fun binaryExpressions(): List<Arguments> {
            return listOf(
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
}
