package cacophony.codegen.instructions.matching

import cacophony.codegen.instructions.cacophonyInstructions.Call
import cacophony.codegen.patterns.ValuePattern
import cacophony.codegen.patterns.cacophonyPatterns.AdditionPattern
import cacophony.codegen.patterns.cacophonyPatterns.CallPattern
import cacophony.controlflow.*
import cacophony.semantic.syntaxtree.Definition
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InstructionMatcherTest {
    private val oddNumberAdditionPattern =
        run {
            val valueLabel = ValueLabel()
            val constLabel = ConstantLabel()
            val lhsSlot = CFGNode.ValueSlot(valueLabel)
            val rhsSlot = CFGNode.ConstantSlot(constLabel) { a -> a % 2 == 0 }

            val res = mockk<ValuePattern>()

            every { res.tree } returns (lhsSlot add rhsSlot)
            every { res.makeInstance(any(), any()) } returns emptyList()
            res
        }

    @Test
    fun `matcher checks predicate`() {
        val standardAdditionPattern = AdditionPattern
        val instructionMatcher = InstructionMatcherImpl(listOf(standardAdditionPattern, oddNumberAdditionPattern), emptyList(), emptyList())

        var node = CFGNode.Constant(1) add CFGNode.Constant(2)
        assertThat(instructionMatcher.findMatchesForValue(node, Register.VirtualRegister()).size).isEqualTo(2)

        node = CFGNode.Constant(1) add CFGNode.Constant(1)
        assertThat(instructionMatcher.findMatchesForValue(node, Register.VirtualRegister()).size).isEqualTo(1)
    }

    @Test
    fun `slots are passed`() {
        val constLabel = ConstantLabel()
        val valueLabel = ValueLabel()
        val registerLabel = RegisterLabel()
        val patternTree =
            (CFGNode.ConstantSlot(constLabel) { true } add CFGNode.ValueSlot(valueLabel)) add
                CFGNode.RegisterSlot(registerLabel)
        val customAdditionPattern = mockk<ValuePattern>()
        every { customAdditionPattern.tree } returns patternTree
        every { customAdditionPattern.makeInstance(any(), any()) } returns emptyList()

        val instructionMatcher = InstructionMatcherImpl(listOf(customAdditionPattern), emptyList(), emptyList())

        val register = Register.VirtualRegister()

        val nodes = listOf(CFGNode.Constant(1), CFGNode.Constant(2) add CFGNode.Constant(3), CFGNode.RegisterUse(register))

//            +
//           / \
//          +  reg
//         / \
//        1   +
//           / \
//          2   3
        val node = (nodes[0] add nodes[1]) add nodes[2]
        val resultRegister = Register.VirtualRegister()
        val subOperationResult = Register.VirtualRegister()

        val match = instructionMatcher.findMatchesForValue(node, resultRegister).elementAt(0)

        match.instructionMaker(mapOf(valueLabel to subOperationResult))
        verify {
            customAdditionPattern.makeInstance(
                match {
                    it.constantFill == mapOf(constLabel to nodes[0]) &&
                        it.valueFill == mapOf(valueLabel to subOperationResult) &&
                        it.registerFill == mapOf(registerLabel to register)
                },
                resultRegister,
            )
        }
    }

    @Test
    fun `consts and registers count as value`() {
        val standardAdditionPattern = AdditionPattern
        val instructionMatcher = InstructionMatcherImpl(listOf(standardAdditionPattern), emptyList(), emptyList())

        val constNode = CFGNode.Constant(1)
        val registerNode = CFGNode.RegisterUse(Register.VirtualRegister())

        val node = constNode add registerNode
        assertThat(instructionMatcher.findMatchesForValue(node, Register.VirtualRegister()).size).isEqualTo(1)
    }

    @Test
    fun `matcher sets function call label`() {
        val instructionMatcher = InstructionMatcherImpl(emptyList(), listOf(CallPattern), emptyList())
        val function = Definition.FunctionDeclaration(mockk(), "f", mockk(), listOf(), mockk(), mockk())
        val node = CFGNode.Call(function)

        val match = instructionMatcher.findMatchesForSideEffects(node).elementAt(0)
        val instructions = match.instructionMaker(emptyMap())

        assertThat(instructions.size == 1)
        val instruction = instructions[0]
        assertThat(instruction is Call)
        assertThat((instruction as Call).label.name).startsWith("f_0")
    }
}
