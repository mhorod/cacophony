package cacophony.codegen.instructions.matching

import cacophony.codegen.patterns.ValuePattern
import cacophony.codegen.patterns.cacophonyPatterns.AdditionPattern
import cacophony.controlflow.*
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InstructionMatcherTest {
    @Test
    fun `matcher checks predicate`() {
        val standardAdditionPattern = AdditionPattern
        val valueLabel = ValueLabel()
        val constLabel = ConstantLabel()
        val lhsSlot = CFGNode.ValueSlot(valueLabel)
        val rhsSlot = CFGNode.ConstantSlot(constLabel) { a -> a % 2 == 0 }

        val constAdditionPattern = mockk<ValuePattern>()
        every { constAdditionPattern.tree } returns (lhsSlot add rhsSlot)

        val instructionMatcher = InstructionMatcherImpl(listOf(standardAdditionPattern, constAdditionPattern), emptyList(), emptyList())

        var node = CFGNode.Constant(1) add CFGNode.Constant(2)
        assertThat(instructionMatcher.findMatchesForValue(node, Register.VirtualRegister()).size).isEqualTo(2)

        node = CFGNode.Constant(1) add CFGNode.Constant(1)
        assertThat(instructionMatcher.findMatchesForValue(node, Register.VirtualRegister()).size).isEqualTo(1)
    }
}
