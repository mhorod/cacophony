package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import org.junit.jupiter.api.Test

class ReturnTest {
    @Test
    fun `return exits while(true) loop`() {
        // given
        val fDef =
            intFunctionDefinition(
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
                    lit(0),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("condition") {
                        writeRegister(virtualRegister("x"), integer(0))
                    }
                "condition" does
                    conditional("true branch", "return from loop") {
                        registerUse("x") lt integer(10)
                    }
                "true branch" does
                    jump("condition") {
                        registerUse("x") addeq integer(1)
                    }
                "return from loop" does
                    jump("bodyExit") {
                        writeRegister(getResultRegister(), integer(0))
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `return exits loop with condition`() {
        // given
        val fDef =
            intFunctionDefinition(
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
                    lit(0),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("loop condition") {
                        writeRegister(virtualRegister("x"), integer(0))
                    }
                "loop condition" does
                    conditional("increment x", "exitWhile") {
                        registerUse("x") lt integer(10)
                    }
                "increment x" does
                    jump("check x mod 5") {
                        registerUse("x") addeq integer(1)
                    }
                "check x mod 5" does
                    conditional("return from loop", "loop condition") {
                        (registerUse("x") mod integer(5)) eq integer(0)
                    }
                "exitWhile" does jump("bodyExit") { writeRegister(getResultRegister(), integer(0)) }
                "return from loop" does jump("bodyExit") { writeRegister(getResultRegister(), integer(0)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `block instructions after return are not computed`() {
        // given
        val fDef =
            intFunctionDefinition(
                "f",
                block(
                    returnStatement(lit(1)),
                    variableDeclaration("x", lit(2)),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("bodyExit") { writeRegister(getResultRegister(), integer(1)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `block instructions are not computed when both branches of if return`() {
        // given
        val fDef =
            intFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("condition") {
                        writeRegister(virtualRegister("x"), integer(2))
                    }
                "condition" does
                    conditional("write 1 to rax", "write 2 to rax") {
                        registerUse("x") eq integer(2)
                    }
                "write 1 to rax" does
                    jump("bodyExit") {
                        writeRegister(getResultRegister(), integer(1))
                    }
                "write 2 to rax" does
                    jump("bodyExit") {
                        writeRegister(getResultRegister(), integer(2))
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
