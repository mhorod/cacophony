package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.semantic.syntaxtree.Definition
import org.junit.jupiter.api.Test

class FunctionPrologueAndEpilogueTest {
    @Test
    fun `function with no arguments has trivial prologue and trivial epilogue`() {
        // given

        /*
         * let f = [] -> Unit => ();
         */
        val fDef = unitFunctionDefinition("f", block())

        // when
        val actualCFG = generateCFGWithSimplifiedCalls(fDef)

        // then
        val expectedCFG =
            singleFragmentCFG(fDef) {
                setupStackFrame("entry", "save preserved")
                savePreservedRegisters("save preserved", "store static link")
                "store static link" does
                    jump("store result") {
                        memoryAccess(registerUse(rbp) sub integer(0)) assign registerUse(rdi)
                    }
                "store result" does jump("restore preserved") { writeRegister("result", CFGNode.UNIT) }
                restorePreservedRegisters("restore preserved", "move result to rax")
                "move result to rax" does jump("teardown") { writeRegister(rax, "result") }
                teardownStackFrame("teardown", "exit")
                "exit" does final { returnNode(1) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `function with one argument moves it to a virtual register in the prologue`() {
        // given

        /*
         * let f = [x: Int] -> Int => 17;
         */
        val fDef = intFunctionDefinition("f", listOf(intArg("x")), lit(17))

        // when
        val actualCFG = generateCFGWithSimplifiedCalls(fDef)

        // then
        val expectedCFG =
            singleFragmentCFG(fDef) {
                setupStackFrame("entry", "save preserved")
                savePreservedRegisters("save preserved", "store argument")
                "store argument" does jump("store static link") { writeRegister("0th arg", registerUse(rdi)) }
                "store static link" does
                    jump("store result") {
                        memoryAccess(registerUse(rbp) sub integer(0)) assign registerUse(rsi)
                    }
                "store result" does jump("restore preserved") { writeRegister("result", integer(17)) }
                restorePreservedRegisters("restore preserved", "move result to rax")
                "move result to rax" does jump("teardown") { writeRegister(rax, "result") }
                teardownStackFrame("teardown", "exit")
                "exit" does final { returnNode(1) }
            }
        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `function with seven arguments moves them to virtual registers or onto the stack in the prologue`() {
        // given

        /*
         * let f = [x0: Int, x1: Int, x2: Int, x3: Int, x4: Int, x5: Int, x6: Int] -> Int => 83;
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(
                    intArg("x0"),
                    intArg("x1"),
                    intArg("x2"),
                    intArg("x3"),
                    intArg("x4"),
                    intArg("x5"),
                    intArg("x6"),
                ),
                lit(83),
            )

        // when
        val actualCFG = generateCFGWithSimplifiedCalls(fDef)

        // then
        val expectedCFG =
            singleFragmentCFG(fDef) {
                setupStackFrame("entry", "save preserved")
                savePreservedRegisters("save preserved", "store 0th argument")
                "store 0th argument" does jump("store 1st argument") { writeRegister("0th arg", registerUse(rdi)) }
                "store 1st argument" does jump("store 2nd argument") { writeRegister("1st arg", registerUse(rsi)) }
                "store 2nd argument" does jump("store 3rd argument") { writeRegister("2nd arg", registerUse(rdx)) }
                "store 3rd argument" does jump("store 4th argument") { writeRegister("3rd arg", registerUse(rcx)) }
                "store 4th argument" does jump("store 5th argument") { writeRegister("4th arg", registerUse(r8)) }
                "store 5th argument" does jump("store 6th argument") { writeRegister("5th arg", registerUse(r9)) }
                "store 6th argument" does
                    jump("store static link") {
                        writeRegister("6th arg", memoryAccess(registerUse(rbp) sub integer(-40)))
                    }
                "store static link" does
                    jump("store result") {
                        memoryAccess(registerUse(rbp) sub integer(0)) assign memoryAccess(registerUse(rbp) sub integer(-48))
                    }
                "store result" does jump("restore preserved") { writeRegister("result", integer(83)) }
                restorePreservedRegisters("restore preserved", "move result to rax")
                "move result to rax" does jump("teardown") { writeRegister(rax, "result") }
                teardownStackFrame("teardown", "exit")
                "exit" does final { returnNode(1) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `function with one argument moves it onto the stack if it should be accessed via static link`() {
        // given

        /*
         * let f = [x: Int] -> Int => (
         *     let g = [] -> Int => x += 3;
         *     g[];
         *     x
         * );
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(intArg("x")),
                block(
                    intFunctionDefinition(
                        "g",
                        variableUse("x") addeq lit(3),
                    ),
                    variableUse("x"),
                ),
            )

        // when
        val actualFragment = generateCFGWithSimplifiedCalls(fDef)[fDef]!!

        // then
        val expectedFragment =
            standaloneCFGFragment(fDef) {
                setupStackFrame("entry", "save preserved", allocatedSpace = 8)
                savePreservedRegisters("save preserved", "store argument")
                "store argument" does
                    jump("store static link") {
                        memoryAccess(registerUse(rbp) sub integer(8)) assign registerUse(rdi)
                    }
                "store static link" does
                    jump("store result") {
                        memoryAccess(registerUse(rbp) sub integer(0)) assign registerUse(rsi)
                    }
                "store result" does
                    jump("restore preserved") {
                        writeRegister("result", memoryAccess(registerUse(rbp) sub integer(8)))
                    }
                restorePreservedRegisters("restore preserved", "move result to rax")
                "move result to rax" does jump("teardown") { writeRegister(rax, "result") }
                teardownStackFrame("teardown", "exit")
                "exit" does final { returnNode(1) }
            }

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }
}

// TODO: struct prologue and epilogue test

private fun generateCFGWithSimplifiedCalls(definition: Definition.FunctionDefinition) =
    generateSimplifiedCFG(definition, realPrologue = true, realEpilogue = true, fullCallSequences = false)

private fun CFGFragmentBuilder.savePreservedRegisters(localEntry: String, localExit: String) {
    localEntry does jump("save r12") { writeRegister("saved rbx", registerUse(rbx)) }
    "save r12" does jump("save r13") { writeRegister("saved r12", registerUse(r12)) }
    "save r13" does jump("save r14") { writeRegister("saved r13", registerUse(r13)) }
    "save r14" does jump("save r15") { writeRegister("saved r14", registerUse(r14)) }
    "save r15" does jump(localExit) { writeRegister("saved r15", registerUse(r15)) }
}

private fun CFGFragmentBuilder.restorePreservedRegisters(localEntry: String, localExit: String) {
    localEntry does jump("restore r12") { writeRegister(rbx, "saved rbx") }
    "restore r12" does jump("restore r13") { writeRegister(r12, "saved r12") }
    "restore r13" does jump("restore r14") { writeRegister(r13, "saved r13") }
    "restore r14" does jump("restore r15") { writeRegister(r14, "saved r14") }
    "restore r15" does jump(localExit) { writeRegister(r15, "saved r15") }
}

private fun CFGFragmentBuilder.setupStackFrame(localEntry: String, localExit: String, allocatedSpace: Int = 0) {
    localEntry does jump("padding") { pushRegister(rbp, false) }
    "padding" does jump("save outline") { registerUse(rsp) subeq integer(8) }
    "save outline" does jump("setup rbp") { pushLabel("outline_label") }
    "setup rbp" does jump("setup rsp") { registerUse(rbp) assign (registerUse(rsp) sub integer(8)) }
    "setup rsp" does jump(localExit) { registerUse(rsp) subeq integer(8 + allocatedSpace) }
}

private fun CFGFragmentBuilder.teardownStackFrame(localEntry: String, localExit: String) {
    localEntry does jump("teardown rbp") { writeRegister(rsp, registerUse(rbp) add integer(24)) }
    "teardown rbp" does jump(localExit) { popRegister(rbp, false) }
}
