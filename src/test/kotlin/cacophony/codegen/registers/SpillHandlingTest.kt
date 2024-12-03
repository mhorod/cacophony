package cacophony.codegen.registers

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.InstructionCovering
import cacophony.codegen.linearization.BasicBlock
import cacophony.controlflow.CFGNode
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.controlflow.functions.FunctionHandler
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SpillHandlingTest {
    private fun mockInstruction(def: Set<Register>, use: Set<Register>): Instruction {
        val instruction = mockk<Instruction>()
        every { instruction.registersRead } returns use
        every { instruction.registersWritten } returns def
        return instruction
    }

    private fun mockBlock(instructions: List<Instruction>): BasicBlock {
        val block = mockk<BasicBlock>()
        every { block.instructions() } returns instructions
        return block
    }

    @Test
    fun `generates proper prologue for instructions reading spilled registers`() {
        // given
        val spareReg1 = Register.FixedRegister(HardwareRegister.RAX)
        val spareReg2 = Register.FixedRegister(HardwareRegister.RBX)
        val spilledRegA = Register.VirtualRegister()
        val spilledRegB = Register.VirtualRegister()

        val spillSlotA = mockk<CFGNode.LValue>()
        val spillSlotB = mockk<CFGNode.LValue>()
        val functionHandler = mockk<FunctionHandler>()
        every { functionHandler.allocateFrameVariable(any()) } returns spillSlotA andThen spillSlotB

        val prologueInstructionA = mockk<Instruction>()
        val prologueInstructionB = mockk<Instruction>()
        var capturedNode1: CFGNode? = null
        var capturedNode2: CFGNode? = null
        val capture = slot<CFGNode>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructions(capture(capture)) } answers {
            capturedNode1 = capture.captured
            listOf(prologueInstructionA)
        } andThenAnswer {
            capturedNode2 = capture.captured
            listOf(prologueInstructionB)
        }

        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledRegA, spilledRegB))

        val instruction = mockInstruction(setOf(), setOf(spilledRegA, spilledRegB))
        val instructionWithSubRegisters = mockInstruction(setOf(), setOf(spareReg1, spareReg2))
        every { instruction.substituteRegisters(any()) } returns instructionWithSubRegisters

        val block = mockBlock(listOf(instruction))

        // when
        val adjustedLoweredCFG =
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                functionHandler,
                listOf(block),
                registerAllocation,
                setOf(spareReg1, spareReg2),
            )

        // then
        // assert mocks were called with proper params
        assert(
            capturedNode1 != null &&
                capturedNode2 != null &&
                capturedNode1 is CFGNode.Assignment &&
                capturedNode2 is CFGNode.Assignment,
        )
        val dest1 = (capturedNode1 as CFGNode.Assignment).destination
        val dest2 = (capturedNode2 as CFGNode.Assignment).destination
        val val1 = (capturedNode1 as CFGNode.Assignment).value
        val val2 = (capturedNode2 as CFGNode.Assignment).value

        assertThat(listOf(dest1, dest2)).containsExactlyInAnyOrder(CFGNode.RegisterUse(spareReg1), CFGNode.RegisterUse(spareReg2))
        assertThat(listOf(val1, val2)).containsExactlyInAnyOrder(spillSlotA, spillSlotB)

        // assert the result is correct
        assertThat(adjustedLoweredCFG).hasSize(1)
        val newInstructions = adjustedLoweredCFG[0].instructions()
        assertThat(newInstructions).hasSize(3)
        assertThat(newInstructions[2]).isEqualTo(instructionWithSubRegisters)
        assertThat(listOf(newInstructions[0], newInstructions[1])).containsExactlyInAnyOrder(
            prologueInstructionA,
            prologueInstructionB,
        )
    }

    @Test
    fun `generates proper epilogue for instruction writing spilled registers`() {
        // given
        val spareReg1 = Register.FixedRegister(HardwareRegister.RAX)
        val spareReg2 = Register.FixedRegister(HardwareRegister.RBX)
        val spilledRegA = Register.VirtualRegister()
        val spilledRegB = Register.VirtualRegister()

        val spillSlotA = mockk<CFGNode.LValue>()
        val spillSlotB = mockk<CFGNode.LValue>()
        val functionHandler = mockk<FunctionHandler>()
        every { functionHandler.allocateFrameVariable(any()) } returns spillSlotA andThen spillSlotB

        val epilogueInstructionA = mockk<Instruction>()
        val epilogueInstructionB = mockk<Instruction>()
        var capturedNode1: CFGNode? = null
        var capturedNode2: CFGNode? = null
        val capture = slot<CFGNode>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructions(capture(capture)) } answers {
            capturedNode1 = capture.captured
            listOf(epilogueInstructionA)
        } andThenAnswer {
            capturedNode2 = capture.captured
            listOf(epilogueInstructionB)
        }

        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledRegA, spilledRegB))

        val instruction = mockInstruction(setOf(spilledRegA, spilledRegB), setOf())
        val instructionWithSubRegisters = mockInstruction(setOf(spareReg1, spareReg2), setOf())
        every { instruction.substituteRegisters(any()) } returns instructionWithSubRegisters

        val block = mockBlock(listOf(instruction))

        // when
        val adjustedLoweredCFG =
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                functionHandler,
                listOf(block),
                registerAllocation,
                setOf(spareReg1, spareReg2),
            )

        // then
        // assert mocks were called with proper params
        assert(
            capturedNode1 != null &&
                capturedNode2 != null &&
                capturedNode1 is CFGNode.Assignment &&
                capturedNode2 is CFGNode.Assignment,
        )
        val dest1 = (capturedNode1 as CFGNode.Assignment).destination
        val dest2 = (capturedNode2 as CFGNode.Assignment).destination
        val val1 = (capturedNode1 as CFGNode.Assignment).value
        val val2 = (capturedNode2 as CFGNode.Assignment).value

        assertThat(listOf(dest1, dest2)).containsExactlyInAnyOrder(spillSlotA, spillSlotB)
        assertThat(listOf(val1, val2)).containsExactlyInAnyOrder(CFGNode.RegisterUse(spareReg1), CFGNode.RegisterUse(spareReg2))

        // assert the result is correct
        assertThat(adjustedLoweredCFG).hasSize(1)
        val newInstructions = adjustedLoweredCFG[0].instructions()
        assertThat(newInstructions).hasSize(3)
        assertThat(newInstructions[0]).isEqualTo(instructionWithSubRegisters)
        assertThat(listOf(newInstructions[1], newInstructions[2])).containsExactlyInAnyOrder(
            epilogueInstructionA,
            epilogueInstructionB,
        )
    }

    @Test
    fun `generates proper prologue and epilogue for instructions reading and writing spilled registers`() {
        // given
        val spareReg1 = Register.FixedRegister(HardwareRegister.RAX)
        val spareReg2 = Register.FixedRegister(HardwareRegister.RBX)
        val spilledRegA = Register.VirtualRegister()
        val spilledRegB = Register.VirtualRegister()

        val spillSlotA = mockk<CFGNode.LValue>()
        val spillSlotB = mockk<CFGNode.LValue>()
        val functionHandler = mockk<FunctionHandler>()
        every { functionHandler.allocateFrameVariable(any()) } returns spillSlotA andThen spillSlotB

        val prologueInstruction = mockk<Instruction>()
        val epilogueInstruction = mockk<Instruction>()
        var capturedNode1: CFGNode? = null
        var capturedNode2: CFGNode? = null
        val capture = slot<CFGNode>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructions(capture(capture)) } answers {
            capturedNode1 = capture.captured
            listOf(prologueInstruction)
        } andThenAnswer {
            capturedNode2 = capture.captured
            listOf(epilogueInstruction)
        }

        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledRegA, spilledRegB))

        val instruction = mockInstruction(setOf(spilledRegA), setOf(spilledRegB))
        val instructionWithSubRegisters = mockInstruction(setOf(), setOf())
        every { instruction.substituteRegisters(any()) } returns instructionWithSubRegisters

        val block = mockBlock(listOf(instruction))

        // when
        val adjustedLoweredCFG =
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                functionHandler,
                listOf(block),
                registerAllocation,
                setOf(spareReg1, spareReg2),
            )

        // then
        // assert mocks were called with proper params
        assert(
            capturedNode1 != null &&
                capturedNode2 != null &&
                capturedNode1 is CFGNode.Assignment &&
                capturedNode2 is CFGNode.Assignment,
        )
        val dest1 = (capturedNode1 as CFGNode.Assignment).destination
        val dest2 = (capturedNode2 as CFGNode.Assignment).destination
        val val1 = (capturedNode1 as CFGNode.Assignment).value
        val val2 = (capturedNode2 as CFGNode.Assignment).value

        assertThat(listOf(val1, val2, dest1, dest2)).containsExactlyInAnyOrder(
            spillSlotA,
            spillSlotB,
            CFGNode.RegisterUse(spareReg1),
            CFGNode.RegisterUse(spareReg2),
        )
        assertThat(val1 == spillSlotB || val2 == spillSlotB)
        assertThat(dest1 == spillSlotA || dest2 == spillSlotA)

        // assert the result is correct
        assertThat(adjustedLoweredCFG).hasSize(1)
        val newInstructions = adjustedLoweredCFG[0].instructions()
        assertThat(newInstructions).hasSize(3)
        assertThat(newInstructions[0]).isEqualTo(prologueInstruction)
        assertThat(newInstructions[1]).isEqualTo(instructionWithSubRegisters)
        assertThat(newInstructions[2]).isEqualTo(epilogueInstruction)
    }

    @Test
    fun `modifies all basic blocks with spilled registers`() {
        // given
        val spareReg = Register.FixedRegister(HardwareRegister.RAX)
        val spilledReg = Register.VirtualRegister()

        val spillSlot = mockk<CFGNode.LValue>()
        val functionHandler = mockk<FunctionHandler>()
        every { functionHandler.allocateFrameVariable(any()) } returns spillSlot

        val prologueInstruction = mockk<Instruction>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructions(any()) } returns listOf(prologueInstruction)

        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledReg))

        val instruction = mockInstruction(setOf(), setOf(spilledReg))
        val instructionWithSubRegisters = mockInstruction(setOf(), setOf(spareReg))
        every { instruction.substituteRegisters(any()) } returns instructionWithSubRegisters

        val block = mockBlock(listOf(instruction))

        // when
        val adjustedLoweredCFG =
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                functionHandler,
                listOf(block, block),
                registerAllocation,
                setOf(spareReg),
            )

        // then
        assertThat(adjustedLoweredCFG).hasSize(2)
        assertThat(adjustedLoweredCFG[0].instructions()).isEqualTo(
            adjustedLoweredCFG[1].instructions(),
        )
    }

    @Test
    fun `leaves basic blocks not using spilled registers unchanged`() {
        // given
        val spareReg = Register.FixedRegister(HardwareRegister.RAX)
        val spilledReg = Register.VirtualRegister()

        val spillSlot = mockk<CFGNode.LValue>()
        val functionHandler = mockk<FunctionHandler>()
        every { functionHandler.allocateFrameVariable(any()) } returns spillSlot

        val instructionCovering = mockk<InstructionCovering>()
        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledReg))

        val instruction = mockInstruction(setOf(), setOf())
        every { instruction.substituteRegisters(any()) } returns instruction

        val block = mockBlock(listOf(instruction))

        // when
        val adjustedLoweredCFG =
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                functionHandler,
                listOf(block),
                registerAllocation,
                setOf(spareReg),
            )

        // then
        assertThat(adjustedLoweredCFG).hasSize(1)
        assertThat(adjustedLoweredCFG[0].instructions()).isEqualTo(listOf(instruction))
    }

    @Test
    fun `throws if spare register is used in provided register allocation`() {
        // given
        val spareReg = Register.FixedRegister(HardwareRegister.RAX)
        val functionHandler = mockk<FunctionHandler>()
        val instructionCovering = mockk<InstructionCovering>()
        val registerAllocation = RegisterAllocation(mapOf(Register.VirtualRegister() to HardwareRegister.RAX), setOf())
        val instruction = mockInstruction(setOf(), setOf())
        val block = mockBlock(listOf(instruction))

        // when & then
        assertThrows<SpillHandlingException> {
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                functionHandler,
                listOf(block),
                registerAllocation,
                setOf(spareReg),
            )
        }
    }

    @Test
    fun `throws if fixed register has spilled in register allocation`() {
        // given
        val spareReg = Register.FixedRegister(HardwareRegister.RAX)
        val functionHandler = mockk<FunctionHandler>()
        val instructionCovering = mockk<InstructionCovering>()
        val registerAllocation = RegisterAllocation(mapOf(), setOf(spareReg))
        val instruction = mockInstruction(setOf(), setOf())
        val block = mockBlock(listOf(instruction))

        // when & then
        assertThrows<SpillHandlingException> {
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                functionHandler,
                listOf(block),
                registerAllocation,
                setOf(spareReg),
            )
        }
    }

    @Test
    fun `throws if encountered instruction with spills using one of spare registers`() {
        // given
        val spareReg = Register.FixedRegister(HardwareRegister.RAX)
        val spilledReg = Register.VirtualRegister()

        val spillSlot = mockk<CFGNode.LValue>()
        val functionHandler = mockk<FunctionHandler>()
        every { functionHandler.allocateFrameVariable(any()) } returns spillSlot

        val instructionCovering = mockk<InstructionCovering>()
        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledReg))
        val instruction = mockInstruction(setOf(Register.FixedRegister(HardwareRegister.RAX)), setOf(spilledReg))
        val block = mockBlock(listOf(instruction))

        // when & then
        assertThrows<SpillHandlingException> {
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                functionHandler,
                listOf(block),
                registerAllocation,
                setOf(spareReg),
            )
        }
    }

    @Test
    fun `throws if not enough spare registers have been provided`() {
        // given
        val spareReg = Register.FixedRegister(HardwareRegister.RAX)
        val spilledRegA = Register.VirtualRegister()
        val spilledRegB = Register.VirtualRegister()

        val spillSlotA = mockk<CFGNode.LValue>()
        val spillSlotB = mockk<CFGNode.LValue>()
        val functionHandler = mockk<FunctionHandler>()
        every { functionHandler.allocateFrameVariable(any()) } returns spillSlotA andThen spillSlotB

        val prologueInstructionA = mockk<Instruction>()
        val prologueInstructionB = mockk<Instruction>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructions(any()) } returns listOf(prologueInstructionA) andThen
            listOf(prologueInstructionB)

        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledRegA, spilledRegB))

        val instruction = mockInstruction(setOf(), setOf(spilledRegA, spilledRegB))
        val instructionWithSubRegisters = mockInstruction(setOf(), setOf(spareReg))
        every { instruction.substituteRegisters(any()) } returns instructionWithSubRegisters

        val block = mockBlock(listOf(instruction))

        // when & then
        assertThrows<SpillHandlingException> {
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                functionHandler,
                listOf(block),
                registerAllocation,
                setOf(spareReg),
            )
        }
    }
}
