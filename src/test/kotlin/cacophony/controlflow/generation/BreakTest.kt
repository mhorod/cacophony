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
            unitFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("condition") {
                        writeRegister(virtualRegister("x"), integer(0))
                    }
                "condition" does
                    conditional("true branch", "exitWhile") {
                        registerUse("x") lt integer(10)
                    }
                "true branch" does
                    jump("condition") {
                        registerUse("x") addeq integer(1)
                    }
                "exitWhile" does jump("bodyExit") { writeRegister(getResultRegister(), unit) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `break exits loop with condition`() {
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
                    conditional("exitWhile", "loop condition") {
                        (registerUse("x") mod integer(5)) eq integer(0)
                    }
                "exitWhile" does jump("bodyExit") { writeRegister(getResultRegister(), unit) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `block instructions after break are not computed`() {
        // given
        val fDef =
            unitFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("write unit to rax") { writeRegister("x", integer(2)) }
                "write unit to rax" does jump("bodyExit") { writeRegister(getResultRegister(), unit) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `block instructions are not computed when both branches of if break`() {
        // given
        val fDef =
            unitFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("condition") {
                        writeRegister(virtualRegister("x"), integer(2))
                    }
                "condition" does
                    conditional("exitWhile", "exitWhile") {
                        registerUse("x") eq integer(2)
                    }
                "exitWhile" does jump("bodyExit") { writeRegister(getResultRegister(), unit) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `assignment of break`() {
        // given
        val fDef =
            unitFunctionDefinition(
                "f",
                whileLoop(
                    lit(true),
                    block(
                        variableDeclaration("x", breakStatement()),
                    ),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("bodyExit") { writeRegister(virtualRegister("x"), unit) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
