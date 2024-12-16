package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import org.junit.jupiter.api.Test

class ForeignCallConventionTest {
    @Test
    fun `call sequence for a foreign function with one parameter correctly forwards the provided constant as argument`() {
        // given
        val calleeDef = foreignFunctionDeclaration("callee", listOf(basicType("Int")), basicType("Int"))
        val callerDef = intFunctionDefinition("caller", call("callee", lit(1)))
        /*
         * foreign callee = [Int] -> Int;
         * let caller = [] -> Int => callee[1];
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualFragment = generateSimplifiedCFG(program, fullCallSequences = true)[callerDef]!!

        // then
        val expectedFragment =
            standaloneWrappedCFGFragment(callerDef) {
                // The argument is prepared in a temporary register...
                "bodyEntry" does jump("prepare rsp") { writeRegister("arg", integer(1)) }
                "prepare rsp" does jump("pass arg") { registerUse(rsp) subeq integer(8) }
                // ...and then it is passed to its destination register (according to the call convention)
                "pass arg" does jump("call") { writeRegister(rdi, registerUse(virtualRegister("arg"))) }
                "call" does jump("restore rsp") { call(calleeDef) }
                "restore rsp" does jump("extract result") { registerUse(rsp) addeq integer(8) }
                "extract result" does jump("forward result") { writeRegister("result", registerUse(rax)) }
                "forward result" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse(virtualRegister("result"))) }
            }

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

    @Test
    fun `call sequence for a foreign function without parameters is correctly generated when the call is used for side effects`() {
        // given
        val calleeDef = foreignFunctionDeclaration("callee", emptyList(), basicType("Int"))
        val callerDef =
            intFunctionDefinition(
                "caller",
                block(
                    call("callee"),
                    lit(2),
                ),
            )
        /*
         * foreign callee = [] -> Int
         * let caller = [] -> Int => (callee[]; 2);
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualFragment = generateSimplifiedCFG(program, fullCallSequences = true)[callerDef]!!

        // then
        val expectedFragment =
            standaloneWrappedCFGFragment(callerDef) {
                "bodyEntry" does jump("call") { registerUse(rsp) subeq integer(8) }
                "call" does jump("restore rsp") { call(calleeDef) }
                "restore rsp" does jump("write block result to rax") { registerUse(rsp) addeq integer(8) }
                // The called function returned something, but we don't care - we only wanted it for side effects
                // We don't extract anything - instead, we prepare our own block result and move it to getResultRegister()
                "write block result to rax" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            integer(2),
                        )
                    }
            }

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }
}
