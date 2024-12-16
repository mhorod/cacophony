package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import org.junit.jupiter.api.Test

class AccessTest {
    @Test
    fun `function argument is correctly accessed`() {
        // given

        /*
         * let f = [x: Int] -> Int => x;
         */
        val fDef = unitFunctionDefinition("f", listOf(arg("x")), variableUse("x"))

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val virReg = Register.VirtualRegister()
        val expectedCFG =
            simplifiedSingleFragmentCFG(fDef) {
                "bodyEntry" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse(virReg)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `function argument is correctly accessed in nested function`() {
        // given

        /*
         * let outer = [x: Int] -> Int => (
         *     let inner = [] -> Int => x;
         *     inner[]
         * );
         */
        val innerDef = unitFunctionDefinition("inner", variableUse("x"))
        val outerDef = unitFunctionDefinition("outer", listOf(arg("x")), block(innerDef, call("inner")))

        // when
        val actualFragment = generateSimplifiedCFG(outerDef)[innerDef]!!

        // then
        val expectedFragment =
            standaloneSimplifiedCFGFragment(innerDef) {
                "bodyEntry" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            memoryAccess(memoryAccess(registerUse(rbp)) sub integer(8)),
                        )
                    }
            }

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

    @Test
    fun `function variable is correctly accessed in nested function`() {
        // given

        /*
         * let outer = [] -> Int => (
         *     let x = 1;
         *     let inner = [] -> Int => x;
         *     inner[]
         * );
         */
        val innerDef = intFunctionDefinition("inner", variableUse("x"))
        val outerDef =
            intFunctionDefinition(
                "outer",
                block(
                    variableDeclaration("x", lit(1)),
                    innerDef,
                    call("inner"),
                ),
            )

        // when
        val actualFragment = generateSimplifiedCFG(outerDef)[innerDef]!!

        // then
        val expectedFragment =
            standaloneSimplifiedCFGFragment(innerDef) {
                "bodyEntry" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            memoryAccess(memoryAccess(registerUse(rbp)) sub integer(8)),
                        )
                    }
            }

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }
}
