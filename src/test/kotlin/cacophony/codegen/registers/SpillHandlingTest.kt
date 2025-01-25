package cacophony.codegen.registers

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.InstructionCovering
import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.controlflow.CFGNode
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.controlflow.functions.StaticFunctionHandler
import cacophony.graphs.GraphColoring
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SpillHandlingTest {
    private val mockRegistersInteraction = RegistersInteraction(setOf(), mapOf(), mapOf())
    private val mockGraphColoring = mockk<GraphColoring<Register.VirtualRegister, Int>>()

    init {
        val graphCapture = slot<Map<Register.VirtualRegister, Set<Register.VirtualRegister>>>()
        val allowedColorsCapture = slot<Set<Int>>()
        every { mockGraphColoring.doColor(capture(graphCapture), any(), any(), capture(allowedColorsCapture)) } answers {
            graphCapture.captured.keys
                .zip(allowedColorsCapture.captured)
                .toMap()
        }
    }

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
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        every { staticFunctionHandler.allocateFrameVariable(any()) } returns spillSlotA andThen spillSlotB

        val prologueInstructionA = mockk<Instruction>()
        val prologueInstructionB = mockk<Instruction>()
        var capturedNode1: CFGNode? = null
        var capturedNode2: CFGNode? = null
        val capture = slot<CFGNode>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructionsWithoutTemporaryRegisters(capture(capture)) } answers {
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
                staticFunctionHandler,
                listOf(block),
                mockRegistersInteraction,
                registerAllocation,
                setOf(spareReg1, spareReg2),
                mockGraphColoring,
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
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        every { staticFunctionHandler.allocateFrameVariable(any()) } returns spillSlotA andThen spillSlotB

        val epilogueInstructionA = mockk<Instruction>()
        val epilogueInstructionB = mockk<Instruction>()
        var capturedNode1: CFGNode? = null
        var capturedNode2: CFGNode? = null
        val capture = slot<CFGNode>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructionsWithoutTemporaryRegisters(capture(capture)) } answers {
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
                staticFunctionHandler,
                listOf(block),
                mockRegistersInteraction,
                registerAllocation,
                setOf(spareReg1, spareReg2),
                mockGraphColoring,
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
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        every { staticFunctionHandler.allocateFrameVariable(any()) } returns spillSlotA andThen spillSlotB

        val prologueInstruction = mockk<Instruction>()
        val epilogueInstruction = mockk<Instruction>()
        var capturedNode1: CFGNode? = null
        var capturedNode2: CFGNode? = null
        val capture = slot<CFGNode>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructionsWithoutTemporaryRegisters(capture(capture)) } answers {
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
                staticFunctionHandler,
                listOf(block),
                mockRegistersInteraction,
                registerAllocation,
                setOf(spareReg1, spareReg2),
                mockGraphColoring,
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
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        every { staticFunctionHandler.allocateFrameVariable(any()) } returns spillSlot

        val prologueInstruction = mockk<Instruction>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructionsWithoutTemporaryRegisters(any()) } returns listOf(prologueInstruction)

        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledReg))

        val instruction = mockInstruction(setOf(), setOf(spilledReg))
        val instructionWithSubRegisters = mockInstruction(setOf(), setOf(spareReg))
        every { instruction.substituteRegisters(any()) } returns instructionWithSubRegisters

        val block = mockBlock(listOf(instruction))

        // when
        val adjustedLoweredCFG =
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                staticFunctionHandler,
                listOf(block, block),
                mockRegistersInteraction,
                registerAllocation,
                setOf(spareReg),
                mockGraphColoring,
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
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        every { staticFunctionHandler.allocateFrameVariable(any()) } returns spillSlot

        val instructionCovering = mockk<InstructionCovering>()
        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledReg))

        val instruction = mockInstruction(setOf(), setOf())
        every { instruction.substituteRegisters(any()) } returns instruction

        val block = mockBlock(listOf(instruction))

        // when
        val adjustedLoweredCFG =
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                staticFunctionHandler,
                listOf(block),
                mockRegistersInteraction,
                registerAllocation,
                setOf(spareReg),
                mockGraphColoring,
            )

        // then
        assertThat(adjustedLoweredCFG).hasSize(1)
        assertThat(adjustedLoweredCFG[0].instructions()).isEqualTo(listOf(instruction))
    }

    @Test
    fun `properly handles spill in instruction using spare register if there are still enough available spare registers`() {
        // given
        val spareReg1 = Register.FixedRegister(HardwareRegister.RAX)
        val spareReg2 = Register.FixedRegister(HardwareRegister.RBX)
        val spilledReg = Register.VirtualRegister()

        val spillSlot = mockk<CFGNode.LValue>()
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        every { staticFunctionHandler.allocateFrameVariable(any()) } returns spillSlot

        val prologueInstruction = mockk<Instruction>()
        var capturedNode: CFGNode? = null
        val capture = slot<CFGNode>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructionsWithoutTemporaryRegisters(capture(capture)) } answers {
            capturedNode = capture.captured
            listOf(prologueInstruction)
        }

        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledReg))

        val instruction = mockInstruction(setOf(Register.FixedRegister(HardwareRegister.RAX)), setOf(spilledReg))
        val instructionWithSubRegisters = mockInstruction(setOf(), setOf(spareReg1, spareReg2))
        every { instruction.substituteRegisters(any()) } returns instructionWithSubRegisters

        val block = mockBlock(listOf(instruction))

        // when
        val adjustedLoweredCFG =
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                staticFunctionHandler,
                listOf(block),
                mockRegistersInteraction,
                registerAllocation,
                setOf(spareReg1, spareReg2),
                mockGraphColoring,
            )

        // then
        // assert mocks were called with proper params
        assert(capturedNode != null && capturedNode is CFGNode.Assignment)
        val dest = (capturedNode as CFGNode.Assignment).destination
        val value = (capturedNode as CFGNode.Assignment).value

        assertThat(dest).isEqualTo(CFGNode.RegisterUse(spareReg2))
        assertThat(value).isEqualTo(spillSlot)

        // assert the result is correct
        assertThat(adjustedLoweredCFG).hasSize(1)
        val newInstructions = adjustedLoweredCFG[0].instructions()
        assertThat(newInstructions).hasSize(2)
        assertThat(newInstructions[0]).isEqualTo(prologueInstruction)
        assertThat(newInstructions[1]).isEqualTo(instructionWithSubRegisters)
    }

    @Test
    fun `throws if spare register is used in provided register allocation`() {
        // given
        val spareReg = Register.FixedRegister(HardwareRegister.RAX)
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        val instructionCovering = mockk<InstructionCovering>()
        val registerAllocation = RegisterAllocation(mapOf(Register.VirtualRegister() to HardwareRegister.RAX), setOf())
        val instruction = mockInstruction(setOf(), setOf())
        val block = mockBlock(listOf(instruction))

        // when & then
        assertThrows<SpillHandlingException> {
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                staticFunctionHandler,
                listOf(block),
                mockRegistersInteraction,
                registerAllocation,
                setOf(spareReg),
                mockGraphColoring,
            )
        }
    }

    @Test
    fun `throws if fixed register has spilled in register allocation`() {
        // given
        val spareReg = Register.FixedRegister(HardwareRegister.RAX)
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        val instructionCovering = mockk<InstructionCovering>()
        val registerAllocation = RegisterAllocation(mapOf(), setOf(spareReg))
        val instruction = mockInstruction(setOf(), setOf())
        val block = mockBlock(listOf(instruction))

        // when & then
        assertThrows<SpillHandlingException> {
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                staticFunctionHandler,
                listOf(block),
                mockRegistersInteraction,
                registerAllocation,
                setOf(spareReg),
                mockGraphColoring,
            )
        }
    }

    @Test
    fun `throws if there is instruction with more spills than spare registers`() {
        // given
        val spareReg = Register.FixedRegister(HardwareRegister.RAX)
        val spilledRegA = Register.VirtualRegister()
        val spilledRegB = Register.VirtualRegister()

        val spillSlotA = mockk<CFGNode.LValue>()
        val spillSlotB = mockk<CFGNode.LValue>()
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        every { staticFunctionHandler.allocateFrameVariable(any()) } returns spillSlotA andThen spillSlotB

        val prologueInstructionA = mockk<Instruction>()
        val prologueInstructionB = mockk<Instruction>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructionsWithoutTemporaryRegisters(any()) } returns listOf(prologueInstructionA) andThen
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
                staticFunctionHandler,
                listOf(block),
                mockRegistersInteraction,
                registerAllocation,
                setOf(spareReg),
                mockGraphColoring,
            )
        }
    }

    @Test
    fun `throws if encountered instruction with more spills than spare registers it is not using`() {
        // given
        val spareReg = Register.FixedRegister(HardwareRegister.RAX)
        val spilledReg = Register.VirtualRegister()

        val spillSlot = mockk<CFGNode.LValue>()
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        every { staticFunctionHandler.allocateFrameVariable(any()) } returns spillSlot

        val instructionCovering = mockk<InstructionCovering>()
        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledReg))
        val instruction = mockInstruction(setOf(Register.FixedRegister(HardwareRegister.RAX)), setOf(spilledReg))
        val block = mockBlock(listOf(instruction))

        // when & then
        assertThrows<SpillHandlingException> {
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                staticFunctionHandler,
                listOf(block),
                mockRegistersInteraction,
                registerAllocation,
                setOf(spareReg),
                mockGraphColoring,
            )
        }
    }

    @Test
    fun `allocates the same frame memory to spills colored with the same color`() {
        // given
        val spareRegA = Register.FixedRegister(HardwareRegister.RAX)
        val spareRegB = Register.FixedRegister(HardwareRegister.RBX)
        val spilledRegA = Register.VirtualRegister()
        val spilledRegB = Register.VirtualRegister()
        val spilledRegC = Register.VirtualRegister()
        val nonSpilledReg = Register.VirtualRegister()

        val spillSlot1 = mockk<CFGNode.LValue>()
        val spillSlot2 = mockk<CFGNode.LValue>()
        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        every { staticFunctionHandler.allocateFrameVariable(any()) } returns spillSlot1 andThen spillSlot2 andThen mockk<CFGNode.LValue>()

        val prologueInstructionA = mockk<Instruction>()
        val prologueInstructionB = mockk<Instruction>()
        val prologueInstructionC = mockk<Instruction>()

        var capturedNode1: CFGNode? = null
        var capturedNode2: CFGNode? = null
        var capturedNode3: CFGNode? = null
        val capture = slot<CFGNode>()
        val instructionCovering = mockk<InstructionCovering>()
        every { instructionCovering.coverWithInstructionsWithoutTemporaryRegisters(capture(capture)) } answers {
            capturedNode1 = capture.captured
            listOf(prologueInstructionA)
        } andThenAnswer {
            capturedNode2 = capture.captured
            listOf(prologueInstructionB)
        } andThenAnswer {
            capturedNode3 = capture.captured
            listOf(prologueInstructionC)
        }

        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledRegA, spilledRegB, spilledRegC))

        val instructionA = mockInstruction(setOf(), setOf(spilledRegA, spilledRegC))
        val instructionB = mockInstruction(setOf(), setOf(spilledRegB, spilledRegC))
        val instructionAWithSubRegisters = mockInstruction(setOf(), setOf(spareRegA, spareRegB))
        val instructionBWithSubRegisters = mockInstruction(setOf(), setOf(spareRegA, spareRegB))
        every { instructionA.substituteRegisters(any()) } returns instructionAWithSubRegisters
        every { instructionB.substituteRegisters(any()) } returns instructionBWithSubRegisters

        val registersInteraction =
            RegistersInteraction(
                setOf(spilledRegA, spilledRegB, spilledRegC, nonSpilledReg, spareRegA, spareRegB),
                mapOf(
                    spilledRegA to setOf(spilledRegC, nonSpilledReg),
                    spilledRegB to setOf(spilledRegC),
                    spilledRegC to setOf(spilledRegA, spilledRegB),
                    nonSpilledReg to setOf(spilledRegA),
                ),
                mapOf(),
            )

        val graphColoring = mockk<GraphColoring<Register.VirtualRegister, Int>>()
        every {
            graphColoring.doColor(
                mapOf(
                    spilledRegA to setOf(spilledRegC),
                    spilledRegB to setOf(spilledRegC),
                    spilledRegC to setOf(spilledRegA, spilledRegB),
                ),
                mapOf(
                    spilledRegA to setOf(),
                    spilledRegB to setOf(),
                    spilledRegC to setOf(),
                ),
                mapOf(),
                setOf(0, 1, 2),
            )
        }.returns(mapOf(spilledRegA to 0, spilledRegB to 0, spilledRegC to 1))

        val block = mockBlock(listOf(instructionA, instructionB))

        // when
        adjustLoweredCFGToHandleSpills(
            instructionCovering,
            staticFunctionHandler,
            listOf(block),
            registersInteraction,
            registerAllocation,
            setOf(spareRegA, spareRegB),
            graphColoring,
        )

        // then
        // assert mocks were called with proper params
        assert(
            capturedNode1 != null &&
                capturedNode2 != null &&
                capturedNode3 != null &&
                capturedNode1 is CFGNode.Assignment &&
                capturedNode2 is CFGNode.Assignment &&
                capturedNode3 is CFGNode.Assignment,
        )
        val dest1 = (capturedNode1 as CFGNode.Assignment).destination
        val dest2 = (capturedNode2 as CFGNode.Assignment).destination
        val dest3 = (capturedNode3 as CFGNode.Assignment).destination
        val val1 = (capturedNode1 as CFGNode.Assignment).value
        val val2 = (capturedNode2 as CFGNode.Assignment).value
        val val3 = (capturedNode3 as CFGNode.Assignment).value

        assertThat(setOf(dest1, dest2, dest3)).containsExactly(CFGNode.RegisterUse(spareRegA), CFGNode.RegisterUse(spareRegB))
        assertThat(setOf(val1, val2, val3)).containsExactly(spillSlot1, spillSlot2)

        verify(exactly = 2) { staticFunctionHandler.allocateFrameVariable(any()) }
    }

    @Test
    fun `throws if references and non-references share color`() {
        // given
        val spareRegA = Register.FixedRegister(HardwareRegister.RAX)
        val spareRegB = Register.FixedRegister(HardwareRegister.RBX)
        val spilledRegA = Register.VirtualRegister(true)
        val spilledRegB = Register.VirtualRegister(false)

        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        val instructionCovering = mockk<InstructionCovering>()
        val loweredCFGFragment = mockk<LoweredCFGFragment>()

        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledRegA, spilledRegB))

        val registersInteraction =
            RegistersInteraction(
                setOf(spilledRegA, spilledRegB, spareRegA, spareRegB),
                mapOf(
                    spilledRegA to emptySet(),
                    spilledRegB to emptySet(),
                ),
                mapOf(),
            )

        val graphColoring = mockk<GraphColoring<Register.VirtualRegister, Int>>()
        every {
            graphColoring.doColor(
                any(),
                any(),
                any(),
                any(),
            )
        }.returns(mapOf(spilledRegA to 0, spilledRegB to 0))

        // when & then
        assertThrows<SpillHandlingException> {
            adjustLoweredCFGToHandleSpills(
                instructionCovering,
                staticFunctionHandler,
                loweredCFGFragment,
                registersInteraction,
                registerAllocation,
                setOf(spareRegA, spareRegB),
                graphColoring,
            )
        }
    }

    @Test
    fun `properly allocates spills in functionHandler`() {
        // given
        val spareRegA = Register.FixedRegister(HardwareRegister.RAX)
        val spareRegB = Register.FixedRegister(HardwareRegister.RBX)
        val spilledWithRefA = Register.VirtualRegister(true)
        val spilledWithRefB = Register.VirtualRegister(true)
        val spilledWithNoRefA = Register.VirtualRegister(false)
        val spilledWithNoRefB = Register.VirtualRegister(false)

        val staticFunctionHandler = mockk<StaticFunctionHandler>()
        every { staticFunctionHandler.allocateFrameVariable(any()) } returns mockk<CFGNode.LValue>()

        val instructionCovering = mockk<InstructionCovering>()
        val loweredCFGFragment = listOf(mockBlock(emptyList()))

        val registerAllocation = RegisterAllocation(mapOf(), setOf(spilledWithRefA, spilledWithRefB, spilledWithNoRefA, spilledWithNoRefB))

        val registersInteraction =
            RegistersInteraction(
                setOf(spilledWithRefA, spilledWithRefB, spilledWithNoRefA, spilledWithNoRefB, spareRegA, spareRegB),
                mapOf(
                    spilledWithRefA to emptySet(),
                    spilledWithRefB to emptySet(),
                    spilledWithNoRefA to setOf(spilledWithNoRefB),
                    spilledWithNoRefB to setOf(spilledWithNoRefA),
                ),
                mapOf(),
            )

        val graphColoring = mockk<GraphColoring<Register.VirtualRegister, Int>>()
        every {
            graphColoring.doColor(
                any(),
                any(),
                any(),
                any(),
            )
        }.returns(mapOf(spilledWithRefA to 0, spilledWithRefB to 0, spilledWithNoRefA to 1, spilledWithNoRefB to 2))

        // when
        adjustLoweredCFGToHandleSpills(
            instructionCovering,
            staticFunctionHandler,
            loweredCFGFragment,
            registersInteraction,
            registerAllocation,
            setOf(spareRegA, spareRegB),
            graphColoring,
        )

        // then
        verify(exactly = 1) { staticFunctionHandler.allocateFrameVariable(match { it.holdsReference }) }
        verify(exactly = 2) { staticFunctionHandler.allocateFrameVariable(match { !it.holdsReference }) }
    }
}
