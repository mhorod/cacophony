package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
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
            intFunctionDefinition(
                "f",
                ifThenElse(lit(true), lit(11), lit(22)),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("bodyExit") { writeRegister(getResultRegister(), integer(11)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with false condition reduces to false branch`() {
        // given
        val fDef =
            intFunctionDefinition(
                "f",
                ifThenElse(lit(false), lit(11), lit(22)),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("bodyExit") { writeRegister(getResultRegister(), integer(22)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with logical and with false lhs skips computing rhs`() {
        // given
        val fDef =
            intFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("write result to rax") { writeRegister(virtualRegister("result"), integer(22)) }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            registerUse(virtualRegister("result")),
                        )
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with logical or with true lhs skips computing rhs`() {
        // given
        val fDef =
            intFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("write result to rax") { writeRegister(virtualRegister("result"), integer(11)) }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            registerUse(virtualRegister("result")),
                        )
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with logical or with false lhs computes rhs`() {
        // given
        val fDef =
            intFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("condition on x") { writeRegister(virtualRegister("x"), trueValue) }
                "condition on x" does
                    conditional("write 11 to result", "write 22 to result") {
                        registerUse(virtualRegister("x")) neq integer(0)
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
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            registerUse(virtualRegister("result")),
                        )
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with logical and with true lhs computes rhs`() {
        // given
        val fDef =
            intFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("condition on x") { writeRegister(virtualRegister("x"), trueValue) }
                "condition on x" does
                    conditional("write 11 to result", "write 22 to result") {
                        registerUse(virtualRegister("x")) neq integer(0)
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
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            registerUse(virtualRegister("result")),
                        )
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `if statement with nested ifs and ors`() {
        // given
        val fDef =
            intFunctionDefinition(
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
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("write result to rax") { writeRegister(virtualRegister("result"), integer(22)) }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            registerUse(virtualRegister("result")),
                        )
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `while loop with true condition never exits`() {
        // given
        val fDef =
            intFunctionDefinition(
                "f",
                block(
                    whileLoop(lit(true), variableDeclaration("x", lit(10))),
                    variableDeclaration("y", lit(20)),
                    variableUse("y"),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("bodyEntry") {
                        writeRegister(virtualRegister("x"), integer(10))
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `while loop with false condition never enters`() {
        // given
        val fDef =
            intFunctionDefinition(
                "f",
                block(
                    whileLoop(lit(false), variableDeclaration("x", lit(10))),
                    variableDeclaration("y", lit(20)),
                    variableUse("y"),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("write result to rax") {
                        writeRegister(virtualRegister("y"), integer(20))
                    }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(getResultRegister(), registerUse(virtualRegister("y")))
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `deep condition made of true, false, and, or simplifies to single branch`() {
        // given
        val fDef =
            intFunctionDefinition(
                "f",
                ifThenElse(
                    (
                        lit(false) lor
                            (
                                lit(false) lor
                                    (
                                        lit(false) lor
                                            (
                                                lit(false) lor
                                                    (
                                                        lit(false) lor
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
                    lit(24),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(actualCFG.keys.first()) {
                "bodyEntry" does jump("write result to rax") { writeRegister("result", integer(12)) }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            registerUse("result"),
                        )
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `condition with nested ifs simplifies to single branch`() {
        // given
        val fDef =
            intFunctionDefinition(
                "f",
                ifThenElse(
                    ifThenElse(
                        lit(true),
                        lit(false),
                        lit(true),
                    ),
                    lit(10),
                    lit(20),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("write result to rax") { writeRegister("result", integer(20)) }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            registerUse("result"),
                        )
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `condition if ifs nested in branches simplifies to single branch`() {
        // given
        val fDef =
            intFunctionDefinition(
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
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("write result to rax") { writeRegister("result", integer(11)) }
                "write result to rax" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            registerUse("result"),
                        )
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
