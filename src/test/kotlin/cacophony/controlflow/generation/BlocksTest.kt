package cacophony.controlflow.generation

import cacophony.block
import cacophony.cfg
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.functionDeclaration
import cacophony.integer
import cacophony.lit
import cacophony.rax
import cacophony.returnNode
import cacophony.unit
import cacophony.variableDeclaration
import cacophony.writeRegister
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
                fragment(fDef) {
                    "entry" does
                        jump("return") {
                            writeRegister(rax, unit)
                        }
                    "return" does
                        final {
                            returnNode
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
                fragment(fDef) {
                    "entry" does
                        jump("return") {
                            writeRegister(rax, writeRegister("x", integer(1)))
                        }
                    "return" does
                        final {
                            returnNode
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
                fragment(fDef) {
                    "entry" does
                        jump("write result to rax") {
                            writeRegister("x", integer(1))
                        }
                    "write result to rax" does
                        jump("return") {
                            writeRegister(rax, writeRegister("y", integer(2)))
                        }
                    "return" does
                        final {
                            returnNode
                        }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
