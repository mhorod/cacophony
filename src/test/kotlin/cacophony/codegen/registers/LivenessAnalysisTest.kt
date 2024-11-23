package cacophony.codegen.registers

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.CopyInstruction
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.linearization.BasicBlock
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LivenessAnalysisTest {
    private fun mockCopyInstruction(
        def: Set<Register>,
        use: Set<Register>,
    ): CopyInstruction =
        object : CopyInstruction {
            override val registersRead: Set<Register>
                get() = use
            override val registersWritten: Set<Register>
                get() = def

            override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String = "mock"
        }

    private fun mockInstruction(
        def: Set<Register>,
        use: Set<Register>,
    ): Instruction =
        object : Instruction {
            override val registersRead: Set<Register>
                get() = use
            override val registersWritten: Set<Register>
                get() = def

            override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String = "mock"
        }

    //  -------------------------------------------->
    //             2: [def B, use B/def C]
    //            /                       \
    //  1: [def A]                         4: [use A]
    //            \         --------------/
    //             3: [def D]
    @Test
    fun `properly calculates liveness without copy instructions`() {
        // given
        val regA = Register.VirtualRegister()
        val regB = Register.VirtualRegister()
        val regC = Register.VirtualRegister()
        val regD = Register.VirtualRegister()

        val block4 =
            BasicBlock(
                BlockLabel("4"),
                listOf(mockInstruction(setOf(), setOf(regA))),
                setOf(),
                mutableSetOf(),
            )
        val block3 =
            BasicBlock(
                BlockLabel("3"),
                listOf(mockInstruction(setOf(regD), setOf())),
                setOf(block4),
                mutableSetOf(),
            )
        val block2 =
            BasicBlock(
                BlockLabel("2"),
                listOf(
                    mockInstruction(setOf(regB), setOf()),
                    mockInstruction(setOf(regC), setOf(regB)),
                ),
                setOf(block4),
                mutableSetOf(),
            )
        val block1 =
            BasicBlock(
                BlockLabel("1"),
                listOf(
                    mockInstruction(setOf(regA), setOf()),
                ),
                setOf(block2, block3),
                mutableSetOf(),
            )

        block4.predecessors.addAll(setOf(block2, block3))
        block3.predecessors.add(block1)
        block2.predecessors.add(block1)

        // when
        val liveness = analyzeLiveness(listOf(block1, block2, block3, block4))

        // then
        assertThat(liveness.allRegisters).containsExactlyInAnyOrder(regA, regB, regC, regD)
        assertThat(liveness.interference).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(regB, regC, regD),
                regB to setOf(regA),
                regC to setOf(regA),
                regD to setOf(regA),
            ),
        )
        assertThat(liveness.copying).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(),
                regB to setOf(),
                regC to setOf(),
                regD to setOf(),
            ),
        )
    }

    //  1: [def A] --> 2(copy): [def B/use A] --> 3: [use A]
    @Test
    fun `recognizes when registers interfere only in copy instructions`() {
        // given
        val regA = Register.VirtualRegister()
        val regB = Register.VirtualRegister()

        val block3 =
            BasicBlock(
                BlockLabel("3"),
                listOf(mockInstruction(setOf(), setOf(regA))),
                setOf(),
                mutableSetOf(),
            )
        val block2 =
            BasicBlock(
                BlockLabel("2"),
                listOf(mockCopyInstruction(setOf(regB), setOf(regA))),
                setOf(block3),
                mutableSetOf(),
            )
        val block1 =
            BasicBlock(
                BlockLabel("1"),
                listOf(mockInstruction(setOf(regA), setOf())),
                setOf(block2),
                mutableSetOf(),
            )

        block3.predecessors.add(block2)
        block2.predecessors.add(block1)

        // when
        val liveness = analyzeLiveness(listOf(block1, block2, block3))

        // then
        assertThat(liveness.allRegisters).containsExactlyInAnyOrder(regA, regB)
        assertThat(liveness.interference).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(),
                regB to setOf(),
            ),
        )
        assertThat(liveness.copying).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(regB),
                regB to setOf(regA),
            ),
        )
    }

    //  1: [def A] --> 2(copy): [def B/use A] --> 3: [def A] --> 4: [use B]
    @Test
    fun `recognizes when copied registers interfere in other non copy instructions`() {
        // given
        val regA = Register.VirtualRegister()
        val regB = Register.VirtualRegister()

        val block4 =
            BasicBlock(
                BlockLabel("4"),
                listOf(mockInstruction(setOf(), setOf(regB))),
                setOf(),
                mutableSetOf(),
            )
        val block3 =
            BasicBlock(
                BlockLabel("3"),
                listOf(mockInstruction(setOf(regA), setOf())),
                setOf(block4),
                mutableSetOf(),
            )
        val block2 =
            BasicBlock(
                BlockLabel("2"),
                listOf(mockCopyInstruction(setOf(regB), setOf(regA))),
                setOf(block3),
                mutableSetOf(),
            )
        val block1 =
            BasicBlock(
                BlockLabel("1"),
                listOf(mockInstruction(setOf(regA), setOf())),
                setOf(block2),
                mutableSetOf(),
            )

        block4.predecessors.add(block3)
        block3.predecessors.add(block2)
        block2.predecessors.add(block1)

        // when
        val liveness = analyzeLiveness(listOf(block1, block2, block3, block4))

        // then
        assertThat(liveness.allRegisters).containsExactlyInAnyOrder(regA, regB)
        assertThat(liveness.interference).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                regA to setOf(regB),
                regB to setOf(regA),
            ),
        )
        assertThat(liveness.copying).containsExactlyInAnyOrderEntriesOf(
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
                BasicBlock(BlockLabel("A"), listOf(), setOf(), mutableSetOf()),
            )

        // when & then
        org.junit.jupiter.api.assertThrows<LivenessAnalysisErrorException> {
            analyzeLiveness(lCfgFragment)
        }
    }
}
