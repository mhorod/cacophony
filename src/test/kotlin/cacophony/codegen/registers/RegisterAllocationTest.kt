package cacophony.codegen.registers

import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random

class RegisterAllocationTest {
    private fun RegisterAllocation.validate(liveness: Liveness, allowedRegisters: Set<HardwareRegister>) {
        assertThat(spills union successful.keys).isEqualTo(liveness.allRegisters)
        assertThat(spills intersect successful.keys).isEmpty()
        assertThat(successful.values).isSubsetOf(allowedRegisters)

        for ((reg1, interferences) in liveness.interference.entries) {
            for (reg2 in interferences) {
                val hw1 = successful[reg1]
                val hw2 = successful[reg2]
                assertTrue(hw1 == null || hw2 == null || hw1 != hw2)
            }
        }

        for (fixedRegister in liveness.allRegisters.filterIsInstance<Register.FixedRegister>()) {
            assertThat(successful[fixedRegister]).isEqualTo(fixedRegister.hardwareRegister)
        }
    }

    private fun allocateAndValidate(liveness: Liveness, allowedRegisters: Set<HardwareRegister>): RegisterAllocation =
        allocateRegisters(liveness, allowedRegisters).apply { validate(liveness, allowedRegisters) }

    @Test
    fun `cannot allocate undeclared hardware register`() {
        assertThatThrownBy {
            allocateRegisters(
                Liveness(
                    setOf(Register.VirtualRegister(), Register.FixedRegister(HardwareRegister.RBX)),
                    emptyMap(),
                    emptyMap(),
                ),
                setOf(HardwareRegister.RAX),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `register cannot interfere with itself`() {
        assertThatThrownBy {
            val reg1 = Register.VirtualRegister()
            val reg2 = Register.VirtualRegister()
            allocateRegisters(
                Liveness(
                    setOf(reg1, reg2),
                    mapOf(reg1 to setOf(reg1, reg2), reg2 to setOf(reg1, reg2)),
                    emptyMap(),
                ),
                setOf(HardwareRegister.RAX),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `unknown register as interference key`() {
        assertThatThrownBy {
            val known = setOf(Register.VirtualRegister())
            allocateRegisters(
                Liveness(
                    known,
                    mapOf(Register.VirtualRegister() to emptySet()),
                    emptyMap(),
                ),
                setOf(HardwareRegister.RAX),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `unknown register in copying map`() {
        assertThatThrownBy {
            val known = setOf(Register.VirtualRegister())
            allocateRegisters(
                Liveness(
                    known,
                    emptyMap(),
                    mapOf(known.first() to setOf(Register.VirtualRegister())),
                ),
                setOf(HardwareRegister.RAX),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `0 registers with 0 colors`() {
        allocateAndValidate(
            Liveness(
                emptySet(),
                emptyMap(),
                emptyMap(),
            ),
            setOf(),
        )
    }

    @Test
    fun `1 register is enough if there are no interferences`() {
        val allocation =
            allocateAndValidate(
                Liveness(
                    (1..50).map { Register.VirtualRegister() }.toSet(),
                    emptyMap(),
                    emptyMap(),
                ),
                setOf(HardwareRegister.RBP),
            )

        assertThat(allocation.spills).isEmpty()
    }

    @Test
    fun `2 registers with copy are colored properly`() {
        val regA = Register.VirtualRegister()
        val regB = Register.VirtualRegister()
        val allocation =
            allocateAndValidate(
                Liveness(
                    setOf(regA, regB),
                    emptyMap(),
                    mapOf(regA to setOf(regB), regB to setOf(regA)),
                ),
                setOf(HardwareRegister.RBP),
            )

        assertThat(allocation.spills).isEmpty()
    }

    @Test
    fun `2 registers with copy are colored with the same color`() {
        val regA = Register.VirtualRegister()
        val regB = Register.VirtualRegister()
        val allocation =
            allocateAndValidate(
                Liveness(
                    setOf(regA, regB),
                    emptyMap(),
                    mapOf(regA to setOf(regB), regB to setOf(regA)),
                ),
                setOf(HardwareRegister.RBP, HardwareRegister.RSP),
            )

        assertThat(allocation.spills).isEmpty()
        assertThat(allocation.successful[regA]!!).isEqualTo(allocation.successful[regB]!!)
    }

    @Test
    fun `2 fixed registers are colored properly`() {
        val regB = Register.FixedRegister(HardwareRegister.RBX)
        val regC = Register.FixedRegister(HardwareRegister.RCX)
        val allocation =
            allocateAndValidate(
                Liveness(
                    setOf(regB, regC),
                    emptyMap(),
                    emptyMap(),
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX, HardwareRegister.RCX),
            )

        assertThat(allocation.spills).isEmpty()
    }

    @Test
    fun `16 clique is 16 colorable`() {
        val hwRegisters = HardwareRegister.entries.toSet()
        val registers = hwRegisters.map { Register.VirtualRegister() }.toSet()

        val allocation =
            allocateAndValidate(
                Liveness(
                    registers,
                    registers.associateWith { registers subtract setOf(it) },
                    emptyMap(),
                ),
                hwRegisters,
            )

        assertThat(allocation.spills).isEmpty()
    }

    @Test
    fun `interference takes priority over copying`() {
        val regA = Register.VirtualRegister()
        val regB = Register.VirtualRegister()
        val allocation =
            allocateAndValidate(
                Liveness(
                    setOf(regA, regB),
                    mapOf(regA to setOf(regB), regB to setOf(regA)),
                    mapOf(regA to setOf(regB)),
                ),
                setOf(HardwareRegister.RAX),
            )
        assertThat(allocation.spills).hasSize(1)
    }

    @ParameterizedTest
    @ValueSource(ints = [5, 7, 21])
    fun `4 clique blowup`(n: Int) {
        val registers = (0..<n).map { Register.VirtualRegister() }
        val interferences = mutableMapOf<Register, MutableSet<Register>>()
        for (i in 0..<n) {
            for (j in 0..<n) {
                if (i % 4 != j % 4)
                    interferences.getOrPut(registers[i]) { mutableSetOf() }.add(registers[j])
            }
        }
        val allocation =
            allocateAndValidate(
                Liveness(
                    registers.toSet(),
                    interferences,
                    emptyMap(),
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX, HardwareRegister.RCX),
            )
        assertThat(allocation.spills).hasSize(n / 4)
    }

    @ParameterizedTest
    @ValueSource(ints = [4, 8, 20])
    fun `clique with 3 available colors`(n: Int) {
        val registers = (0..<n).map { Register.VirtualRegister() }
        val interferences = mutableMapOf<Register, MutableSet<Register>>()
        for (i in 0..<n) {
            for (j in 0..<n) {
                if (i != j) {
                    interferences.getOrPut(registers[i]) { mutableSetOf() }.add(registers[j])
                }
            }
        }
        val allocation =
            allocateAndValidate(
                Liveness(
                    registers.toSet(),
                    interferences,
                    emptyMap(),
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX, HardwareRegister.RCX),
            )
        assertThat(allocation.spills).hasSize(n - 3)
    }

    @Test
    fun `copying chain with interference link`() {
        val registers = (0..4).map { Register.VirtualRegister() }
        val interferences = mapOf(registers[1] to setOf(registers[3]), registers[3] to setOf(registers[1]))
        val copying = (1..4).associate { registers[it] to setOf(registers[it - 1]) }
        allocateAndValidate(
            Liveness(
                registers.toSet(),
                interferences.toMap(),
                copying.toMap(),
            ),
            setOf(HardwareRegister.RAX),
        )
    }

    @Test
    fun `copying chain with fixed registers`() {
        val registers =
            listOf(
                Register.VirtualRegister(),
                Register.FixedRegister(HardwareRegister.RAX),
                Register.VirtualRegister(),
                Register.FixedRegister(HardwareRegister.RBX),
                Register.VirtualRegister(),
            )
        val copying = (1..<registers.size).associate { registers[it] to setOf(registers[it - 1]) }
        val allocation =
            allocateAndValidate(
                Liveness(
                    registers.toSet(),
                    emptyMap(),
                    copying,
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX),
            )
        assertThat(allocation.spills).isEmpty()
    }

    @Test
    fun `bipartite clique`() {
        val registers = (0..<20).map { Register.VirtualRegister() }
        val interferences =
            (0..<20).map { i ->
                registers[i] to
                    (0..<20)
                        .filter {
                            i / 10 != it / 10
                        }.map { registers[it] }
                        .toSet()
            }
        val allocation =
            allocateAndValidate(
                Liveness(
                    registers.toSet(),
                    interferences.toMap(),
                    emptyMap(),
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX),
            )
        assertThat(allocation.spills).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, 6])
    fun `half of bipartite clique`(seed: Int) {
        val n = 20
        val rnd = Random(seed)
        val registers = (0..<n).map { Register.VirtualRegister() }.shuffled(rnd)
        val interferences = registers.associateWith { mutableSetOf<Register>() }
        for (i in 0..<n / 2) {
            for (j in n / 2..<n) {
                if (rnd.nextBoolean()) {
                    interferences[registers[i]]!!.add(registers[j])
                    interferences[registers[j]]!!.add(registers[i])
                }
            }
        }
        val allocation =
            allocateAndValidate(
                Liveness(
                    registers.toSet(),
                    interferences.toMap(),
                    emptyMap(),
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX),
            )
        assertThat(allocation.spills).isEmpty()
    }

    @Test
    fun `tree of copies`() {
        val reg = (1..7).map { Register.VirtualRegister() }
        val interferences = reg.associateWith { mutableSetOf<Register>() }
        val copy = reg.associateWith { mutableSetOf<Register>() }
        interferences[reg[1]]!!.add(reg[2])
        interferences[reg[2]]!!.add(reg[1])
        interferences[reg[3]]!!.add(reg[4])
        interferences[reg[4]]!!.add(reg[3])
        interferences[reg[5]]!!.add(reg[6])
        interferences[reg[6]]!!.add(reg[5])
        copy[reg[1]]!!.add(reg[0])
        copy[reg[2]]!!.add(reg[0])
        copy[reg[3]]!!.add(reg[1])
        copy[reg[4]]!!.add(reg[1])
        copy[reg[5]]!!.add(reg[2])
        copy[reg[6]]!!.add(reg[2])
        val allocation =
            allocateAndValidate(
                Liveness(
                    reg.toSet(),
                    interferences.toMap(),
                    copy.toMap(),
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX, HardwareRegister.RCX),
            )
        assertThat(allocation.spills).isEmpty()
    }

    @Test
    fun `bipartite clique with some copies`() {
        val reg = (0..8).map { Register.VirtualRegister() }
        val interferences = reg.associateWith { setOf<Register>() }.toMutableMap()
        val copy = reg.associateWith { mutableSetOf<Register>() }

        val a = (0..2).map { reg[it] }.toSet()
        val b = (3..5).map { reg[it] }.toSet()
        a.forEach { interferences[it] = b }
        b.forEach { interferences[it] = a }
        copy[reg[6]]!!.add(reg[0])
        copy[reg[7]]!!.add(reg[5])
        copy[reg[8]]!!.add(reg[1])

        val allocation =
            allocateAndValidate(
                Liveness(
                    reg.toSet(),
                    interferences.toMap(),
                    copy.toMap(),
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX),
            )

        assertThat(allocation.spills).isEmpty()
        assertThat(allocation.successful[reg[6]]).isEqualTo(allocation.successful[reg[0]])
        assertThat(allocation.successful[reg[7]]).isEqualTo(allocation.successful[reg[3]])
        assertThat(allocation.successful[reg[8]]).isEqualTo(allocation.successful[reg[0]])
    }

    @Test
    fun `copying with fixed registers`() {
        val rax = Register.FixedRegister(HardwareRegister.RAX)
        val rbx = Register.FixedRegister(HardwareRegister.RBX)
        val reg1 = Register.VirtualRegister()
        val reg2 = Register.VirtualRegister()
        val allocation =
            allocateAndValidate(
                Liveness(
                    setOf(rax, rbx, reg1, reg2),
                    emptyMap(),
                    mapOf(rax to setOf(reg2), reg1 to setOf(rbx)),
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX),
            )
        assertThat(allocation.spills).isEmpty()
        assertThat(allocation.successful[rax]).isEqualTo(HardwareRegister.RAX)
        assertThat(allocation.successful[rbx]).isEqualTo(HardwareRegister.RBX)
    }

    @Test
    fun `two copies with interference`() {
        val reg1 = Register.VirtualRegister()
        val reg2 = Register.VirtualRegister()
        val reg3 = Register.VirtualRegister()
        val allocation =
            allocateAndValidate(
                Liveness(
                    setOf(reg1, reg2, reg3),
                    mapOf(reg2 to setOf(reg3), reg3 to setOf(reg2)),
                    mapOf(reg1 to setOf(reg2), reg3 to setOf(reg1)),
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX, HardwareRegister.RCX),
            )
        assertThat(allocation.spills).isEmpty()
        assertThat(allocation.successful[reg1]).isIn(allocation.successful[reg2], allocation.successful[reg3])
    }

    @Test
    fun `bipartite clique with some copies and fixed register`() {
        val reg = (0..8).map { Register.VirtualRegister() }.toMutableList<Register>()
        reg[6] = Register.FixedRegister(HardwareRegister.RBX)
        val interferences = reg.associateWith { setOf<Register>() }.toMutableMap()
        val copy = reg.associateWith { mutableSetOf<Register>() }

        val a = (0..2).map { reg[it] }.toSet()
        val b = (3..5).map { reg[it] }.toSet()
        a.forEach { interferences[it] = b }
        b.forEach { interferences[it] = a }
        copy[reg[6]]!!.add(reg[0])
        copy[reg[7]]!!.add(reg[5])
        copy[reg[8]]!!.add(reg[1])

        val allocation =
            allocateAndValidate(
                Liveness(
                    reg.toSet(),
                    interferences.toMap(),
                    copy.toMap(),
                ),
                setOf(HardwareRegister.RAX, HardwareRegister.RBX),
            )

        assertThat(allocation.spills).isEmpty()
        assertThat(allocation.successful[reg[6]]).isEqualTo(allocation.successful[reg[0]])
        assertThat(allocation.successful[reg[7]]).isEqualTo(allocation.successful[reg[3]])
        assertThat(allocation.successful[reg[8]]).isEqualTo(allocation.successful[reg[0]])
    }
}
