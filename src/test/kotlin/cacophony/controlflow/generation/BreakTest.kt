package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.controlflow.mod
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
        val actualCFG = testPipeline().generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("condition") {
                            writeRegister(virtualRegister("x"), integer(0))
                        }
                    "condition" does
                        conditional("true branch", "exitWhile") {
                            readRegister("x") lt integer(10)
                        }
                    "true branch" does
                        jump("condition") {
                            readRegister("x") addeq integer(1)
                        }
                    "exitWhile" does jump("exit") { writeRegister(getResultRegister(), unit) }
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
        val actualCFG = testPipeline().generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("loop condition") {
                            writeRegister(virtualRegister("x"), integer(0))
                        }
                    "loop condition" does
                        conditional("increment x", "exitWhile") {
                            readRegister("x") lt integer(10)
                        }
                    "increment x" does
                        jump("check x mod 5") {
                            readRegister("x") addeq integer(1)
                        }
                    "check x mod 5" does
                        conditional("exitWhile", "loop condition") {
                            (readRegister("x") mod integer(5)) eq integer(0)
                        }
                    "exitWhile" does jump("exit") { writeRegister(getResultRegister(), unit) }
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
        val actualCFG = testPipeline().generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does jump("write unit to rax") { writeRegister("x", integer(2)) }
                    "write unit to rax" does jump("exit") { writeRegister(getResultRegister(), unit) }
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
                        variableDeclaration("y", empty()),
                        returnStatement(variableUse("y")),
                    ),
                ),
            )

        // when
        val actualCFG = testPipeline().generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("condition") {
                            writeRegister(virtualRegister("x"), integer(2))
                        }
                    "condition" does
                        conditional("exitWhile", "exitWhile") {
                            readRegister("x") eq integer(2)
                        }
                    "exitWhile" does jump("exit") { writeRegister(getResultRegister(), unit) }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
