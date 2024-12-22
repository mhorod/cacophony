package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.programCfgToGraphviz
import cacophony.controlflow.registerUse
import cacophony.semantic.syntaxtree.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class StructTest {
    private fun simpleStruct(): Struct =
        Struct(
            mockRange(),
            mapOf(
                StructField(mockRange(), "a", null) to
                    Literal.IntLiteral(
                        mockRange(),
                        7,
                    ),
                StructField(mockRange(), "b", null) to
                    Literal.BoolLiteral(
                        mockRange(),
                        true,
                    ),
            ),
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
                "bodyEntry" does jump("assign b") { cacophony.controlflow.writeRegister(virA, CFGNode.ConstantKnown(7)) }
                "assign b" does jump("assign res") { cacophony.controlflow.writeRegister(virB, CFGNode.ConstantKnown(1)) }
                "assign res" does jump("bodyExit") { cacophony.controlflow.writeRegister(getResultRegister(), CFGNode.ConstantKnown(42)) }
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
                FieldRef.RValue(
                    mockRange(),
                    simpleStruct(),
                    "a",
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)

        // then

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                // this is nice :)
                "bodyEntry" does jump("bodyExit") { cacophony.controlflow.writeRegister(getResultRegister(), CFGNode.ConstantKnown(7)) }
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
                        FieldRef.RValue(mockRange(), variableUse("x"), "a"),
                    ),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)
        println(actualCFG)
        println(programCfgToGraphviz(actualCFG))
        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b") { cacophony.controlflow.writeRegister(virA, CFGNode.ConstantKnown(7)) }
                "assign b" does jump("assign res") { cacophony.controlflow.writeRegister(virB, CFGNode.ConstantKnown(1)) }
                "assign res" does jump("bodyExit") { cacophony.controlflow.writeRegister(getResultRegister(), registerUse(virA)) }
            }

        assertEquivalent(actualCFG, expectedCFG)
    }

    @Disabled
    @Test
    fun `single level return field from variable after modification`() {
        // given

        /*
         * let f = [] -> Int => (let x = {a = 7, b = true}; x.a += x.a; x.a);
         */
        val fDef =
            intFunctionDefinition(
                "f",
                listOf(),
                Block(
                    mockRange(),
                    listOf(
                        variableDeclaration("x", simpleStruct()),
                        OperatorBinary.AdditionAssignment(
                            mockRange(),
                            FieldRef.LValue(mockRange(), variableUse("x"), "a"),
                            FieldRef.RValue(mockRange(), variableUse("x"), "a"),
                        ),
                        FieldRef.RValue(mockRange(), variableUse("x"), "a"),
                    ),
                ),
            )

        // when
        val actualCFG = generateSimplifiedCFG(fDef)
        println(actualCFG)
        println(programCfgToGraphviz(actualCFG))
        // then
        val virA = Register.VirtualRegister()
        val virB = Register.VirtualRegister()

        val expectedCFG =
            singleWrappedFragmentCFG(fDef) {
                "bodyEntry" does jump("assign b") { cacophony.controlflow.writeRegister(virA, CFGNode.ConstantKnown(7)) }
                "assign b" does jump("assign res") { cacophony.controlflow.writeRegister(virB, CFGNode.ConstantKnown(1)) }
                "assign res" does jump("bodyExit") { cacophony.controlflow.writeRegister(getResultRegister(), registerUse(virA)) }
            }

//        assertEquivalent(actualCFG, expectedCFG)
    }
}
