package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.addeq
import cacophony.controlflow.cfg
import cacophony.controlflow.eq
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.controlflow.integer
import cacophony.controlflow.lt
import cacophony.controlflow.mod
import cacophony.controlflow.rax
import cacophony.controlflow.returnNode
import cacophony.controlflow.unit
import org.junit.jupiter.api.Test

class ReturnTest {
    @Test
    fun `return exits while(true) loop`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(0)),
                    whileLoop(
                        // condition
                        lit(true),
                        // body
                        ifThenElse(
                            // if
                            variableUse("x") lt lit(10),
                            // then
                            block(
                                variableUse("x") addeq lit(1),
                            ),
                            // else
                            returnStatement(lit(0)),
                        ),
                    ),
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
                            cacophony.controlflow.writeRegister(virtualRegister("x"), integer(0))
                        }
                    "condition" does
                        conditional("true branch", "return from loop") {
                            readRegister("x") lt integer(10)
                        }
                    "true branch" does
                        jump("condition") {
                            readRegister("x") addeq integer(1)
                        }
                    "return from loop" does
                        jump("loop return") {
                            cacophony.controlflow.writeRegister(rax, integer(0))
                        }
                    "loop return" does final { returnNode }
                    "return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `return exits loop with condition`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(0)),
                    whileLoop(
                        // condition
                        variableUse("x") lt lit(10),
                        // body
                        block(
                            variableUse("x") addeq lit(1),
                            ifThen(
                                // if
                                (variableUse("x") mod lit(5)) eq lit(0),
                                // then
                                returnStatement(lit(0)),
                            ),
                        ),
                    ),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does
                        jump("loop condition") {
                            cacophony.controlflow.writeRegister(virtualRegister("x"), integer(0))
                        }
                    "loop condition" does
                        conditional("increment x", "exit") {
                            readRegister("x") lt integer(10)
                        }
                    "increment x" does
                        jump("check x mod 5") {
                            readRegister("x") addeq integer(1)
                        }
                    "check x mod 5" does
                        conditional("return from loop", "loop condition") {
                            (readRegister("x") mod integer(5)) eq integer(0)
                        }
                    "exit" does jump("return") { cacophony.controlflow.writeRegister(rax, unit) }
                    "return" does final { returnNode }
                    "return from loop" does jump("loop return") { cacophony.controlflow.writeRegister(rax, integer(0)) }
                    "loop return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `block instructions after return are not computed`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                block(
                    returnStatement(lit(1)),
                    variableDeclaration("x", lit(2)),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("return") { cacophony.controlflow.writeRegister(rax, integer(1)) }
                    "return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `block instructions are not computed when both branches of if return`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(2)),
                    ifThenElse(
                        // if
                        variableUse("x") eq lit(2),
                        // then
                        returnStatement(lit(1)),
                        // else
                        returnStatement(lit(2)),
                    ),
                    variableDeclaration("y", lit(5)),
                    returnStatement(variableUse("y")),
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
                            cacophony.controlflow.writeRegister(virtualRegister("x"), integer(2))
                        }
                    "condition" does
                        conditional("write 1 to rax", "write 2 to rax") {
                            readRegister("x") eq integer(2)
                        }
                    "write 1 to rax" does
                        jump("return 1") {
                            cacophony.controlflow.writeRegister(rax, integer(1))
                        }
                    "write 2 to rax" does
                        jump("return 2") {
                            cacophony.controlflow.writeRegister(rax, integer(2))
                        }
                    "return 1" does final { returnNode }
                    "return 2" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
