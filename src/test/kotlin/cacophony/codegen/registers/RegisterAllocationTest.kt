package cacophony.codegen.registers

import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.graphs.GraphColoring
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegisterAllocationTest {
    @Test
    fun `calls graph coloring with proper parameters`() {
        // given
        val reg0 = Register.VirtualRegister()
        val reg1 = Register.VirtualRegister()
        val reg2 = Register.VirtualRegister()
        val reg3 = Register.VirtualRegister()
        val reg4 = Register.FixedRegister(HardwareRegister.RBX)
        val liveness =
            Liveness(
                setOf(reg0, reg1, reg2, reg3, reg4),
                mapOf(reg0 to setOf(reg1)),
                mapOf(reg2 to setOf(reg1)),
            )

        val graphColoring = mockk<GraphColoring<Register, HardwareRegister>>()
        every { graphColoring.doColor(any(), any(), any(), any()) } returns mapOf()

        val registerAllocator =
            RegisterAllocator(
                liveness,
                setOf(HardwareRegister.RAX),
                graphColoring,
            )

        // when
        registerAllocator.allocate()

        // then
        verify {
            graphColoring.doColor(
                mapOf(reg0 to setOf(reg1), reg1 to setOf(), reg2 to setOf(), reg3 to setOf(), reg4 to setOf()),
                mapOf(reg2 to setOf(reg1)),
                mapOf(reg4 to HardwareRegister.RBX),
                setOf(HardwareRegister.RAX),
            )
        }
    }

    @Test
    fun `throws if register interferes with itself`() {
        // given
        val reg1 = Register.VirtualRegister()
        val reg2 = Register.VirtualRegister()
        val liveness =
            Liveness(
                setOf(reg1, reg2),
                mapOf(reg1 to setOf(reg1, reg2), reg2 to setOf(reg1, reg2)),
                emptyMap(),
            )
        val graphColoring = mockk<GraphColoring<Register, HardwareRegister>>()

        // when & then
        assertThrows<IllegalArgumentException> {
            RegisterAllocator(liveness, setOf(HardwareRegister.RAX), graphColoring)
        }
    }

    @Test
    fun `throws if unknown register as interference key`() {
        // given
        val known = setOf(Register.VirtualRegister())
        val liveness =
            Liveness(
                known,
                mapOf(Register.VirtualRegister() to emptySet()),
                emptyMap(),
            )
        val graphColoring = mockk<GraphColoring<Register, HardwareRegister>>()

        // when & then
        assertThrows<IllegalArgumentException> {
            RegisterAllocator(
                liveness,
                setOf(HardwareRegister.RAX),
                graphColoring,
            )
        }
    }

    @Test
    fun `throws if unknown register in copying map`() {
        // given
        val known = setOf(Register.VirtualRegister())
        val liveness =
            Liveness(
                known,
                emptyMap(),
                mapOf(known.first() to setOf(Register.VirtualRegister())),
            )
        val graphColoring = mockk<GraphColoring<Register, HardwareRegister>>()

        // when & then
        assertThrows<IllegalArgumentException> {
            RegisterAllocator(liveness, setOf(HardwareRegister.RAX), graphColoring)
        }
    }
}
