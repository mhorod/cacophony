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
        val expectedCFG =
            cfg {
                fragment(fDef) {
                    "entry" does jump("return") { writeRegister(rax, registerUse(virtualRegister("arg register"))) }
                    "return" does final { returnNode }
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
                fragment(innerDef) {
                    "entry" does
                        jump("return") {
                            writeRegister(
                                rax,
                                memoryAccess(memoryAccess(registerUse(rbp)) add integer(8)),
                            )
                        }
                    "return" does final { returnNode }
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
                fragment(innerDef) {
                    "entry" does
                        jump("return") {
                            writeRegister(
                                rax,
                                memoryAccess(memoryAccess(registerUse(rbp)) add integer(8)),
                            )
                        }
                    "return" does final { returnNode }
                }
            }[innerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }
}
