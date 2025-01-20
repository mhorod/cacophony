package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import org.junit.jupiter.api.Test

class StructReturnTest {
    @Test
    fun `return primitive field`() {
        // given

        /*
         * let f = [] -> Int => (let x = {a = 7, b = true}; x.a);
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(),
                block(
                    variableDeclaration("x", simpleStruct()),
                    rvalueFieldRef(variableUse("x"), "a"),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b") { writeRegister("virA", integer(7)) }
                "assign b" does jump("assign res") { writeRegister("virB", integer(1)) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse("virA")) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `return whole struct immediate no call sequence`() {
        // given

        /*
         * let f = [] -> {a: Int, b: Bool} => {a = 7, b = true};
         */
        val fDef =
            functionDefinition(
                "f",
                listOf(),
                simpleStruct(),
                simpleType(),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b") { writeRegister("virA", integer(7)) }
                "assign b" does jump("bodyExit") { writeRegister("virB", integer(1)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `return whole struct variable no call sequence`() {
        // given

        /*
         * let f = [] -> {a: Int, b: Bool} => (let x = {a = 7, b = true}; x);
         */
        val fDef =
            functionDefinition(
                "f",
                listOf(),
                block(
                    variableDeclaration("x", simpleStruct()),
                    variableUse("x"),
                ),
                simpleType(),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign var b") { writeRegister("virA", integer(7)) }
                "assign var b" does jump("assign res a") { writeRegister("virB", integer(1)) }
                "assign res a" does jump("assign res b") { writeRegister("virC", registerUse("virA")) }
                "assign res b" does jump("bodyExit") { writeRegister("virD", registerUse("virB")) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `return sub-struct variable no call sequence`() {
        // given

        /*
         * let f = [] -> {a: Int, b: Bool} => (
         *  let x = {a = 5, b = {a = 7, b = true}};
         *  x.b
         * );
         */
        val fDef =
            functionDefinition(
                "f",
                listOf(),
                block(
                    variableDeclaration("x", nestedStruct()),
                    rvalueFieldRef(variableUse("x"), "b"),
                ),
                simpleType(),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign var b.a") { writeRegister("virA", integer(5)) }
                "assign var b.a" does jump("assign var b.b") { writeRegister("virB", integer(7)) }
                "assign var b.b" does jump("assign res a") { writeRegister("virC", integer(1)) }
                "assign res a" does jump("assign res b") { writeRegister("virD", registerUse("virB")) }
                "assign res b" does jump("bodyExit") { writeRegister("virE", registerUse("virC")) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `return sub-struct variable modification no call sequence`() {
        // given

        /*
         * let f = [] -> {a: Int, b: Bool} => (
         *  let x = {a = 5, b = {a = 7, b = true}};
         *  x.b.a += 2;
         *  x.b
         * );
         */
        val fDef =
            functionDefinition(
                "f",
                listOf(),
                block(
                    variableDeclaration("x", nestedStruct()),
                    lvalueFieldRef(lvalueFieldRef(variableUse("x"), "b"), "a") addeq lit(2),
                    rvalueFieldRef(variableUse("x"), "b"),
                ),
                simpleType(),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign var b.a") { writeRegister("virA", integer(5)) }
                "assign var b.a" does jump("assign var b.b") { writeRegister("virB", integer(7)) }
                "assign var b.b" does jump("addeq") { writeRegister("virC", integer(1)) }
                "addeq" does jump("assign res a") { registerUse("virB") addeq integer(2) }
                "assign res a" does jump("assign res b") { writeRegister("virD", registerUse("virB")) }
                "assign res b" does jump("bodyExit") { writeRegister("virE", registerUse("virC")) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    // TODO: rewrite
//    @Test
//    fun `call sequence - return struct simple no args`() {
//        // given
//
//        /* let g = [] -> Int => (
//         *  let f = [] -> {a: Int, b: Bool} => {a = 7, b = true};
//         *  f[].a
//         * );
//         */
//        val fDef =
//            functionDefinition(
//                "f",
//                listOf(),
//                simpleStruct(),
//                simpleType(),
//            )
//
//        val gDef =
//            intFunctionDefinition(
//                "g",
//                listOf(),
//                block(
//                    fDef,
//                    rvalueFieldRef(call(variableUse("f")), "a"),
//                ),
//            )
//
//        // when
//        val actualCFG = generateSimplifiedCFG(gDef, fullCallSequences = true)[gDef]!!
//
//        // then
//        val expectedCFG =
//            standaloneWrappedCFGFragment(gDef) {
//                "bodyEntry" does jump("assign sl") { registerUse(rsp) subeq integer(0) }
//                "assign sl" does jump("call f") { writeRegister(rdi, registerUse(rbp)) }
//                "call f" does jump("restore rsp") { call(fDef) }
//                "restore rsp" does jump("assign a") { registerUse(rsp) addeq integer(0) }
//                "assign a" does jump("assign b") { writeRegister("virA", registerUse(rax)) }
//                "assign b" does jump("assign res") { writeRegister("virB", registerUse(rdi)) }
//                "assign res" does jump("bodyExit") { writeRegister("virC", registerUse("virA")) }
//            }
//
//        assertFragmentIsEquivalent(actualCFG, expectedCFG)
//    }

//    @Test
//    fun `call sequence - return struct nested few args`() {
//        // given
//
//        /* let g = [] -> Int => (
//         *  let f = [x: Int, y: Int, z: Int] -> {a: Int, b: Bool} => {a = 5, b = {a = 7, b = true}};
//         *  f[0,1,2].a
//         * );
//         */
//        val fDef =
//            functionDefinition(
//                "f",
//                listOf(intArg("x"), intArg("y"), intArg("z")),
//                nestedStruct(),
//                nestedType(),
//            )
//
//        val gDef =
//            intFunctionDefinition(
//                "g",
//                listOf(),
//                block(
//                    fDef,
//                    rvalueFieldRef(call(variableUse("f"), lit(0), lit(1), lit(2)), "a"),
//                ),
//            )
//
//        // when
//        val actualCFG = generateSimplifiedCFG(gDef, fullCallSequences = true)[gDef]!!
//
//        // then
//        val expectedCFG =
//            standaloneWrappedCFGFragment(gDef) {
//                "bodyEntry" does jump("prep arg2") { writeRegister("arg1", integer(0)) }
//                "prep arg2" does jump("prep arg3") { writeRegister("arg2", integer(1)) }
//                "prep arg3" does jump("prep rsp") { writeRegister("arg3", integer(2)) }
//                "prep rsp" does jump("assign arg1") { registerUse(rsp) subeq integer(0) }
//                "assign arg1" does jump("assign arg2") { writeRegister(rdi, registerUse("arg1")) }
//                "assign arg2" does jump("assign arg3") { writeRegister(rsi, registerUse("arg2")) }
//                "assign arg3" does jump("assign sl") { writeRegister(rdx, registerUse("arg3")) }
//                "assign sl" does jump("call f") { writeRegister(rcx, registerUse(rbp)) }
//                "call f" does jump("restore rsp") { call(fDef) }
//                "restore rsp" does jump("assign a") { registerUse(rsp) addeq integer(0) }
//                "assign a" does jump("assign b.a") { writeRegister("virA", registerUse(rax)) }
//                "assign b.a" does jump("assign b.b") { writeRegister("virB", registerUse(rdi)) }
//                "assign b.b" does jump("assign res") { writeRegister("virC", registerUse(rsi)) }
//                "assign res" does jump("bodyExit") { writeRegister("virD", registerUse("virA")) }
//            }
//
//        assertFragmentIsEquivalent(actualCFG, expectedCFG)
//    }

    @Test
    fun `return with struct and value`() {
        /*
         * let f = [] -> {a:Int,b:Bool} => (
         *  return {a:7, b:true};
         *  {a:7, b:true}
         * );
         */

        // given
        val fDef =
            functionDefinition(
                "f",
                emptyList(),
                block(
                    returnStatement(simpleStruct()),
                    simpleStruct(),
                ),
                simpleType(),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign res.b") { writeRegister(virtualRegister("res.a"), integer(7)) }
                "assign res.b" does jump("bodyExit") { writeRegister(virtualRegister("res.b"), integer(1)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `return with struct`() {
        /*
         * let f = [] -> {a:Int,b:Bool} => return {a:7, b:true};
         */

        // given
        val fDef =
            functionDefinition(
                "f",
                emptyList(),
                returnStatement(simpleStruct()),
                simpleType(),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign res.b") { writeRegister(virtualRegister("res.a"), integer(7)) }
                "assign res.b" does jump("bodyExit") { writeRegister(virtualRegister("res.b"), integer(1)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
