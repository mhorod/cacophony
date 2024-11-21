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

class BreakTest {
    @Test
    fun `break exits while(true) loop`() {
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
                            breakStatement(),
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
                        conditional("true branch", "exit") {
                            readRegister("x") lt integer(10)
                        }
                    "true branch" does
                        jump("condition") {
                            readRegister("x") addeq integer(1)
                        }
                    "exit" does jump("return") { cacophony.controlflow.writeRegister(rax, unit) }
                    "return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `break exits loop with condition`() {
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
                                breakStatement(),
                                // else
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
                        conditional("exit", "loop condition") {
                            (readRegister("x") mod integer(5)) eq integer(0)
                        }
                    "exit" does jump("return") { cacophony.controlflow.writeRegister(rax, unit) }
                    "return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `block instructions after break are not computed`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                whileLoop(
                    lit(true),
                    block(
                        variableDeclaration("x", lit(2)),
                        breakStatement(),
                        variableDeclaration("y", lit(3)),
                    ),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("write unit to rax") { writeRegister("x", integer(2)) }
                    "write unit to rax" does jump("return") { cacophony.controlflow.writeRegister(rax, unit) }
                    "return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `block instructions are not computed when both branches of if break`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                whileLoop(
                    lit(true),
                    block(
                        variableDeclaration("x", lit(2)),
                        ifThenElse(
                            // if
                            variableUse("x") eq lit(2),
                            // then
                            breakStatement(),
                            // else
                            breakStatement(),
                        ),
                        variableDeclaration("y", lit(5)),
                        returnStatement(variableUse("y")),
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
                            cacophony.controlflow.writeRegister(virtualRegister("x"), integer(2))
                        }
                    "condition" does
                        conditional("exit", "exit") {
                            readRegister("x") eq integer(2)
                        }
                    "exit" does jump("return") { cacophony.controlflow.writeRegister(rax, unit) }
                    "return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
