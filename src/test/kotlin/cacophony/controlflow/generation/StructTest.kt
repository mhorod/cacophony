package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.semantic.syntaxtree.*
import org.junit.jupiter.api.Test

class StructTest {
    private fun simpleStruct(int: Int = 7, bool: Boolean = true): Struct =
        structDeclaration(
            structField("a") to lit(int),
            structField("b") to lit(bool),
        )

    private fun nestedStruct(): Struct =
        structDeclaration(
            structField("a") to lit(5),
            structField("b") to simpleStruct(),
        )

    @Test
    fun `single level struct assignment`() {
        // given

        /*
         * let f = [] -> Unit => let x = {a = 7, b = true};
         */
        val fDef =
            unitFunctionDefinition(
                "f",
                listOf(),
                variableDeclaration(
                    "x",
                    simpleStruct(),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b") { writeRegister(virA, integer(7)) }
                "assign b" does jump("assign res") { writeRegister(virB, integer(1)) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), integer(42)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `single level return field from immediate`() {
        // given

        /*
         * let f = [] -> Int => {a = 7, b = true}.a;
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(),
                rvalueFieldRef(simpleStruct(), "a"),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                // this is nice :)
                "bodyEntry" does jump("bodyExit") { writeRegister(getResultRegister(), integer(7)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `single level return field from variable`() {
        // given

        /*
         * let f = [] -> Int => (let x = {a = 7, b = true}; x.a);
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(),
                Block(
                    mockRange(),
                    listOf(
                        variableDeclaration("x", simpleStruct()),
                        rvalueFieldRef(variableUse("x"), "a"),
                    ),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b") { writeRegister(virA, integer(7)) }
                "assign b" does jump("assign res") { writeRegister(virB, integer(1)) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse(virA)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `single level return field from variable after modification`() {
        // given

        /*
         * let f = [] -> Int => (let x = {a = 7, b = true}; x.a = x.a + 12; x.a);
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(),
                Block(
                    mockRange(),
                    listOf(
                        variableDeclaration("x", simpleStruct()),
                        lvalueFieldRef(variableUse("x"), "a") addeq rvalueFieldRef(variableUse("x"), "a"),
                        rvalueFieldRef(variableUse("x"), "a"),
                    ),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b") { writeRegister(virA, integer(7)) }
                "assign b" does jump("+=") { writeRegister(virB, integer(1)) }
                "+=" does jump("assign res") { CFGNode.AdditionAssignment(registerUse(virA), registerUse(virA)) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse(virA)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `nested level single access return field from immediate`() {
        // given

        /*
         * let f = [] -> Int => {a = 5, b = {a = 7, b = true} }.a;
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(),
                rvalueFieldRef(nestedStruct(), "a"),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                // this is nice :)
                "bodyEntry" does jump("bodyExit") { writeRegister(getResultRegister(), integer(5)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `nested level single access return field from variable`() {
        // given

        /*
         * let f = [] -> Int => (let x = {a = 5, b = {a = 7, b = true} }; x.a);
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(),
                block(
                    variableDeclaration("x", nestedStruct()),
                    rvalueFieldRef(variableUse("x"), "a"),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()
        val virC = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b.a") { writeRegister(virA, integer(5)) }
                "assign b.a" does jump("assign b.b") { writeRegister(virB, integer(7)) }
                "assign b.b" does jump("assign res") { writeRegister(virC, integer(1)) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse(virA)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `nested level nested access return field from variable`() {
        // given

        /*
         * let f = [] -> Int => (let x = {a = 5, b = {a = 7, b = true} }; x.b.a);
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(),
                block(
                    variableDeclaration("x", nestedStruct()),
                    rvalueFieldRef(rvalueFieldRef(variableUse("x"), "b"), "a"),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()
        val virC = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b.a") { writeRegister(virA, integer(5)) }
                "assign b.a" does jump("assign b.b") { writeRegister(virB, integer(7)) }
                "assign b.b" does jump("assign res") { writeRegister(virC, integer(1)) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse(virB)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `assign same simple`() {
        // given

        /*
         * let f = [] -> Unit => (let x = {a = 7, b = true}; x = {a = 5, b = false};);
         */
        val fDef =
            unitFunctionDefinition(
                "f",
                listOf(),
                block(
                    variableDeclaration("x", simpleStruct()),
                    variableUse("x") assign simpleStruct(5, false),
                    empty(),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b") { writeRegister(virA, integer(7)) }
                "assign b" does jump("reassign a") { writeRegister(virB, integer(1)) }
                "reassign a" does jump("reassign b") { writeRegister(virA, integer(5)) }
                "reassign b" does jump("assign res") { writeRegister(virB, integer(0)) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), integer(42)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `assign switched simple`() {
        // given

        /*
         * let f = [] -> Unit => (let x = {a = 7, b = true}; x = {b = false, a = 5};);
         */
        val fDef =
            unitFunctionDefinition(
                "f",
                listOf(),
                block(
                    variableDeclaration("x", simpleStruct()),
                    variableUse("x") assign struct("b" to lit(false), "a" to lit(5)),
                    empty(),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b") { writeRegister(virA, integer(7)) }
                "assign b" does jump("reassign a") { writeRegister(virB, integer(1)) }
                "reassign a" does jump("reassign b") { writeRegister(virA, integer(5)) }
                "reassign b" does jump("assign res") { writeRegister(virB, integer(0)) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), integer(42)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `reassign in nested`() {
        // given

        /*
         * let f = [] -> Int => (let x = {a = 5, b = {a = 7, b = true} }; x.b = {a = 5, b = false}; x.b.a);
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(),
                block(
                    variableDeclaration("x", nestedStruct()),
                    lvalueFieldRef(variableUse("x"), "b") assign simpleStruct(5, false),
                    rvalueFieldRef(rvalueFieldRef(variableUse("x"), "b"), "a"),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()
        val virC = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b.a") { writeRegister(virA, integer(5)) }
                "assign b.a" does jump("assign b.b") { writeRegister(virB, integer(7)) }
                "assign b.b" does jump("reassign b.a") { writeRegister(virC, integer(1)) }
                "reassign b.a" does jump("reassign b.b") { writeRegister(virB, integer(5)) }
                "reassign b.b" does jump("assign res") { writeRegister(virC, integer(0)) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse(virB)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    // TODO: fix this test - accessing subfield makes whole structure land on stack
    @Test
    fun `access simple subfield in nested function`() {
        // given

        /*
         * let f = [] -> Int => (
         *  let x = {a = 7, b = true};
         *  let g = [] -> Int => x.a = 3;
         *  x.a
         * );
         */
        val gDef =
            intFunctionDefinition(
                "g",
                listOf(),
                lvalueFieldRef(variableUse("x"), "a") assign lit(3),
            )
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(),
                block(
                    variableDeclaration("x", simpleStruct()),
                    gDef,
                    rvalueFieldRef(variableUse("x"), "a"),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)
        val fCFG = actualCFG[fDef]!!
        val gCFG = actualCFG[gDef]!!

        // then

        val expectedCFGf =
            standaloneWrappedCFGFragment(fDef) {
                "bodyEntry" does jump("assign b") { memoryAccess(registerUse(rbp) sub integer(8)) assign integer(7) }
                "assign b" does jump("assign res") { registerUse(virtualRegister("x.b")) assign integer(1) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), memoryAccess(registerUse(rbp) sub integer(8))) }
            }

        val expectedCFGg =
            standaloneWrappedCFGFragment(gDef) {
                "bodyEntry" does
                    jump("bodyExit") {
                        writeRegister(
                            getResultRegister(),
                            memoryAccess(memoryAccess(registerUse(rbp)) sub integer(8)) assign integer(3),
                        )
                    }
            }

        // TODO: expected should be different
        assertFragmentIsEquivalent(fCFG, expectedCFGf)
        assertFragmentIsEquivalent(gCFG, expectedCFGg)
    }
}
