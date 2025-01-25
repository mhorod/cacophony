package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.controlflow.print.programCfgToGraphviz
import org.junit.jupiter.api.Test

class LocalCallConventionTest {
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
        val actualFragment = generateSimplifiedCFG(program, fullCallSequences = true)[callerDef.value]!!

        // then
        val expectedFragment =
            standaloneWrappedCFGFragment(callerDef.value) {
                "bodyEntry" does jump("save static link") { registerUse(virtualRegister("label")) assign dataLabel("funLabel") }
                "save static link" does jump("decrement rsp") { writeRegister(virtualRegister("staticLink"), registerUse(rbp)) }
                "decrement rsp" does jump("write rdi") { registerUse(rsp) subeq integer(0) }
                "write rdi" does jump("call") { registerUse(rdi) assign registerUse(virtualRegister("staticLink")) }
                "call" does jump("increment rsp") { call(registerUse(virtualRegister("label")), 1) }
                "increment rsp" does jump("copy result1") { registerUse(rsp) addeq integer(0) }
                "copy result1" does jump("copy result2") { registerUse("vr2") assign registerUse(rax) }
                "copy result2" does jump("bodyExit") { registerUse("vr3") assign registerUse("vr2") }
            }

        print(
            programCfgToGraphviz(
                mapOf(
                    intFunctionDefinition("actual", lit(17)).value to actualFragment,
                    intFunctionDefinition("expected", lit(18)).value to expectedFragment,
                ),
            ),
        )

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
        val actualFragment = generateSimplifiedCFG(program, fullCallSequences = true)[callerDef.value]!!

        // then
        val expectedFragment =
            standaloneWrappedCFGFragment(callerDef.value) {
                "bodyEntry" does
                    jump("saveStaticLink") {
                        registerUse(virtualRegister("reg13")) assign CFGNode.DataLabel("callerLabel")
                    }
                "saveStaticLink" does
                    jump("alignRsp") {
                        registerUse(virtualRegister("reg14")) assign registerUse(rbp)
                    }
                "alignRsp" does
                    jump("loadStaticLink") {
                        (registerUse(rsp) subeq CFGNode.ConstantKnown(0))
                    }
                "loadStaticLink" does
                    jump("callInner") {
                        registerUse(rdi) assign registerUse(virtualRegister("reg14"))
                    }
                "callInner" does
                    jump("recoverRsp") {
                        call(registerUse(virtualRegister("reg13")), 1)
                    }
                "recoverRsp" does
                    jump("saveOuterResult") {
                        (registerUse(rsp) addeq CFGNode.ConstantKnown(0))
                    }
                "saveOuterResult" does
                    jump("bodyExit") {
                        registerUse(virtualRegister("reg7")) assign CFGNode.ConstantKnown(2)
                    }
            }

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
        val actualFragment = generateSimplifiedCFG(program, fullCallSequences = true)[callerDef.value]!!

        // then
        val expectedFragment =
            standaloneWrappedCFGFragment(callerDef) {
                "bodyEntry" does
                    jump("saveStaticLink") {
                        registerUse(virtualRegister("reg13")) assign CFGNode.DataLabel("funLabel")
                    }
                "saveStaticLink" does
                    jump("saveArg") {
                        registerUse(virtualRegister("reg14")) assign registerUse(rbp)
                    }
                "saveArg" does
                    jump("adjustRsp") {
                        registerUse(virtualRegister("reg15")) assign CFGNode.ConstantKnown(1)
                    }
                "adjustRsp" does
                    jump("passArg") {
                        (registerUse(rsp) subeq CFGNode.ConstantKnown(0))
                    }
                "passArg" does
                    jump("passStaticLink") {
                        registerUse(rdi) assign registerUse(virtualRegister("reg15"))
                    }
                "passStaticLink" does
                    jump("callInner") {
                        registerUse(rsi) assign registerUse(virtualRegister("reg14"))
                    }
                "callInner" does
                    jump("restoreRsp") {
                        call(registerUse(virtualRegister("reg13")), 2)
                    }
                "restoreRsp" does
                    jump("saveResult") {
                        (registerUse(rsp) addeq CFGNode.ConstantKnown(0))
                    }
                "saveResult" does
                    jump("passResult") {
                        registerUse(virtualRegister("reg16")) assign registerUse(rax)
                    }
                "passResult" does
                    jump("bodyExit") {
                        registerUse(virtualRegister("reg7")) assign registerUse(virtualRegister("reg16"))
                    }
            }

        assertFragmentIsEquivalent(actualFragment, expectedFragment)
    }

//    @Test
//    fun `call sequence for a function with seven parameters correctly forwards all provided constants as arguments`() {
//        // given
//        val calleeDef =
//            intFunctionDefinition(
//                "callee",
//                listOf(intArg("x1"), intArg("x2"), intArg("x3"), intArg("x4"), intArg("x5"), intArg("x6"), intArg("x7")),
//                variableUse("x1"),
//            )
//        val callerDef = intFunctionDefinition("caller", call("callee", lit(1), lit(2), lit(3), lit(4), lit(5), lit(6), lit(7)))
//        /*
//         * let callee = [x1: Int, x2: Int, x3: Int, x4: Int, x5: Int, x6: Int, x7: Int] -> Int => x1;
//         * let caller = [] -> Int => callee[1,2,3,4,5,6,7];
//         */
//        val program = block(calleeDef, callerDef)
//
//        // when
//        val actualFragment = generateSimplifiedCFG(program, fullCallSequences = true)[callerDef]!!
//
//        // then
//        val expectedFragment =
//            standaloneWrappedCFGFragment(callerDef) {
//                "bodyEntry" does
//                    jump("saveStaticLink") {
//                        registerUse(virtualRegister("reg19")) assign CFGNode.DataLabel("innerLabel")
//                    }
//                "saveStaticLink" does
//                    jump("saveArg1") {
//                        registerUse(virtualRegister("reg20")) assign registerUse(rbp)
//                    }
//                "saveArg1" does
//                    jump("saveArg2") {
//                        registerUse(virtualRegister("reg21")) assign CFGNode.ConstantKnown(1)
//                    }
//                "saveArg2" does
//                    jump("saveArg3") {
//                        registerUse(virtualRegister("reg22")) assign CFGNode.ConstantKnown(2)
//                    }
//                "saveArg3" does
//                    jump("saveArg4") {
//                        registerUse(virtualRegister("reg23")) assign CFGNode.ConstantKnown(3)
//                    }
//                "saveArg4" does
//                    jump("saveArg5") {
//                        registerUse(virtualRegister("reg24")) assign CFGNode.ConstantKnown(4)
//                    }
//                "saveArg5" does
//                    jump("saveArg6") {
//                        registerUse(virtualRegister("reg25")) assign CFGNode.ConstantKnown(5)
//                    }
//                "saveArg6" does
//                    jump("saveArg7") {
//                        registerUse(virtualRegister("reg26")) assign CFGNode.ConstantKnown(6)
//                    }
//                "saveArg7" does
//                    jump("adjustRsp") {
//                        registerUse(virtualRegister("reg27")) assign CFGNode.ConstantKnown(7)
//                    }
//                "adjustRsp" does
//                    jump("storeArg7") {
//                        (registerUse(rsp) subeq CFGNode.ConstantKnown(0))
//                    }
//                "storeArg7" does
//                    jump("storeStaticLink") {
//                        registerUse(virtualRegister("reg29")) assign registerUse(virtualRegister("reg27"))
//                    }
//                "storeStaticLink" does
//                    jump("passArg1") {
//                        registerUse(virtualRegister("reg30")) assign registerUse(virtualRegister("reg20"))
//                    }
//                "passArg1" does
//                    jump("passArg2") {
//                        registerUse(rdi) assign registerUse(virtualRegister("reg21"))
//                    }
//                "passArg2" does
//                    jump("passArg3") {
//                        registerUse(rsi) assign registerUse(virtualRegister("reg22"))
//                    }
//                "passArg3" does
//                    jump("passArg4") {
//                        registerUse(rdx) assign registerUse(virtualRegister("reg23"))
//                    }
//                "passArg4" does
//                    jump("passArg5") {
//                        registerUse(rcx) assign registerUse(virtualRegister("reg24"))
//                    }
//                "passArg5" does
//                    jump("passArg6") {
//                        registerUse(r8) assign registerUse(virtualRegister("reg25"))
//                    }
//                "passArg6" does
//                    jump("passStaticLink") {
//                        registerUse(r9) assign registerUse(virtualRegister("reg26"))
//                    }
//                "passStaticLink" does
//                    jump("passArg7") {
//                        CFGNode.Push(registerUse(virtualRegister("reg30")))
//                    }
//                "passArg7" does
//                    jump("callInner") {
//                        CFGNode.Push(registerUse(virtualRegister("reg29")))
//                    }
//                "callInner" does
//                    jump("adjustRsp") {
//                        call(registerUse(virtualRegister("reg19")), 8)
//                    }
//                "adjustRsp" does
//                    jump("saveInnerResult") {
//                        (registerUse(rsp) addeq CFGNode.ConstantKnown(16))
//                    }
//                "saveInnerResult" does
//                    jump("passInnerResult") {
//                        registerUse(virtualRegister("reg28")) assign registerUse(rax)
//                    }
//                "passInnerResult" does
//                    jump("bodyExit") {
//                        registerUse(virtualRegister("reg13")) assign registerUse(virtualRegister("reg28"))
//                    }
// //                // The arguments are prepared in a temporary registers...
// //                "bodyEntry" does jump("prepare arg2") { writeRegister("arg1", integer(1)) }
// //                "prepare arg2" does jump("prepare arg3") { writeRegister("arg2", integer(2)) }
// //                "prepare arg3" does jump("prepare arg4") { writeRegister("arg3", integer(3)) }
// //                "prepare arg4" does jump("prepare arg5") { writeRegister("arg4", integer(4)) }
// //                "prepare arg5" does jump("prepare arg6") { writeRegister("arg5", integer(5)) }
// //                "prepare arg6" does jump("prepare arg7") { writeRegister("arg6", integer(6)) }
// //                "prepare arg7" does jump("prepare rsp") { writeRegister("arg7", integer(7)) }
// //                "prepare rsp" does jump("prepare arg7 for push") { registerUse(rsp) subeq integer(0) }
// //                // ...and then they are passed via stack
// //                "prepare arg7 for push" does
// //                    jump("prepare static link for push") {
// //                        writeRegister(
// //                            "temp arg7",
// //                            registerUse(virtualRegister("arg7")),
// //                        )
// //                    }
// //                "prepare static link for push" does
// //                    jump("pass arg1") {
// //                        writeRegister(
// //                            "temp static link",
// //                            registerUse(rbp),
// //                        )
// //                    }
// //                // ...or via their respective registers
// //                "pass arg1" does jump("pass arg2") { writeRegister(rdi, registerUse(virtualRegister("arg1"))) }
// //                "pass arg2" does jump("pass arg3") { writeRegister(rsi, registerUse(virtualRegister("arg2"))) }
// //                "pass arg3" does jump("pass arg4") { writeRegister(rdx, registerUse(virtualRegister("arg3"))) }
// //                "pass arg4" does jump("pass arg5") { writeRegister(rcx, registerUse(virtualRegister("arg4"))) }
// //                "pass arg5" does jump("pass arg6") { writeRegister(r8, registerUse(virtualRegister("arg5"))) }
// //                "pass arg6" does jump("push static link") { writeRegister(r9, registerUse(virtualRegister("arg6"))) }
// //                "push static link" does jump("push arg7") { pushRegister("temp static link") }
// //                "push arg7" does jump("call") { pushRegister("temp arg7") }
// //                // "call" does jump("restore rsp") { call(calleeDef) }
// //                // The argument still on the stack must then be ignored
// //                "restore rsp" does jump("extract result") { registerUse(rsp) addeq integer(16) }
// //                "extract result" does jump("forward result") { writeRegister("result", registerUse(rax)) }
// //                "forward result" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse(virtualRegister("result"))) }
//            }
//
//        assertFragmentIsEquivalent(actualFragment, expectedFragment)
//    }
//
    @Test
    fun `call sequence for a function with eight parameters correctly forwards all provided constants as arguments`() {
        // given
        val calleeDef =
            intFunctionDefinition(
                "callee",
                listOf(intArg("x1"), intArg("x2"), intArg("x3"), intArg("x4"), intArg("x5"), intArg("x6"), intArg("x7"), intArg("x8")),
                variableUse("x1"),
            )
        val callerDef = intFunctionDefinition("caller", call("callee", lit(1), lit(2), lit(3), lit(4), lit(5), lit(6), lit(7), lit(8)))
        /*
         * let callee = [x1: Int, x2: Int, x3: Int, x4: Int, x5: Int, x6: Int, x7: Int, x8: Int] -> Int => x1;
         * let caller = [] -> Int => callee[1,2,3,4,5,6,7,8];
         */
        val program = block(calleeDef, callerDef)

        // when
        val actualFragment = generateSimplifiedCFG(program, fullCallSequences = true)[callerDef.value]!!

        val expectedFragment =
            standaloneWrappedCFGFragment(callerDef) {
                "bodyEntry" does
                    jump("saveStaticLink") {
                        registerUse(virtualRegister("reg20")) assign CFGNode.DataLabel("innerFunLabel")
                    }
                "saveStaticLink" does
                    jump("v@38fd3840") {
                        registerUse(virtualRegister("reg21")) assign registerUse(rbp)
                    }
                "v@38fd3840" does
                    jump("v@27e8e6a7") {
                        registerUse(virtualRegister("reg22")) assign CFGNode.ConstantKnown(1)
                    }
                "v@27e8e6a7" does
                    jump("v@33edfff2") {
                        registerUse(virtualRegister("reg23")) assign CFGNode.ConstantKnown(2)
                    }
                "v@33edfff2" does
                    jump("v@2914c6c1") {
                        registerUse(virtualRegister("reg24")) assign CFGNode.ConstantKnown(3)
                    }
                "v@2914c6c1" does
                    jump("v@78fad49b") {
                        registerUse(virtualRegister("reg25")) assign CFGNode.ConstantKnown(4)
                    }
                "v@78fad49b" does
                    jump("v@1847d8fd") {
                        registerUse(virtualRegister("reg26")) assign CFGNode.ConstantKnown(5)
                    }
                "v@1847d8fd" does
                    jump("v@4489a668") {
                        registerUse(virtualRegister("reg27")) assign CFGNode.ConstantKnown(6)
                    }
                "v@4489a668" does
                    jump("v@4ba452f6") {
                        registerUse(virtualRegister("reg28")) assign CFGNode.ConstantKnown(7)
                    }
                "v@4ba452f6" does
                    jump("v@2608bae4") {
                        registerUse(virtualRegister("reg29")) assign CFGNode.ConstantKnown(8)
                    }
                "v@2608bae4" does
                    jump("v@12ea9fdf") {
                        (registerUse(rsp) subeq CFGNode.ConstantKnown(8))
                    }
                "v@12ea9fdf" does
                    jump("vl@16610ef") {
                        registerUse(virtualRegister("reg31")) assign registerUse(virtualRegister("reg28"))
                    }
                "vl@16610ef" does
                    jump("v@4ac522ef") {
                        registerUse(virtualRegister("reg32")) assign registerUse(virtualRegister("reg29"))
                    }
                "v@4ac522ef" does
                    jump("v@29796ec9") {
                        registerUse(virtualRegister("reg33")) assign registerUse(virtualRegister("reg21"))
                    }
                "v@29796ec9" does
                    jump("v@6fab185e") {
                        registerUse(rdi) assign registerUse(virtualRegister("reg22"))
                    }
                "v@6fab185e" does
                    jump("v@5e7b8c2c") {
                        registerUse(rsi) assign registerUse(virtualRegister("reg23"))
                    }
                "v@5e7b8c2c" does
                    jump("v@3dca1f56") {
                        registerUse(rdx) assign registerUse(virtualRegister("reg24"))
                    }
                "v@3dca1f56" does
                    jump("v@5f272100") {
                        registerUse(rcx) assign registerUse(virtualRegister("reg25"))
                    }
                "v@5f272100" does
                    jump("v@68f9ab52") {
                        registerUse(r8) assign registerUse(virtualRegister("reg26"))
                    }
                "v@68f9ab52" does
                    jump("v@6ae1408d") {
                        registerUse(r9) assign registerUse(virtualRegister("reg27"))
                    }
                "v@6ae1408d" does
                    jump("vl@73cc228") {
                        CFGNode.Push(registerUse(virtualRegister("reg33")))
                    }
                "vl@73cc228" does
                    jump("v@6175ccee") {
                        CFGNode.Push(registerUse(virtualRegister("reg32")))
                    }
                "v@6175ccee" does
                    jump("v@44f358a9") {
                        CFGNode.Push(registerUse(virtualRegister("reg31")))
                    }
                "v@44f358a9" does
                    jump("v@24f69d92") {
                        call(registerUse(virtualRegister("reg20")), 9)
                    }
                "v@24f69d92" does
                    jump("v@453a26a6") {
                        (registerUse(rsp) addeq CFGNode.ConstantKnown(32))
                    }
                "v@453a26a6" does
                    jump("v@540fb7b6") {
                        registerUse(virtualRegister("reg30")) assign registerUse(rax)
                    }
                "v@540fb7b6" does
                    jump("bodyExit") {
                        registerUse(virtualRegister("reg14")) assign registerUse(virtualRegister("reg30"))
                    }
            }

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
        val actualFragment = generateSimplifiedCFG(program, fullCallSequences = true)[callerDef.value]!!

        // then
        val expectedFragment =
            standaloneWrappedCFGFragment(callerDef) {
                "bodyEntry" does
                    jump("v@7eb1009e") {
                        registerUse(virtualRegister("reg13")) assign CFGNode.DataLabel("innerFunLabel")
                    }
                "v@7eb1009e" does
                    jump("v@78debb5c") {
                        registerUse(virtualRegister("reg14")) assign registerUse(rbp)
                    }
                "v@78debb5c" does
                    jump("vl@e9df129") {
                        registerUse(virtualRegister("reg15")) assign CFGNode.DataLabel("innerFunLabel")
                    }
                "vl@e9df129" does
                    jump("v@5ffdcb6e") {
                        registerUse(virtualRegister("reg16")) assign registerUse(rbp)
                    }
                "v@5ffdcb6e" does
                    jump("v@2a027680") {
                        registerUse(virtualRegister("reg17")) assign CFGNode.ConstantKnown(1)
                    }
                "v@2a027680" does
                    jump("v@3071c6a2") {
                        (registerUse(rsp) subeq CFGNode.ConstantKnown(0))
                    }
                "v@3071c6a2" does
                    jump("v@2ac2a433") {
                        registerUse(rdi) assign registerUse(virtualRegister("reg17"))
                    }
                "v@2ac2a433" does
                    jump("v@137d5e38") {
                        registerUse(rsi) assign registerUse(virtualRegister("reg16"))
                    }
                "v@137d5e38" does
                    jump("v@2af29ca1") {
                        call(registerUse(virtualRegister("reg15")), 2)
                    }
                "v@2af29ca1" does
                    jump("v@14880b8b") {
                        (registerUse(rsp) addeq CFGNode.ConstantKnown(0))
                    }
                "v@14880b8b" does
                    jump("v@47f1a7e5") {
                        registerUse(virtualRegister("reg18")) assign registerUse(rax)
                    }
                "v@47f1a7e5" does
                    jump("v@39907aa0") {
                        (registerUse(rsp) subeq CFGNode.ConstantKnown(0))
                    }
                "v@39907aa0" does
                    jump("vl@2b6678e") {
                        registerUse(rdi) assign registerUse(virtualRegister("reg18"))
                    }
                "vl@2b6678e" does
                    jump("v@7825d9e8") {
                        registerUse(rsi) assign registerUse(virtualRegister("reg14"))
                    }
                "v@7825d9e8" does
                    jump("v@32219786") {
                        call(registerUse(virtualRegister("reg13")), 2)
                    }
                "v@32219786" does
                    jump("vl@85ebe18") {
                        (registerUse(rsp) addeq CFGNode.ConstantKnown(0))
                    }
                "vl@85ebe18" does
                    jump("v@6fd946d7") {
                        registerUse(virtualRegister("reg19")) assign registerUse(rax)
                    }
                "v@6fd946d7" does
                    jump("bodyExit") {
                        registerUse(virtualRegister("reg7")) assign registerUse(virtualRegister("reg19"))
                    }
            }

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
        val actualCFG = generateSimplifiedCFG(fDef, fullCallSequences = true)

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("v@6b2e0fb7") {
                        registerUse(virtualRegister("reg6")) assign CFGNode.DataLabel("funLabel")
                    }
                "v@6b2e0fb7" does
                    jump("v@236734a4") {
                        registerUse(virtualRegister("reg7")) assign registerUse(rbp)
                    }
                "v@236734a4" does
                    jump("v@1e307175") {
                        (registerUse(rsp) subeq CFGNode.ConstantKnown(0))
                    }
                "v@1e307175" does
                    jump("v@250d213a") {
                        registerUse(rdi) assign registerUse(virtualRegister("reg7"))
                    }
                "v@250d213a" does
                    jump("v@7c4ff9bd") {
                        call(registerUse(virtualRegister("reg6")), 1)
                    }
                "v@7c4ff9bd" does
                    jump("v@66973084") {
                        (registerUse(rsp) addeq CFGNode.ConstantKnown(0))
                    }
                "v@66973084" does
                    jump("v@6b34341f") {
                        registerUse(virtualRegister("reg8")) assign registerUse(rax)
                    }
                "v@6b34341f" does
                    jump("bodyExit") {
                        registerUse(virtualRegister("reg0")) assign (CFGNode.ConstantKnown(1) add registerUse(virtualRegister("reg8")))
                    }
            }

        // then
        assertEquivalent(actualCFG, expectedCFG)
    }
}
