package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import org.junit.jupiter.api.Test

class AccessTest {
    @Test
    fun `function argument is correctly accessed`() {
        // given

        /*
         * let f = [x: Int] -> Int => x;
         */
        val fDef = functionDeclaration("f", listOf(arg("x")), variableUse("x"))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val virReg = Register.VirtualRegister()
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argReg(virReg), argStack(0)), 8) {
                    "bodyEntry" does jump("exit") { writeRegister(rax, registerUse(virReg)) }
                }
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
        val innerDef = functionDeclaration("inner", variableUse("x"))
        val outerDef = functionDeclaration("outer", listOf(arg("x")), block(innerDef, call("inner")))

        // when
        val actualFragment = pipeline.generateControlFlowGraph(outerDef)[innerDef]!!

        // then
        val expectedFragment =
            cfg {
                fragment(innerDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("exit") {
                            writeRegister(
                                rax,
                                memoryAccess(memoryAccess(registerUse(rbp)) add integer(8)),
                            )
                        }
                }
            }[innerDef]!!

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
        val innerDef = functionDeclaration("inner", variableUse("x"))
        val outerDef =
            functionDeclaration(
                "outer",
                block(
                    variableDeclaration("x", lit(1)),
                    innerDef,
                    call("inner"),
                ),
            )

        // when
        val actualFragment = pipeline.generateControlFlowGraph(outerDef)[innerDef]!!

        // then
        val expectedFragment =
            cfg {
                fragment(innerDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does
                        jump("exit") {
                            writeRegister(
                                rax,
                                memoryAccess(memoryAccess(registerUse(rbp)) add integer(8)),
                            )
                        }
                }
            }[innerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }
}
