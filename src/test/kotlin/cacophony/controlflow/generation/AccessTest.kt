package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.controlflow.print.cfgFragmentToBuilder
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
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse("")) }
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
        val actualFragment = generateSimplifiedCFG(outerDef)[innerDef.value]!!
        println(cfgFragmentToBuilder(innerDef.value, actualFragment))

        // then
        val expectedFragment =
            standaloneWrappedCFGFragment(innerDef.value) {
                "bodyEntry" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            memoryAccess(memoryAccess(registerUse(rbp) sub integer(8)) sub integer(16)),
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
        val actualFragment = generateSimplifiedCFG(outerDef)[innerDef.value]!!

        // then
        val expectedFragment =
            standaloneWrappedCFGFragment(innerDef.value) {
                "bodyEntry" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            memoryAccess(memoryAccess(registerUse(rbp) sub integer(8)) sub integer(16)),
                        )
                    }
            }

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }
}
