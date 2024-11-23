package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.matching.InstructionMatcher
import cacophony.codegen.instructions.matching.Match
import cacophony.controlflow.CFGNode
import cacophony.controlflow.ValueLabel
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CacophonyInstructionCoveringTest {
    @Test
    fun `largest instruction is chosen`() {
        val instrMatcher = mockk<InstructionMatcher>()
        val instrCovering = CacophonyInstructionCovering(instrMatcher)
        val root = mockk<CFGNode>()
        val instr1 = mockk<Instruction>()
        val match1 = Match({ _ -> listOf(instr1) }, emptyMap(), 1)
        val instr2 = mockk<Instruction>()
        val match2 = Match({ _ -> listOf(instr2) }, emptyMap(), 2)
        every { instrMatcher.findMatchesForSideEffects(root) } returns setOf(match1, match2)

        assertThat(instrCovering.coverWithInstructions(root)).containsExactly(instr2)
    }

    @Test
    fun `largest instruction is chosen when jumping`() {
        val instrMatcher = mockk<InstructionMatcher>()
        val instrCovering = CacophonyInstructionCovering(instrMatcher)
        val root = mockk<CFGNode>()
        val instr1 = mockk<Instruction>()
        val match1 = Match({ _ -> listOf(instr1) }, emptyMap(), 1)
        val instr2 = mockk<Instruction>()
        val match2 = Match({ _ -> listOf(instr2) }, emptyMap(), 2)
        val label = BlockLabel("label")
        every { instrMatcher.findMatchesForCondition(root, label) } returns setOf(match1, match2)

        assertThat(instrCovering.coverWithInstructionsAndJump(root, label)).containsExactly(instr2)
    }

    @Test
    fun `covering properly recurses`() {
        //     1
        //   /   \
        //  2     3
        //         \
        //          4
        val instrMatcher = mockk<InstructionMatcher>()
        val instrCovering = CacophonyInstructionCovering(instrMatcher)
        val root = mockk<CFGNode>()
        val node2 = mockk<CFGNode>()
        val node3 = mockk<CFGNode>()
        val node4 = mockk<CFGNode>()
        val instr1 = mockk<Instruction>()
        val match1 = Match({ _ -> listOf(instr1) }, mapOf(ValueLabel() to node2, ValueLabel() to node3), 1)
        every { instrMatcher.findMatchesForSideEffects(root) } returns setOf(match1)
        val instr2 = mockk<Instruction>()
        val match2 = Match({ _ -> listOf(instr2) }, emptyMap(), 1)
        every { instrMatcher.findMatchesForValue(node2, any()) } returns setOf(match2)
        val instr3 = mockk<Instruction>()
        val match3 = Match({ _ -> listOf(instr3) }, mapOf(ValueLabel() to node4), 1)
        every { instrMatcher.findMatchesForValue(node3, any()) } returns setOf(match3)
        val instr4 = mockk<Instruction>()
        val match4 = Match({ _ -> listOf(instr4) }, emptyMap(), 1)
        every { instrMatcher.findMatchesForValue(node4, any()) } returns setOf(match4)

        val instructions = instrCovering.coverWithInstructions(root)
        assertThat(instructions).containsExactlyInAnyOrder(instr1, instr2, instr3, instr4)
        assertThat(instructions.indexOf(instr2)).isLessThan(instructions.indexOf(instr1))
        assertThat(instructions.indexOf(instr3)).isLessThan(instructions.indexOf(instr1))
        assertThat(instructions.indexOf(instr4)).isLessThan(instructions.indexOf(instr3))
    }

    @Test
    fun `largest instruction is chosen in recursion`() {
        //     1
        //   /   \
        //  2   3 | 4
        //       \   \
        //        5   6
        val instrMatcher = mockk<InstructionMatcher>()
        val instrCovering = CacophonyInstructionCovering(instrMatcher)
        val root = mockk<CFGNode>()
        val node2 = mockk<CFGNode>()
        val node3 = mockk<CFGNode>()
        val node5 = mockk<CFGNode>()
        val node6 = mockk<CFGNode>()
        val instr1 = mockk<Instruction>()
        val match1 = Match({ _ -> listOf(instr1) }, mapOf(ValueLabel() to node2, ValueLabel() to node3), 1)
        every { instrMatcher.findMatchesForSideEffects(root) } returns setOf(match1)
        val instr2 = mockk<Instruction>()
        val match2 = Match({ _ -> listOf(instr2) }, emptyMap(), 1)
        every { instrMatcher.findMatchesForValue(node2, any()) } returns setOf(match2)
        val instr3 = mockk<Instruction>()
        val match3 = Match({ _ -> listOf(instr3) }, mapOf(ValueLabel() to node5), 2)
        val instr4 = mockk<Instruction>()
        val match4 = Match({ _ -> listOf(instr4) }, mapOf(ValueLabel() to node6), 1)
        every { instrMatcher.findMatchesForValue(node3, any()) } returns setOf(match3, match4)
        val instr5 = mockk<Instruction>()
        val match5 = Match({ _ -> listOf(instr5) }, emptyMap(), 1)
        every { instrMatcher.findMatchesForValue(node5, any()) } returns setOf(match5)
        val instr6 = mockk<Instruction>()
        val match6 = Match({ _ -> listOf(instr6) }, emptyMap(), 1)
        every { instrMatcher.findMatchesForValue(node6, any()) } returns setOf(match6)

        val instructions = instrCovering.coverWithInstructions(root)
        assertThat(instructions).containsExactlyInAnyOrder(instr1, instr2, instr3, instr5)
        assertThat(instructions.indexOf(instr2)).isLessThan(instructions.indexOf(instr1))
        assertThat(instructions.indexOf(instr3)).isLessThan(instructions.indexOf(instr1))
        assertThat(instructions.indexOf(instr5)).isLessThan(instructions.indexOf(instr3))
    }
}
