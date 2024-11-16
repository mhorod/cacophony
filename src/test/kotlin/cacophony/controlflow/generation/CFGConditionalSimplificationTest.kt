package cacophony.controlflow.generation

import cacophony.block
import cacophony.cfg
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.controlflow.programCfgToGraphviz
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
import cacophony.variableDeclaration
import cacophony.variableUse
import cacophony.writeRegister
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
    fun `if statement with logical and with false lhs reduces to false branch`() {
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
                    "entry" does jump("writeRax") { writeRegister(virtualRegister("result"), integer(22)) }
                    "writeRax" does jump("return") { writeRegister(rax, registerUse(virtualRegister("result"))) }
                    "return" does final { returnNode }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with logical or with true lhs reduces to true branch`() {
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
                    "entry" does jump("writeRax") { writeRegister(virtualRegister("result"), integer(11)) }
                    "writeRax" does jump("return") { writeRegister(rax, registerUse(virtualRegister("result"))) }
                    "return" does final { returnNode }
                }
            }
        println(programCfgToGraphviz(actualCFG))
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
                    lit(false) land (
                        lit(true) lor (
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
                    "entry" does jump("writeRax") { writeRegister(virtualRegister("result"), integer(22)) }
                    "writeRax" does jump("return") { writeRegister(rax, registerUse(virtualRegister("result"))) }
                    "return" does final { returnNode }
                }
            }
        println(programCfgToGraphviz(actualCFG))
        assertEquivalent(actualCFG, expectedCFG)
    }
}
