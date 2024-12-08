package cacophony.codegen.registers

import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register
import cacophony.graphs.FirstFitGraphColoring
import cacophony.graphs.GraphColoring

class RegisterAllocationException(reason: String) : Exception(reason)

/**
 * @param successful Map describing the allocation of registers that made it into hardware registers
 * @param spills Registers that did not make it into hardware registers
 */
data class RegisterAllocation(val successful: HardwareRegisterMapping, val spills: Set<Register>)

/**
 * @throws IllegalArgumentException if liveness object is invalid i.e:
 * - There is a register interfering with itself, or
 * - interference or copying mappings contain register outside of liveness.allRegisters
 */
fun allocateRegisters(liveness: Liveness, allowedRegisters: Set<HardwareRegister>): RegisterAllocation {
    val allocation = RegisterAllocator(liveness, allowedRegisters, FirstFitGraphColoring()).allocate()
    allocation.validate(liveness, allowedRegisters)
    return allocation
}

class RegisterAllocator(
    private val liveness: Liveness,
    private val allowedRegisters: Set<HardwareRegister>,
    private val graphColoring: GraphColoring<Register, HardwareRegister>,
) {
    init {
        for (mapping in listOf(liveness.interference, liveness.copying)) {
            require(liveness.allRegisters.containsAll(mapping.keys union mapping.values.flatten())) { "Unexpected register" }
        }
        for ((register, interferences) in liveness.interference) {
            require(register !in interferences) { "Register cannot interfere with itself" }
        }
    }

    fun allocate(): RegisterAllocation {
        val interferenceGraph = liveness.allRegisters.associateWith { liveness.interference.getOrDefault(it, emptySet()) }

        val registersColoring =
            graphColoring.doColor(
                interferenceGraph,
                liveness.copying,
                liveness.allRegisters
                    .filterIsInstance<Register.FixedRegister>()
                    .associateWith { (it as Register.FixedRegister).hardwareRegister },
                allowedRegisters,
            )

        return RegisterAllocation(registersColoring, liveness.allRegisters.filter { !registersColoring.containsKey(it) }.toSet())
    }
}

/**
 * @throws RegisterAllocationException if:
 * - Spills and successful mappings do not cover whole liveness.allRegisters,
 * - spills and successful mappings intersect,
 * - not allowed register was used for allocation of virtual register,
 * - hardware register was not allocated to itself,
 * - interfering registers received the same allocation, or
 * - fixed register was not allocated
 */
fun RegisterAllocation.validate(liveness: Liveness, allowedRegisters: Set<HardwareRegister>) {
    if (spills union successful.keys != liveness.allRegisters) {
        throw RegisterAllocationException("Spills and successful registers do not cover all registers")
    }

    if ((spills intersect successful.keys).isNotEmpty()) {
        throw RegisterAllocationException("Spills and successful registers intersect")
    }

    if (successful.filter { it.key is Register.VirtualRegister }.values.all { it !in allowedRegisters }) {
        throw RegisterAllocationException("Not allowed hardware register was used for virtual register allocation")
    }

    if (
        successful
            .filter { it.key is Register.FixedRegister }
            .mapKeys { it.key as Register.FixedRegister }
            .any { it.value !== it.key.hardwareRegister }
    ) {
        throw RegisterAllocationException("Hardware register was not allocated to itself")
    }

    for ((reg1, interferences) in liveness.interference.entries) {
        for (reg2 in interferences) {
            val hw1 = successful[reg1]
            val hw2 = successful[reg2]
            if (hw1 != null && hw2 != null && hw1 == hw2) {
                throw RegisterAllocationException("Interfering registers $reg1 and $reg2 received the same allocation: $hw1")
            }
        }
    }

    liveness.allRegisters.filterIsInstance<Register.FixedRegister>().forEach {
        if (!successful.containsKey(it)) {
            throw RegisterAllocationException("Fixed register $it was not allocated.")
        }
    }
}
