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
        val actualFragment = generateSimplifiedCFG(program, fullCallSequences = true)[callerDef.value]!!

        val expectedFragment =
            standaloneWrappedCFGFragment(callerDef) {
                "bodyEntry" does
                    jump("v@70730412") {
                        registerUse(virtualRegister("reg6")) assign CFGNode.DataLabel("calleeLabel")
                    }
                "v@70730412" does
                    jump("v@59778353") {
                        registerUse(virtualRegister("reg7")) assign CFGNode.ConstantKnown(0)
                    }
                "v@59778353" does
                    jump("vl@be0947a") {
                        registerUse(virtualRegister("reg8")) assign CFGNode.ConstantKnown(1)
                    }
                "vl@be0947a" does
                    jump("v@35fcbf9f") {
                        (registerUse(rsp) subeq CFGNode.ConstantKnown(0))
                    }
                "v@35fcbf9f" does
                    jump("v@22da40fd") {
                        registerUse(rdi) assign registerUse(virtualRegister("reg8"))
                    }
                "v@22da40fd" does
                    jump("v@593e90e6") {
                        registerUse(rsi) assign registerUse(virtualRegister("reg7"))
                    }
                "v@593e90e6" does
                    jump("v@56729cff") {
                        call(registerUse(virtualRegister("reg6")), 2)
                    }
                "v@56729cff" does
                    jump("vl@622d1e9") {
                        (registerUse(rsp) addeq CFGNode.ConstantKnown(0))
                    }
                "vl@622d1e9" does
                    jump("v@19cc5907") {
                        registerUse(virtualRegister("reg9")) assign registerUse(rax)
                    }
                "v@19cc5907" does
                    jump("bodyExit") {
                        registerUse(virtualRegister("reg0")) assign registerUse(virtualRegister("reg9"))
                    }
            }

        // then
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
        val actualFragment = generateSimplifiedCFG(program, fullCallSequences = true)[callerDef.value]!!

        val expectedFragment =
            standaloneWrappedCFGFragment(callerDef.value) {
                "bodyEntry" does
                    jump("v@5b9bb3ef") {
                        registerUse(virtualRegister("reg6")) assign CFGNode.DataLabel("calleeLabel")
                    }
                "v@5b9bb3ef" does
                    jump("v@2834cd2e") {
                        registerUse(virtualRegister("reg7")) assign CFGNode.ConstantKnown(0)
                    }
                "v@2834cd2e" does
                    jump("v@5fff231e") {
                        (registerUse(rsp) subeq CFGNode.ConstantKnown(0))
                    }
                "v@5fff231e" does
                    jump("v@16cc6900") {
                        registerUse(rdi) assign registerUse(virtualRegister("reg7"))
                    }
                "v@16cc6900" does
                    jump("v@1ab17fbe") {
                        call(registerUse(virtualRegister("reg6")), 1)
                    }
                "v@1ab17fbe" does
                    jump("v@47d25117") {
                        (registerUse(rsp) addeq CFGNode.ConstantKnown(0))
                    }
                "v@47d25117" does
                    jump("bodyExit") {
                        registerUse(virtualRegister("reg0")) assign CFGNode.ConstantKnown(2)
                    }
            }

        // then
        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }
}
