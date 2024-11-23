package cacophony.codegen.registers

import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegisterAllocationTest {
    private fun <E> Map<E, Set<E>>.undirect(): Map<E, Set<E>> {
        val returnValue: MutableMap<E, MutableSet<E>> = mutableMapOf()
        for ((x, ySet) in entries) {
            for (y in ySet) {
                returnValue.getOrPut(x) { mutableSetOf() }.add(y)
                returnValue.getOrPut(y) { mutableSetOf() }.add(x)
            }
        }
        return returnValue
    }

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

        // if there is spillage, exactly two registers are free
//        if (spills.isNotEmpty()) {
//            assertThat(successful.values.toSet().size).isEqualTo(allowedRegisters.size - 2)
//        }
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

//    private fun generateInstance(seed: Int): Pair<Liveness, Set<HardwareRegister>> {
//        val rnd = Random(seed)
//
//        val availableHwRegisterCount = (1..HardwareRegister.entries.size).random(rnd)
//        val availableHwRegisters = HardwareRegister.entries.take(availableHwRegisterCount)
//
//        val hwRegisters = availableHwRegisters.take((0..availableHwRegisterCount).random(rnd)).map { Register.FixedRegister(it) }
//
//        val virtualRegisterCount = (1..100).random(rnd)
//        val virtualRegisters = (1..virtualRegisterCount).map { Register.VirtualRegister() }
//
//        val allRegisters = listOf(hwRegisters, virtualRegisters).flatten()
//
//        val interferenceDensity = rnd.nextDouble(1.0)
//        val copyDensity = rnd.nextDouble(1.0)
//
//
//
// //        return Pair(availableHwRegisters.toSet())
//        TODO()
//    }
//
//    @Test
//    fun `random instances`() {
//        (1..2000).forEach { seed -> generateInstance(seed).let { allocateAndValidate(it.first, it.second) } }
//    }
}
