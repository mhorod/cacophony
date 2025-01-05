package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import org.junit.jupiter.api.Test

class ReferenceTest {
    @Test
    fun `dereference simple type`() {
        // given

        /*
         * let f = [x: &Int] -> Int => @x;
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(typedArg("x", referenceType(intType()))),
                block(
                    dereference(variableUse("x")),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)
        println(programCfgToGraphviz(actualCFG))

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("bodyExit") { writeRegister(virB, memoryAccess(registerUse(virA) add integer(0))) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `dereference and operation`() {
        // given

        /*
         * let f = [x: &Int, y: &Int] -> Int => @x + @y;
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(typedArg("x", referenceType(intType())), typedArg("y", referenceType(intType()))),
                block(
                    dereference(variableUse("x")) add dereference(variableUse("y")),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)
        println(programCfgToGraphviz(actualCFG))

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()
        val virC = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does
                    jump("bodyExit") {
                        writeRegister(
                            virC,
                            memoryAccess(registerUse(virA) add integer(0)) add memoryAccess(registerUse(virB) add integer(0)),
                        )
                    }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `write to dereference`() {
        // given

        /*
         * let f = [x: &Int] -> Unit => (@x = 2;);
         */
        val fDef =
            unitFunctionDefinition(
                "f",
                listOf(typedArg("x", referenceType(intType())), typedArg("y", referenceType(intType()))),
                block(
                    dereference(variableUse("x")) assign lit(2),
                    empty(),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)
        println(programCfgToGraphviz(actualCFG))

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign res") { memoryAccess(registerUse(virA) add integer(0)) assign integer(2) }
                "assign res" does jump("bodyExit") { writeRegister(virB, integer(42)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Test
    fun `dereference in condition`() {
        // given

        /*
         * let f = [x: &Bool] -> Unit => while @x do @x = false;
         */
        val fDef =
            unitFunctionDefinition(
                "f",
                listOf(typedArg("x", referenceType(boolType()))),
                whileLoop(
                    dereference(variableUse("x")),
                    dereference(variableUse("x")) assign lit(false),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then
        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does conditional("body", "while exit") { memoryAccess(readRegister("@x") add integer(0)) neq integer(0) }
                "body" does jump("bodyEntry") { memoryAccess(readRegister("@x") add integer(0)) assign integer(0) }
                "while exit" does jump("bodyExit") { writeRegister("res", integer(42)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }
}
