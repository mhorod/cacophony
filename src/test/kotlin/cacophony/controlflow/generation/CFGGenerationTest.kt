package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.controlflow.CFGNode.Companion.TRUE
import cacophony.controlflow.CFGNode.Companion.UNIT
import org.junit.jupiter.api.Test

fun argStack(offset: Int) = VariableAllocation.OnStack(offset)

fun argReg(reg: Register.VirtualRegister) = VariableAllocation.InRegister(reg)

class CFGGenerationTest {
    @Test
    fun `CFG of empty function`() {
        // given
        val emptyBlock = block()
        val fDef = unitFunctionDefinition("f", emptyBlock)

        // when
        val cfg = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            simplifiedSingleFragmentCFG(fDef) {
                "bodyEntry" does jump("bodyExit") { writeRegister(getResultRegister(), UNIT) }
            }

        assertEquivalent(cfg, expectedCFG)
    }

    @Test
    fun `CFG of function returning true`() {
        // given
        val fDef = boolFunctionDefinition("f", lit(true))

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            simplifiedSingleFragmentCFG(fDef) {
                "bodyEntry" does jump("bodyExit") { writeRegister(getResultRegister(), TRUE) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `CFG of if with variable condition`() {
        // given
        val fDef =
            intFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            simplifiedSingleFragmentCFG(fDef) {
                "bodyEntry" does jump("condition") { writeRegister(virtualRegister("x"), integer(1)) }
                "condition" does
                    conditional("true", "false") {
                        registerUse(virtualRegister("x")) neq integer(0)
                    }
                "true" does jump("end") { writeRegister(virtualRegister("t"), integer(11)) }
                "false" does jump("end") { writeRegister(virtualRegister("t"), integer(22)) }
                "end" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse(virtualRegister("t"))) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `CFG of while loop with variable condition`() {
        // given
        val fDef =
            unitFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            simplifiedSingleFragmentCFG(fDef) {
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
                "exitWhile" does jump("bodyExit") { writeRegister(getResultRegister(), unit) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `CFG of while with noop body`() {
        // given
        val fDef =
            unitFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            simplifiedSingleFragmentCFG(fDef) {
                "bodyEntry" does jump("bodyEntry") { CFGNode.NoOp }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
