package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.cfgFragmentToGraphviz
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import org.junit.jupiter.api.Test

class CallTest {
    @Test
    fun `call sequence for a function without parameters is correctly generated when the call is used as a value`() {
        // given
        val calleeDef = functionDeclaration("callee", lit(1))
        val callerDef = functionDeclaration("caller", call("callee"))
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(program)
        val actualFragment = actualCFG[callerDef]!!

        // then
        val expectedFragment = cfg {
            fragment(callerDef) {
                "entry" does jump("store rsp") { writeRegister("temp rsp", registerUse(rsp)) }
                "store rsp" does jump("pad") { pushRegister("temp rsp") }
                "pad" does jump("adjust rsp") { pushRegister("temp rsp") }
                "adjust rsp" does jump("pass static link") {
                    writeRegister(
                        rsp,
                        registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16))
                    )
                }
                "pass static link" does jump("call") { writeRegister(rdi, registerUse(rbp)) }
                "call" does jump("restore rsp") { call(calleeDef) }
                "restore rsp" does jump("extract result") { popRegister(rsp) }
                "extract result" does jump("forward result") { writeRegister("result", registerUse(rax)) }
                "forward result" does jump("return") { writeRegister(rax, registerUse(virtualRegister("result"))) }
                "return" does final { returnNode }
            }
        }[callerDef]!!

        println(cfgFragmentToGraphviz(expectedFragment))

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }
}