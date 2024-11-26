package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class CallTest {
    @Test
    fun `call sequence for a function without parameters is correctly generated when the call is used as a value`() {
        // given
        val calleeDef = functionDeclaration("callee", lit(1))
        val callerDef = functionDeclaration("caller", call("callee"))
        /*
         * let callee = [] -> Int => 1;
         * let caller = [] -> Int => callee[];
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(program)
        val actualFragment = actualCFG[callerDef]!!

        // then
        val expectedFragment =
            cfg {
                fragment(callerDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does jump("store rsp") { writeRegister("temp rsp", registerUse(rsp)) }
                    "store rsp" does jump("pad") { pushRegister("temp rsp") }
                    "pad" does jump("adjust rsp") { pushRegister("temp rsp") }
                    "adjust rsp" does
                        jump("pass static link") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    "pass static link" does jump("call") { writeRegister(rdi, registerUse(rbp)) }
                    "call" does jump("restore rsp") { call(calleeDef) }
                    "restore rsp" does jump("extract result") { popRegister(rsp) }
                    // The called function returned something, and we are using it as a value - we need to extract it from rax
                    "extract result" does jump("forward result") { writeRegister("result", registerUse(rax)) }
                    "forward result" does jump("exit") { writeRegister(rax, registerUse(virtualRegister("result"))) }
                }
            }[callerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

    @Test
    fun `call sequence for a function without parameters is correctly generated when the call is used for side effects`() {
        // given
        val calleeDef = functionDeclaration("callee", variableDeclaration("x", lit(1)))
        val callerDef =
            functionDeclaration(
                "caller",
                block(
                    call("callee"),
                    lit(2),
                ),
            )
        /*
         * let callee = [] -> Int => let x = 1;
         * let caller = [] -> Int => (callee[]; 2);
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(program)
        val actualFragment = actualCFG[callerDef]!!

        // then
        val expectedFragment =
            cfg {
                fragment(callerDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does jump("store rsp") { writeRegister("temp rsp", registerUse(rsp)) }
                    "store rsp" does jump("pad") { pushRegister("temp rsp") }
                    "pad" does jump("adjust rsp") { pushRegister("temp rsp") }
                    "adjust rsp" does
                        jump("pass static link") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    "pass static link" does jump("call") { writeRegister(rdi, registerUse(rbp)) }
                    "call" does jump("restore rsp") { call(calleeDef) }
                    "restore rsp" does jump("write block result to rax") { popRegister(rsp) }
                    // The called function returned something, but we don't care - we only wanted it for side effects
                    // We don't extract anything - instead, we prepare our own block result and move it to rax
                    "write block result to rax" does
                        jump("exit") {
                            writeRegister(
                                rax,
                                integer(2),
                            )
                        }
                }
            }[callerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

    @Test
    fun `call sequence for a function with one parameter correctly forwards the provided constant as argument`() {
        // given
        val calleeDef = functionDeclaration("callee", listOf(arg("x")), variableUse("x"))
        val callerDef = functionDeclaration("caller", call("callee", lit(1)))
        /*
         * let callee = [x: Int] -> Int => x;
         * let caller = [] -> Int => callee[1];
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(program)
        val actualFragment = actualCFG[callerDef]!!

        // then
        val expectedFragment =
            cfg {
                fragment(callerDef, listOf(argStack(0)), 8) {
                    // The argument is prepared in a temporary register...
                    "bodyEntry" does jump("prepare rsp") { writeRegister("arg", integer(1)) }
                    "prepare rsp" does jump("store rsp") { writeRegister("temp rsp", registerUse(rsp)) }
                    "store rsp" does jump("pad") { pushRegister("temp rsp") }
                    "pad" does jump("adjust rsp") { pushRegister("temp rsp") }
                    "adjust rsp" does
                        jump("pass static link") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    "pass static link" does jump("pass arg") { writeRegister(rdi, registerUse(rbp)) }
                    // ...and then it is passed to its destination register (according to the call convention)
                    "pass arg" does jump("call") { writeRegister(rsi, registerUse(virtualRegister("arg"))) }
                    "call" does jump("restore rsp") { call(calleeDef) }
                    "restore rsp" does jump("extract result") { popRegister(rsp) }
                    "extract result" does jump("forward result") { writeRegister("result", registerUse(rax)) }
                    "forward result" does jump("exit") { writeRegister(rax, registerUse(virtualRegister("result"))) }
                }
            }[callerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

    @Test
    @Disabled("TODO: should work after changes in analyzed function or prologue generation")
    fun `call sequence for a function with six parameters correctly forwards all provided constants as arguments`() {
        // given
        val calleeDef =
            functionDeclaration(
                "callee",
                listOf(arg("x1"), arg("x2"), arg("x3"), arg("x4"), arg("x5"), arg("x6")),
                variableUse("x1"),
            )
        val callerDef = functionDeclaration("caller", call("callee", lit(1), lit(2), lit(3), lit(4), lit(5), lit(6)))
        /*
         * let callee = [x1: Int, x2: Int, x3: Int, x4: Int, x5: Int, x6: Int] -> Int => x1;
         * let caller = [] -> Int => callee[];
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(program)
        val actualFragment = actualCFG[callerDef]!!

        // then
        val expectedFragment =
            cfg {
                fragment(callerDef, listOf(argStack(0)), 8) {
                    // The arguments are prepared in a temporary registers...
                    "bodyEntry" does jump("prepare arg2") { writeRegister("arg1", integer(1)) }
                    "prepare arg2" does jump("prepare arg3") { writeRegister("arg2", integer(2)) }
                    "prepare arg3" does jump("prepare arg4") { writeRegister("arg3", integer(3)) }
                    "prepare arg4" does jump("prepare arg5") { writeRegister("arg4", integer(4)) }
                    "prepare arg5" does jump("prepare arg6") { writeRegister("arg5", integer(5)) }
                    "prepare arg6" does jump("prepare rsp") { writeRegister("arg6", integer(6)) }
                    "prepare rsp" does jump("store rsp") { writeRegister("temp rsp", registerUse(rsp)) }
                    "store rsp" does jump("pad") { pushRegister("temp rsp") }
                    "pad" does jump("adjust rsp") { pushRegister("temp rsp") }
                    "adjust rsp" does
                        jump("pass static link") {
                            writeRegister(
                                rsp,
                                // rsp needs to be adjusted by 8 because one argument will be passed via the stack
                                registerUse(rsp) add ((registerUse(rsp) add integer(8)) mod integer(16)),
                            )
                        }
                    "pass static link" does jump("pass arg1") { writeRegister(rdi, registerUse(rbp)) }
                    // ...and then they are passed to their destination registers...
                    "pass arg1" does jump("pass arg2") { writeRegister(rsi, registerUse(virtualRegister("arg1"))) }
                    "pass arg2" does jump("pass arg3") { writeRegister(rdx, registerUse(virtualRegister("arg2"))) }
                    "pass arg3" does jump("pass arg4") { writeRegister(rcx, registerUse(virtualRegister("arg3"))) }
                    "pass arg4" does jump("pass arg5") { writeRegister(r8, registerUse(virtualRegister("arg4"))) }
                    "pass arg5" does jump("prepare arg6 for push") { writeRegister(r9, registerUse(virtualRegister("arg5"))) }
                    // ...or via the stack
                    "prepare arg6 for push" does jump("push arg6") { writeRegister("temp arg6", registerUse(virtualRegister("arg6"))) }
                    "push arg6" does jump("call") { pushRegister("temp arg6") }
                    "call" does jump("clear arg6") { call(calleeDef) }
                    // The argument still on the stack must then be ignored
                    "clear arg6" does jump("restore rsp") { writeRegister(rsp, registerUse(rsp) add integer(8)) }
                    "restore rsp" does jump("extract result") { popRegister(rsp) }
                    "extract result" does jump("forward result") { writeRegister("result", registerUse(rax)) }
                    "forward result" does jump("exit") { writeRegister(rax, registerUse(virtualRegister("result"))) }
                }
            }[callerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

    @Test
    fun `call sequence is correctly generated for one call being argument to another`() {
        // given
        val calleeDef = functionDeclaration("callee", listOf(arg("x")), variableUse("x"))
        val callerDef = functionDeclaration("caller", call("callee", call("callee", lit(1))))
        /*
         * let callee = [x: Int] -> Int => x;
         * let caller = [] -> Int => callee[callee[1]];
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = pipeline.generateControlFlowGraph(program)
        val actualFragment = actualCFG[callerDef]!!

        // then
        val expectedFragment =
            cfg {
                fragment(callerDef, listOf(argStack(0)), 8) {
                    // The argument is prepared in a temporary register...
                    "bodyEntry" does jump("prepare rsp in") { writeRegister("arg in", integer(1)) }
                    "prepare rsp in" does jump("store rsp in") { writeRegister("temp rsp in", registerUse(rsp)) }
                    "store rsp in" does jump("pad in") { pushRegister("temp rsp in") }
                    "pad in" does jump("adjust rsp in") { pushRegister("temp rsp in") }
                    "adjust rsp in" does
                        jump("pass static link in") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    "pass static link in" does jump("pass arg in") { writeRegister(rdi, registerUse(rbp)) }
                    // ...and then it is passed to its destination register (according to the call convention)...
                    "pass arg in" does jump("call in") { writeRegister(rsi, registerUse(virtualRegister("arg in"))) }
                    "call in" does jump("restore rsp in") { call(calleeDef) }
                    "restore rsp in" does jump("extract result in") { popRegister(rsp) }
                    "extract result in" does jump("prepare rsp out") { writeRegister("result in", registerUse(rax)) }
                    "prepare rsp out" does jump("store rsp out") { writeRegister("temp rsp out", registerUse(rsp)) }
                    "store rsp out" does jump("pad out") { pushRegister("temp rsp out") }
                    "pad out" does jump("adjust rsp out") { pushRegister("temp rsp out") }
                    "adjust rsp out" does
                        jump("pass static link out") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    "pass static link out" does jump("pass arg out") { writeRegister(rdi, registerUse(rbp)) }
                    // ...and then the internal result is passed to its destination register (according to the call convention)
                    "pass arg out" does jump("call out") { writeRegister(rsi, registerUse(virtualRegister("result in"))) }
                    "call out" does jump("restore rsp out") { call(calleeDef) }
                    "restore rsp out" does jump("extract result out") { popRegister(rsp) }
                    "extract result out" does jump("forward result out") { writeRegister("result out", registerUse(rax)) }
                    "forward result out" does jump("exit") { writeRegister(rax, registerUse(virtualRegister("result out"))) }
                }
            }[callerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

    @Test
    fun `call sequence is correctly generated when function calls itself`() {
        // given

        /*
         * let f = [] -> Int => 1 + f[];
         */
        val fDef = functionDeclaration("f", lit(1) add call("f"))

        // when
        val actualCFG = pipeline.generateControlFlowGraph(fDef)

        // then
        val expectedCFG =
            cfg {
                fragment(fDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does jump("store rsp") { writeRegister("temp rsp", registerUse(rsp)) }
                    "store rsp" does jump("pad") { pushRegister("temp rsp") }
                    "pad" does jump("adjust rsp") { pushRegister("temp rsp") }
                    "adjust rsp" does
                        jump("pass static link") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    "pass static link" does jump("call") { writeRegister(rdi, registerUse(rbp)) }
                    "call" does jump("restore rsp") { call(fDef) }
                    "restore rsp" does jump("extract result") { popRegister(rsp) }
                    // The called function returned something, and we are using it as a value - we need to extract it from rax
                    "extract result" does jump("forward result") { writeRegister("result", registerUse(rax)) }
                    "forward result" does
                        jump("exit") {
                            writeRegister(rax, integer(1) add registerUse(virtualRegister("result")))
                        }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
