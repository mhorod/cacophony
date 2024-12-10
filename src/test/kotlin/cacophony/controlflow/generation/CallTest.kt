package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import org.junit.jupiter.api.Test

class CallTest {
    @Test
    fun `call sequence for a function without parameters is correctly generated when the call is used as a value`() {
        // given
        val calleeDef = intFunctionDefinition("callee", lit(1))
        val callerDef = intFunctionDefinition("caller", call("callee"))
        /*
         * let callee = [] -> Int => 1;
         * let caller = [] -> Int => callee[];
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = testPipeline().generateControlFlowGraph(program)
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
                    "forward result" does jump("exit") { writeRegister(getResultRegister(), registerUse(virtualRegister("result"))) }
                }
            }[callerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

    @Test
    fun `call sequence for a function without parameters is correctly generated when the call is used for side effects`() {
        // given
        val calleeDef = unitFunctionDefinition("callee", variableDeclaration("x", lit(1)))
        val callerDef =
            intFunctionDefinition(
                "caller",
                block(
                    call("callee"),
                    lit(2),
                ),
            )
        /*
         * let callee = [] -> Unit => let x = 1;
         * let caller = [] -> Int => (callee[]; 2);
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = testPipeline().generateControlFlowGraph(program)
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
                    // We don't extract anything - instead, we prepare our own block result and move it to getResultRegister()
                    "write block result to rax" does
                        jump("exit") {
                            writeRegister(
                                getResultRegister(),
                                integer(2),
                            )
                        }
                }
            }[callerDef]!!

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
        val actualCFG = testPipeline().generateControlFlowGraph(program)
        val actualFragment = actualCFG[callerDef]!!

        // then
        val expectedFragment =
            cfg {
                fragment(callerDef, listOf(argStack(0)), 8) {
                    "bodyEntry" does jump("store rsp") { writeRegister("temp rsp", registerUse(rsp)) }
                    "store rsp" does jump("pad") { pushRegister("temp rsp") }
                    "pad" does jump("adjust rsp") { pushRegister("temp rsp") }
                    "adjust rsp" does
                        jump("call") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    "call" does jump("restore rsp") { call(calleeDef) }
                    "restore rsp" does jump("write block result to rax") { popRegister(rsp) }
                    // The called function returned something, but we don't care - we only wanted it for side effects
                    // We don't extract anything - instead, we prepare our own block result and move it to getResultRegister()
                    "write block result to rax" does
                        jump("exit") {
                            writeRegister(
                                getResultRegister(),
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
        val calleeDef = intFunctionDefinition("callee", listOf(intArg("x")), variableUse("x"))
        val callerDef = intFunctionDefinition("caller", call("callee", lit(1)))
        /*
         * let callee = [x: Int] -> Int => x;
         * let caller = [] -> Int => callee[1];
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = testPipeline().generateControlFlowGraph(program)
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
                        jump("pass arg") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    // ...and then it is passed to its destination register (according to the call convention)
                    "pass arg" does jump("pass static link") { writeRegister(rdi, registerUse(virtualRegister("arg"))) }
                    "pass static link" does jump("call") { writeRegister(rsi, registerUse(rbp)) }
                    "call" does jump("restore rsp") { call(calleeDef) }
                    "restore rsp" does jump("extract result") { popRegister(rsp) }
                    "extract result" does jump("forward result") { writeRegister("result", registerUse(rax)) }
                    "forward result" does jump("exit") { writeRegister(getResultRegister(), registerUse(virtualRegister("result"))) }
                }
            }[callerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

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
        val actualCFG = testPipeline().generateControlFlowGraph(program)
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
                        jump("pass arg") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    // ...and then it is passed to its destination register (according to the call convention)
                    "pass arg" does jump("call") { writeRegister(rdi, registerUse(virtualRegister("arg"))) }
                    "call" does jump("restore rsp") { call(calleeDef) }
                    "restore rsp" does jump("extract result") { popRegister(rsp) }
                    "extract result" does jump("forward result") { writeRegister("result", registerUse(rax)) }
                    "forward result" does jump("exit") { writeRegister(getResultRegister(), registerUse(virtualRegister("result"))) }
                }
            }[callerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

    @Test
    fun `call sequence for a function with seven parameters correctly forwards all provided constants as arguments`() {
        // given
        val calleeDef =
            intFunctionDefinition(
                "callee",
                listOf(intArg("x1"), intArg("x2"), intArg("x3"), intArg("x4"), intArg("x5"), intArg("x6"), intArg("x7")),
                variableUse("x1"),
            )
        val callerDef = intFunctionDefinition("caller", call("callee", lit(1), lit(2), lit(3), lit(4), lit(5), lit(6), lit(7)))
        /*
         * let callee = [x1: Int, x2: Int, x3: Int, x4: Int, x5: Int, x6: Int, x7: Int] -> Int => x1;
         * let caller = [] -> Int => callee[1,2,3,4,5,6,7];
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = testPipeline().generateControlFlowGraph(program)
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
                    "prepare arg6" does jump("prepare arg7") { writeRegister("arg6", integer(6)) }
                    "prepare arg7" does jump("prepare rsp") { writeRegister("arg7", integer(7)) }
                    "prepare rsp" does jump("store rsp") { writeRegister("temp rsp", registerUse(rsp)) }
                    "store rsp" does jump("pad") { pushRegister("temp rsp") }
                    "pad" does jump("adjust rsp") { pushRegister("temp rsp") }
                    "adjust rsp" does
                        jump("pass arg1") {
                            writeRegister(
                                rsp,
                                // rsp needs to be adjusted by 0 because two arguments will be passed via the stack
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    // ...and then they are passed to their destination registers...
                    "pass arg1" does jump("pass arg2") { writeRegister(rdi, registerUse(virtualRegister("arg1"))) }
                    "pass arg2" does jump("pass arg3") { writeRegister(rsi, registerUse(virtualRegister("arg2"))) }
                    "pass arg3" does jump("pass arg4") { writeRegister(rdx, registerUse(virtualRegister("arg3"))) }
                    "pass arg4" does jump("pass arg5") { writeRegister(rcx, registerUse(virtualRegister("arg4"))) }
                    "pass arg5" does jump("pass arg6") { writeRegister(r8, registerUse(virtualRegister("arg5"))) }
                    "pass arg6" does jump("prepare arg7 for push") { writeRegister(r9, registerUse(virtualRegister("arg6"))) }
                    // ...or via the stack
                    "prepare arg7 for push" does
                        jump("prepare static link for push") { writeRegister("temp arg7", registerUse(virtualRegister("arg7"))) }
                    "prepare static link for push" does jump("push static link") { writeRegister("temp static link", registerUse(rbp)) }
                    "push static link" does jump("push arg7") { pushRegister("temp static link") }
                    "push arg7" does jump("call") { pushRegister("temp arg7") }

                    "call" does jump("clear stack args") { call(calleeDef) }
                    // The argument still on the stack must then be ignored
                    "clear stack args" does jump("restore rsp") { writeRegister(rsp, registerUse(rsp) add integer(16)) }
                    "restore rsp" does jump("extract result") { popRegister(rsp) }
                    "extract result" does jump("forward result") { writeRegister("result", registerUse(rax)) }
                    "forward result" does jump("exit") { writeRegister(getResultRegister(), registerUse(virtualRegister("result"))) }
                }
            }[callerDef]!!

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

    @Test
    fun `call sequence is correctly generated for one call being argument to another`() {
        // given
        val calleeDef = intFunctionDefinition("callee", listOf(intArg("x")), variableUse("x"))
        val callerDef = intFunctionDefinition("caller", call("callee", call("callee", lit(1))))
        /*
         * let callee = [x: Int] -> Int => x;
         * let caller = [] -> Int => callee[callee[1]];
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualCFG = testPipeline().generateControlFlowGraph(program)
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
                        jump("pass arg in") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    // ...and then it is passed to its destination register (according to the call convention)...
                    "pass arg in" does jump("pass static link in") { writeRegister(rdi, registerUse(virtualRegister("arg in"))) }
                    "pass static link in" does jump("call in") { writeRegister(rsi, registerUse(rbp)) }
                    "call in" does jump("restore rsp in") { call(calleeDef) }
                    "restore rsp in" does jump("extract result in") { popRegister(rsp) }
                    "extract result in" does jump("prepare rsp out") { writeRegister("result in", registerUse(rax)) }
                    "prepare rsp out" does jump("store rsp out") { writeRegister("temp rsp out", registerUse(rsp)) }
                    "store rsp out" does jump("pad out") { pushRegister("temp rsp out") }
                    "pad out" does jump("adjust rsp out") { pushRegister("temp rsp out") }
                    "adjust rsp out" does
                        jump("pass arg out") {
                            writeRegister(
                                rsp,
                                registerUse(rsp) add ((registerUse(rsp) add integer(0)) mod integer(16)),
                            )
                        }
                    // ...and then the internal result is passed to its destination register (according to the call convention)
                    "pass arg out" does jump("pass static link out") { writeRegister(rdi, registerUse(virtualRegister("result in"))) }
                    "pass static link out" does jump("call out") { writeRegister(rsi, registerUse(rbp)) }
                    "call out" does jump("restore rsp out") { call(calleeDef) }
                    "restore rsp out" does jump("extract result out") { popRegister(rsp) }
                    "extract result out" does jump("forward result out") { writeRegister("result out", registerUse(rax)) }
                    "forward result out" does
                        jump("exit") { writeRegister(getResultRegister(), registerUse(virtualRegister("result out"))) }
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
        val fDef = intFunctionDefinition("f", lit(1) add call("f"))

        // when
        val actualCFG = testPipeline().generateControlFlowGraph(fDef)

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
                            writeRegister(getResultRegister(), integer(1) add registerUse(virtualRegister("result")))
                        }
                }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
