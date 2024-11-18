package cacophony.controlflow.generation

import cacophony.block
import cacophony.cfg
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.eq
import cacophony.functionDeclaration
import cacophony.ifThenElse
import cacophony.integer
import cacophony.land
import cacophony.lit
import cacophony.lor
import cacophony.rax
import cacophony.registerUse
import cacophony.returnNode
import cacophony.returnStatement
import cacophony.trueValue
import cacophony.utils.StringInput
import cacophony.variableDeclaration
import cacophony.variableUse
import cacophony.whileLoop
import cacophony.writeRegister
import com.sun.tools.javac.jvm.ByteCodes.lor
import org.junit.jupiter.api.Test

/**
 * Part of CFG generation tests.
 *
 * Tests if conditional statements are simplified correctly when the condition value is known at compile time.
 */
class CFGConditionalSimplificationTest {
    @Test
    fun `if statement with true condition reduces to true branch`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                ifThenElse(lit(true), lit(11), lit(22)),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("return") { writeRegister(rax, integer(11)) }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with false condition reduces to false branch`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                ifThenElse(lit(false), lit(11), lit(22)),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("return") { writeRegister(rax, integer(22)) }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with logical and with false lhs skips computing rhs`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                ifThenElse(
                    // if
                    lit(false) land (
                            block(
                                variableDeclaration("x", lit(10)),
                                variableDeclaration("y", lit(20)),
                                (variableUse("x") eq variableUse("y")),
                            )
                            ),
                    // then
                    lit(11),
                    // else
                    lit(22),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("write result to rax") { writeRegister(virtualRegister("result"), integer(22)) }
                    "write result to rax" does
                            jump("return") {
                                writeRegister(
                                    rax,
                                    registerUse(virtualRegister("result")),
                                )
                            }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with logical or with true lhs skips computing rhs`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                ifThenElse(
                    // if
                    lit(true) lor (
                            block(
                                variableDeclaration("x", lit(10)),
                                variableDeclaration("y", lit(20)),
                                (variableUse("x") eq variableUse("y")),
                            )
                            ),
                    // then
                    lit(11),
                    // else
                    lit(22),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("write result to rax") { writeRegister(virtualRegister("result"), integer(11)) }
                    "write result to rax" does
                            jump("return") {
                                writeRegister(
                                    rax,
                                    registerUse(virtualRegister("result")),
                                )
                            }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with logical or with false lhs computes rhs`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                ifThenElse(
                    // if
                    lit(false) lor (
                            block(
                                variableDeclaration("x", lit(true)),
                                variableUse("x"),
                            )
                            ),
                    // then
                    lit(11),
                    // else
                    lit(22),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("condition on x") { writeRegister(virtualRegister("x"), trueValue) }
                    "condition on x" does
                            conditional("write 11 to result", "write 22 to result") {
                                registerUse(virtualRegister("x"))
                            }
                    "write 11 to result" does
                            jump("write result to rax") {
                                writeRegister(
                                    virtualRegister("result"),
                                    integer(11),
                                )
                            }
                    "write 22 to result" does
                            jump("write result to rax") {
                                writeRegister(
                                    virtualRegister("result"),
                                    integer(22),
                                )
                            }
                    "write result to rax" does
                            jump("return") {
                                writeRegister(
                                    rax,
                                    registerUse(virtualRegister("result")),
                                )
                            }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with logical and with true lhs computes rhs`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                ifThenElse(
                    // if
                    lit(true) land (
                            block(
                                variableDeclaration("x", lit(true)),
                                variableUse("x"),
                            )
                            ),
                    // then
                    lit(11),
                    // else
                    lit(22),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("condition on x") { writeRegister(virtualRegister("x"), trueValue) }
                    "condition on x" does
                            conditional("write 11 to result", "write 22 to result") {
                                registerUse(virtualRegister("x"))
                            }
                    "write 11 to result" does
                            jump("write result to rax") {
                                writeRegister(
                                    virtualRegister("result"),
                                    integer(11),
                                )
                            }
                    "write 22 to result" does
                            jump("write result to rax") {
                                writeRegister(
                                    virtualRegister("result"),
                                    integer(22),
                                )
                            }
                    "write result to rax" does
                            jump("return") {
                                writeRegister(
                                    rax,
                                    registerUse(virtualRegister("result")),
                                )
                            }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with nested ifs and ors`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                ifThenElse(
                    // if
                    (
                            lit(true)
                                    land
                                    (lit(false) lor lit(false))
                            )
                            land
                            (
                                    lit(true) lor (
                                            lit(false)
                                                    land
                                                    block(
                                                        variableDeclaration("x", lit(10)),
                                                        variableDeclaration("y", lit(20)),
                                                        (variableUse("x") eq variableUse("y")),
                                                    )
                                            )
                                    ),
                    // then
                    lit(11),
                    // else
                    lit(22),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("write result to rax") { writeRegister(virtualRegister("result"), integer(22)) }
                    "write result to rax" does
                            jump("return") {
                                writeRegister(
                                    rax,
                                    registerUse(virtualRegister("result")),
                                )
                            }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `while loop with true condition never exits`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                block(
                    whileLoop(lit(true), variableDeclaration("x", lit(10))),
                    variableDeclaration("y", lit(20)),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does
                            jump("entry") {
                                writeRegister(virtualRegister("x"), integer(10))
                            }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `while loop with false condition never enters`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                block(
                    whileLoop(lit(false), variableDeclaration("x", lit(10))),
                    variableDeclaration("y", lit(20)),
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
                            jump("write result to rax") {
                                writeRegister(virtualRegister("y"), integer(20))
                            }
                    "write result to rax" does
                            jump("return") {
                                writeRegister(rax, registerUse(virtualRegister("y")))
                            }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `deep condition made of true, false, and, or simplifies to single branch`() {
        // given
        val fDef = functionDeclaration(
            "f",
            ifThenElse(
                (lit(false) lor
                                (lit(false) lor
                                        (lit(false) lor
                                                (lit(false) lor
                                                        (lit(false) lor
                                                                (
                                                                        lit(false) lor
                                                                                lit(true)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )

                        ),
                lit(12),
                lit(24)
            )
        )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(actualCFG.keys.first()) {
                    "entry" does jump("write result to rax") { writeRegister("result", integer(12)) }
                    "write result to rax" does jump("return") { writeRegister(rax, readRegister("result")) }
                    "return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `condition with nested ifs simplifies to single branch`() {
        // given
        val fDef = functionDeclaration(
            "f",
            ifThenElse(
                ifThenElse(
                    lit(true),
                    lit(false),
                    lit(true),
                ),
                lit(10),
                lit(20),
            )
        )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("write result to rax") { writeRegister("result", integer(20)) }
                    "write result to rax" does jump("return") { writeRegister(rax, readRegister("result")) }
                    "return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `condition if ifs nested in branches simplifies to single branch`() {
        // given
        val fDef = functionDeclaration(
            "f",
            ifThenElse(
                lit(true),
                ifThenElse(
                    ifThenElse(
                        lit(false),
                        lit(false),
                        lit(true),
                    ),
                    lit(11),
                    lit(12),
                ),
                ifThenElse(
                    lit(true),
                    lit(13),
                    lit(15),
                ),
            )
        )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("write result to rax") { writeRegister("result", integer(11)) }
                    "write result to rax" does jump("return") { writeRegister(rax, readRegister("result")) }
                    "return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
