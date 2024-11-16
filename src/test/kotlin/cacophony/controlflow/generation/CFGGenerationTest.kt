package cacophony.controlflow.generation

import cacophony.block
import cacophony.cfg
import cacophony.controlflow.CFGFragment
import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.CFGNode.Companion.TRUE
import cacophony.controlflow.CFGVertex
import cacophony.controlflow.Register
import cacophony.controlflow.X64Register
import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.functionDeclaration
import cacophony.ifThenElse
import cacophony.integer
import cacophony.lit
import cacophony.pipeline.CacophonyPipeline
import cacophony.rax
import cacophony.registerUse
import cacophony.returnNode
import cacophony.returnStatement
import cacophony.variableDeclaration
import cacophony.variableUse
import cacophony.writeRegister
import io.mockk.mockk
import org.junit.jupiter.api.Test

class CFGGenerationTest {
    companion object {
        val diagnostics = CacophonyDiagnostics(mockk())
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
        val entryLabel = CFGLabel()
        val writeUnitToRax =
            CFGNode.Assignment(CFGNode.RegisterUse(Register.FixedRegister(X64Register.RAX)), CFGNode.UNIT)
        val returnLabel = CFGLabel()

        val expectedCFG =
            CFGFragment(
                mapOf(
                    entryLabel to CFGVertex.Jump(writeUnitToRax, returnLabel),
                    returnLabel to CFGVertex.Final(CFGNode.Return),
                ),
                entryLabel,
            )
        assertEquivalent(cfg, mapOf(fDef to expectedCFG))
    }

    @Test
    fun `CFG of function returning true`() {
        // given
        val fDef = functionDeclaration("f", returnStatement(lit(true)))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("return") { writeRegister(rax, TRUE) }
                    "return" does final { returnNode }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `CFG of if with literal condition reduces to single branch`() {
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
                        returnStatement(lit(11)),
                        // else
                        returnStatement(lit(22)),
                    ),
                ),
            )

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("condition") { writeRegister(virtualRegister("x"), integer(1)) }
                    "condition" does
                        conditional("true", "false") {
                            registerUse(virtualRegister("x"))
                        }
                    "true" does jump("returnTrue") { writeRegister(rax, integer(11)) }
                    "false" does jump("returnFalse") { writeRegister(rax, integer(22)) }
                    "returnTrue" does final { returnNode }
                    "returnFalse" does final { returnNode }
                }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }
}
