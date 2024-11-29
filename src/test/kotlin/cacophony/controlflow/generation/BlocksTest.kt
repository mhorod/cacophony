package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import org.junit.jupiter.api.Test

class BlocksTest {
    @Test
    fun `block with no expressions generates noop`() {
        // given
        val fDef = functionDeclaration("f", block())

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("exit") {
                            writeRegister(getResultRegister(), unit)
                        }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `block with single non-extracted expression does not generate separate node`() {
        // given
        val fDef = functionDeclaration("f", variableDeclaration("x", block(lit(1))))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("exit") {
                            writeRegister(getResultRegister(), writeRegister("x", integer(1)))
                        }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `block with two expressions generates node for the first one`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(1)),
                    variableDeclaration("y", lit(2)),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("write result to rax") {
                            writeRegister("x", integer(1))
                        }
                    "write result to rax" does
                        jump("exit") {
                            writeRegister(getResultRegister(), writeRegister("y", integer(2)))
                        }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
