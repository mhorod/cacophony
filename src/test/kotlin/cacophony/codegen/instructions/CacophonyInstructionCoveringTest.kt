package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.matching.InstructionMatcher
import cacophony.codegen.instructions.matching.Match
import cacophony.codegen.patterns.Pattern
import cacophony.codegen.patterns.ValuePattern
import cacophony.controlflow.CFGNode
import cacophony.controlflow.ValueLabel
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CacophonyInstructionCoveringTest {
    private val mockedValuePattern: Pattern = mockk<ValuePattern>().apply { every { priority() } returns 0 }

    @Test
    fun `largest instruction is chosen`() {
        val instrMatcher = mockk<InstructionMatcher>()
        val instrCovering = CacophonyInstructionCovering(instrMatcher)
        val root = mockk<CFGNode.NoOp>()
        every { root.children() } returns emptyList()

        val instr1 = mockk<Instruction>()
        val match1 = Match({ _ -> listOf(instr1) }, emptyMap(), 1, mockedValuePattern)
        val instr2 = mockk<Instruction>()
        val match2 = Match({ _ -> listOf(instr2) }, emptyMap(), 2, mockedValuePattern)
        every { instrMatcher.findMatchesForSideEffects(root) } returns setOf(match1, match2)
        every { instrMatcher.findMatchesForValue(root, any()) } returns setOf(match1)

        assertThat(instrCovering.coverWithInstructions(root)).containsExactly(instr2)
    }

    @Test
    fun `largest instruction is chosen when jumping`() {
        val instrMatcher = mockk<InstructionMatcher>()
        val instrCovering = CacophonyInstructionCovering(instrMatcher)
        val root = mockk<CFGNode.NoOp>()
        every { root.children() } returns emptyList()

        val instr1 = mockk<Instruction>()
        val match1 = Match({ _ -> listOf(instr1) }, emptyMap(), 1, mockedValuePattern)
        val instr2 = mockk<Instruction>()
        val match2 = Match({ _ -> listOf(instr2) }, emptyMap(), 2, mockedValuePattern)
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
        val root = mockk<CFGNode.NoOp>()
        val node2 = mockk<CFGNode.NoOp>()
        val node3 = mockk<CFGNode.NoOp>()
        val node4 = mockk<CFGNode.NoOp>()
        every { root.children() } returns listOf(node2, node3)
        every { node2.children() } returns emptyList()
        every { node3.children() } returns listOf(node4)
        every { node4.children() } returns emptyList()

        val instr1 = mockk<Instruction>()
        val match1 = Match({ _ -> listOf(instr1) }, mapOf(ValueLabel() to node2, ValueLabel() to node3), 1, mockedValuePattern)
        every { instrMatcher.findMatchesForSideEffects(root) } returns setOf(match1)
        every { instrMatcher.findMatchesForValue(root, any()) } returns setOf(match1)

        val instr2 = mockk<Instruction>()
        val match2 = Match({ _ -> listOf(instr2) }, emptyMap(), 1, mockedValuePattern)
        every { instrMatcher.findMatchesForValue(node2, any()) } returns setOf(match2)
        val instr3 = mockk<Instruction>()
        val match3 = Match({ _ -> listOf(instr3) }, mapOf(ValueLabel() to node4), 1, mockedValuePattern)
        every { instrMatcher.findMatchesForValue(node3, any()) } returns setOf(match3)
        val instr4 = mockk<Instruction>()
        val match4 = Match({ _ -> listOf(instr4) }, emptyMap(), 1, mockedValuePattern)
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
        val root = mockk<CFGNode.NoOp>()
        val node2 = mockk<CFGNode.NoOp>()
        val node3 = mockk<CFGNode.NoOp>()
        val node5 = mockk<CFGNode.NoOp>()
        val node6 = mockk<CFGNode.NoOp>()
        every { root.children() } returns listOf(node2, node3)
        every { node2.children() } returns emptyList()
        every { node3.children() } returns listOf(node5, node6)
        every { node5.children() } returns emptyList()
        every { node6.children() } returns emptyList()

        val instr1 = mockk<Instruction>()
        val match1 = Match({ _ -> listOf(instr1) }, mapOf(ValueLabel() to node2, ValueLabel() to node3), 1, mockedValuePattern)
        every { instrMatcher.findMatchesForSideEffects(root) } returns setOf(match1)
        every { instrMatcher.findMatchesForValue(root, any()) } returns setOf(match1)

        val instr2 = mockk<Instruction>()
        val match2 = Match({ _ -> listOf(instr2) }, emptyMap(), 1, mockedValuePattern)
        every { instrMatcher.findMatchesForValue(node2, any()) } returns setOf(match2)
        val instr3 = mockk<Instruction>()
        val match3 = Match({ _ -> listOf(instr3) }, mapOf(ValueLabel() to node5), 2, mockedValuePattern)
        val instr4 = mockk<Instruction>()
        val match4 = Match({ _ -> listOf(instr4) }, mapOf(ValueLabel() to node6), 1, mockedValuePattern)
        every { instrMatcher.findMatchesForValue(node3, any()) } returns setOf(match3, match4)
        val instr5 = mockk<Instruction>()
        val match5 = Match({ _ -> listOf(instr5) }, emptyMap(), 1, mockedValuePattern)
        every { instrMatcher.findMatchesForValue(node5, any()) } returns setOf(match5)
        val instr6 = mockk<Instruction>()
        val match6 = Match({ _ -> listOf(instr6) }, emptyMap(), 1, mockedValuePattern)
        every { instrMatcher.findMatchesForValue(node6, any()) } returns setOf(match6)

        val instructions = instrCovering.coverWithInstructions(root)
        assertThat(instructions).containsExactlyInAnyOrder(instr1, instr2, instr3, instr5)
        assertThat(instructions.indexOf(instr2)).isLessThan(instructions.indexOf(instr1))
        assertThat(instructions.indexOf(instr3)).isLessThan(instructions.indexOf(instr1))
        assertThat(instructions.indexOf(instr5)).isLessThan(instructions.indexOf(instr3))
    }
}
