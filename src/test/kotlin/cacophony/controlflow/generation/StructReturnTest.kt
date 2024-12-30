package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.semantic.syntaxtree.*
import org.junit.jupiter.api.Test

class StructReturnTest {
    private fun simpleStruct(int: Int = 7, bool: Boolean = true): Struct =
        structDeclaration(
            structField("a") to lit(int),
            structField("b") to lit(bool),
        )

    private fun simpleType(): BaseType.Structural = structType("a" to intType(), "b" to boolType())

    private fun nestedStruct(): Struct =
        structDeclaration(
            structField("a") to lit(5),
            structField("b") to simpleStruct(),
        )

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
        println(programCfgToGraphviz(actualCFG))

        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b") { writeRegister(virA, integer(7)) }
                "assign b" does jump("assign res") { writeRegister(virB, integer(1)) }
                "assign res" does jump("bodyExit") { writeRegister(getResultRegister(), registerUse(virA)) }
            }

//        assertEquivalent(actualCFG, expectedCFG)
    }
}
