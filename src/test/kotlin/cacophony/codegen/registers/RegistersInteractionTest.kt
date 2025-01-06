package cacophony.codegen.registers

import cacophony.codegen.instructions.CopyInstruction
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.linearization.BasicBlock
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegistersInteractionTest {
    private fun mockInstruction(def: Set<Register>, use: Set<Register>): Instruction {
        val instruction = mockk<Instruction>()
        every { instruction.registersRead } returns use
        every { instruction.registersWritten } returns def
        return instruction
    }

    private fun mockCopyInstruction(def: Set<Register>, use: Set<Register>): CopyInstruction {
        val instruction = mockk<CopyInstruction>()
        every { instruction.registersRead } returns use
        every { instruction.registersWritten } returns def
        every { instruction.copyInto() } returns def.first()
        every { instruction.copyFrom() } returns use.first()
        return instruction
    }

    private fun mockBlock(instructions: List<Instruction>, successors: Set<BasicBlock>): BasicBlock {
        val block = mockk<BasicBlock>()
        every { block.instructions() } returns instructions
        every { block.successors() } returns successors
        return block
    }

    //  -------------------------------------------->
    //             2: [def B, use B/def C]
    //            /                       \
    //  1: [def A]                         4: [use A]
    //            \                       /
    //             3: [def D] -----------/
    @Test
    fun `properly calculates interference without copy instructions`() {
        // given
        val regA = Register.VirtualRegister()
        val regB = Register.VirtualRegister()
        val regC = Register.VirtualRegister()
        val regD = Register.VirtualRegister()

        val block4 =
            mockBlock(
                listOf(mockInstruction(setOf(), setOf(regA))),
                setOf(),
            )
        val block3 =
            mockBlock(
                listOf(mockInstruction(setOf(regD), setOf())),
                setOf(block4),
            )
        val block2 =
            mockBlock(
                listOf(
                    mockInstruction(setOf(regB), setOf()),
                    mockInstruction(setOf(regC), setOf(regB)),
                ),
                setOf(block4),
            )
        val block1 =
            mockBlock(
                listOf(
                    mockInstruction(setOf(regA), setOf()),
                ),
                setOf(block2, block3),
            )

        every { block4.predecessors() } returns setOf(block2, block3)
        every { block3.predecessors() } returns setOf(block1)
        every { block2.predecessors() } returns setOf(block1)
        every { block1.predecessors() } returns setOf()

        // when
        val registersInteraction = analyzeRegistersInteraction(listOf(block1, block2, block3, block4), emptyList())

        // then
        assertThat(registersInteraction.allRegisters).containsExactlyInAnyOrder(regA, regB, regC, regD)
        assertThat(registersInteraction.interference).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(regB, regC, regD),
                regB to setOf(regA),
                regC to setOf(regA),
                regD to setOf(regA),
            ),
        )
        assertThat(registersInteraction.copying).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(),
                regB to setOf(),
                regC to setOf(),
                regD to setOf(),
            ),
        )
    }

    @Test
    fun `calculates interference between non-references and references`() {
        // given
        val refReg = Register.VirtualRegister(true)
        val nonRefReg = Register.VirtualRegister(false)

        val preservedHardwareReg = HardwareRegister.R12
        val preservedReg = Register.FixedRegister(preservedHardwareReg)

        val block = mockBlock(
            listOf(
                mockInstruction(setOf(refReg), emptySet()),
                mockInstruction(setOf(nonRefReg), emptySet()),
            ),
            setOf(),
        )

        every { block.predecessors() } returns emptySet()

        // when
        val registersInteraction = analyzeRegistersInteraction(listOf(block), listOf(preservedHardwareReg))

        // then
        assertThat(registersInteraction.allRegisters).containsExactlyInAnyOrder(refReg, nonRefReg, preservedReg)
        assertThat(registersInteraction.interference).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                refReg to setOf(preservedReg),
                nonRefReg to emptySet(),
                preservedReg to setOf(refReg),
            ),
        )
    }

    //  1: [def A] --> 2(copy): [def B/use A] --> 3: [use A]
    @Test
    fun `properly founds copy`() {
        // given
        val regA = Register.VirtualRegister()
        val regB = Register.VirtualRegister()

        val block3 =
            mockBlock(
                listOf(mockInstruction(setOf(), setOf(regA))),
                setOf(),
            )
        val block2 =
            mockBlock(
                listOf(mockCopyInstruction(setOf(regB), setOf(regA))),
                setOf(block3),
            )

        val block1 =
            mockBlock(
                listOf(mockInstruction(setOf(regA), setOf())),
                setOf(block2),
            )

        every { block3.predecessors() } returns setOf(block2)
        every { block2.predecessors() } returns setOf(block1)
        every { block1.predecessors() } returns setOf()

        // when
        val registersInteraction = analyzeRegistersInteraction(listOf(block1, block2, block3), emptyList())

        // then
        assertThat(registersInteraction.allRegisters).containsExactlyInAnyOrder(regA, regB)
        assertThat(registersInteraction.interference).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(),
                regB to setOf(),
            ),
        )
        assertThat(registersInteraction.copying).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(regB),
                regB to setOf(),
            ),
        )
    }

    //  1: [def A] --> 2(copy): [def B/use A] --> 3: [def B/useA]
    @Test
    fun `recognizes that copy is not created if copyInto is defined more than once`() {
        // given
        val regA = Register.VirtualRegister()
        val regB = Register.VirtualRegister()

        val block3 =
            mockBlock(
                listOf(mockInstruction(setOf(regB), setOf(regA))),
                setOf(),
            )
        val block2 =
            mockBlock(
                listOf(mockCopyInstruction(setOf(regB), setOf(regA))),
                setOf(block3),
            )

        val block1 =
            mockBlock(
                listOf(mockInstruction(setOf(regA), setOf())),
                setOf(block2),
            )

        every { block3.predecessors() } returns setOf(block2)
        every { block2.predecessors() } returns setOf(block1)
        every { block1.predecessors() } returns setOf()

        // when
        val registersInteraction = analyzeRegistersInteraction(listOf(block1, block2, block3), emptyList())

        // then
        assertThat(registersInteraction.allRegisters).containsExactlyInAnyOrder(regA, regB)
        assertThat(registersInteraction.interference).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(regB),
                regB to setOf(regA),
            ),
        )
        assertThat(registersInteraction.copying).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(),
                regB to setOf(),
            ),
        )
    }

    //  1: [def A] --> 2(copy): [def B/use A] --> 3: [def A] --> 4: [use B]
    @Test
    fun `recognizes that copy is not created if copyFrom is defined again in lifetime of copyInto`() {
        // given
        val regA = Register.VirtualRegister()
        val regB = Register.VirtualRegister()

        val block4 =
            mockBlock(
                listOf(mockInstruction(setOf(), setOf(regB))),
                setOf(),
            )
        val block3 =
            mockBlock(
                listOf(mockInstruction(setOf(regA), setOf())),
                setOf(block4),
            )
        val block2 =
            mockBlock(
                listOf(mockCopyInstruction(setOf(regB), setOf(regA))),
                setOf(block3),
            )

        val block1 =
            mockBlock(
                listOf(mockInstruction(setOf(regA), setOf())),
                setOf(block2),
            )

        every { block4.predecessors() } returns setOf(block3)
        every { block3.predecessors() } returns setOf(block2)
        every { block2.predecessors() } returns setOf(block1)
        every { block1.predecessors() } returns setOf()

        // when
        val registersInteraction = analyzeRegistersInteraction(listOf(block1, block2, block3, block4), emptyList())

        // then
        assertThat(registersInteraction.allRegisters).containsExactlyInAnyOrder(regA, regB)
        assertThat(registersInteraction.interference).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(regB),
                regB to setOf(regA),
            ),
        )
        assertThat(registersInteraction.copying).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(),
                regB to setOf(),
            ),
        )
    }

    @Test
    fun `throws if received LoweredCFGFragment with empty block`() {
        // given
        val lCfgFragment =
            listOf(
                mockBlock(listOf(), setOf()),
            )

        // when & then
        assertThrows<IllegalArgumentException> {
            analyzeRegistersInteraction(lCfgFragment, emptyList())
        }
    }
}
