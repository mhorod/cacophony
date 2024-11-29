package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.controlflow.CFGNode.Companion.TRUE
import cacophony.controlflow.CFGNode.Companion.UNIT
import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.pipeline.CacophonyPipeline
import io.mockk.mockk
import org.junit.jupiter.api.Test

fun argStack(offset: Int) = VariableAllocation.OnStack(offset)

fun argReg(reg: Register.VirtualRegister) = VariableAllocation.InRegister(reg)

class CFGGenerationTest {
    companion object {
        private val diagnostics = CacophonyDiagnostics(mockk())
        val pipeline = CacophonyPipeline(diagnostics)
    }

    @Test
    fun `CFG of empty function`() {
        // given
        val emptyBlock = block()
        val fDef = functionDeclaration("f", emptyBlock)

        // when
        val cfg = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    val resReg = getResultRegister()
                    "bodyEntry" does jump("exit") { writeRegister(resReg, UNIT) }
                }
            }

        assertEquivalent(cfg, expectedCFG)
    }

    @Test
    fun `CFG of function returning true`() {
        // given
        val fDef = functionDeclaration("f", lit(true))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does jump("exit") { writeRegister(getResultRegister(), TRUE) }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `CFG of if with variable condition`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                block(
                    variableDeclaration("x", lit(true)),
                    ifThenElse(
                        // if
                        variableUse("x"),
                        // then
                        lit(11),
                        // else
                        lit(22),
                    ),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does jump("condition") { writeRegister(virtualRegister("x"), integer(1)) }
                    "condition" does
                        conditional("true", "false") {
                            registerUse(virtualRegister("x"))
                        }
                    "true" does jump("end") { writeRegister(virtualRegister("t"), integer(11)) }
                    "false" does jump("end") { writeRegister(virtualRegister("t"), integer(22)) }
                    "end" does jump("exit") { writeRegister(getResultRegister(), registerUse(virtualRegister("t"))) }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `CFG of while loop with variable condition`() {
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
                        variableUse("x") addeq lit(1),
                    ),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("condition") {
                            writeRegister(virtualRegister("x"), integer(0))
                        }
                    "condition" does
                        conditional("body", "exitWhile") {
                            readRegister("x") lt integer(10)
                        }
                    "body" does
                        jump("condition") {
                            readRegister("x") addeq integer(1)
                        }
                    "exitWhile" does jump("exit") { writeRegister(getResultRegister(), unit) }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `CFG of while with noop body`() {
        // given
        val fDef =
            functionDeclaration(
                "f",
                block(
                    whileLoop(
                        // condition
                        lit(true),
                        // body
                        block(lit(1), lit(2)),
                    ),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does jump("bodyEntry") { CFGNode.NoOp }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }
}
