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
 * @throws IllegalArgumentException if registersInteraction object is invalid i.e:
 * - There is a register interfering with itself, or
 * - interference or copying mappings contain register outside registersInteraction.allRegisters
 */
fun allocateRegisters(registersInteraction: RegistersInteraction, allowedRegisters: Set<HardwareRegister>): RegisterAllocation {
    val allocation = RegisterAllocator(registersInteraction, allowedRegisters, FirstFitGraphColoring()).allocate()
    allocation.validate(registersInteraction, allowedRegisters)
    return allocation
}

class RegisterAllocator(
    private val registersInteraction: RegistersInteraction,
    private val allowedRegisters: Set<HardwareRegister>,
    private val graphColoring: GraphColoring<Register, HardwareRegister>,
) {
    init {
        for (mapping in listOf(registersInteraction.interference, registersInteraction.copying)) {
            require(registersInteraction.allRegisters.containsAll(mapping.keys union mapping.values.flatten())) { "Unexpected register" }
        }
        for ((register, interferences) in registersInteraction.interference) {
            require(register !in interferences) { "Register cannot interfere with itself" }
        }
    }

    fun allocate(): RegisterAllocation {
        val interferenceGraph =
            registersInteraction.allRegisters
                .associateWith { registersInteraction.interference.getOrDefault(it, emptySet()) }

        val registersColoring =
            graphColoring.doColor(
                interferenceGraph,
                registersInteraction.copying,
                registersInteraction.allRegisters
                    .filterIsInstance<Register.FixedRegister>()
                    .associateWith { (it as Register.FixedRegister).hardwareRegister },
                allowedRegisters,
            )

        return RegisterAllocation(
            registersColoring,
            registersInteraction.allRegisters
                .filter { !registersColoring.containsKey(it) }
                .toSet(),
        )
    }
}

/**
 * @throws RegisterAllocationException if:
 * - Spills and successful mappings do not cover whole registersInteraction.allRegisters,
 * - spills and successful mappings intersect,
 * - not allowed register was used for allocation of virtual register,
 * - hardware register was not allocated to itself,
 * - interfering registers received the same allocation, or
 * - fixed register was not allocated
 */
fun RegisterAllocation.validate(registersInteraction: RegistersInteraction, allowedRegisters: Set<HardwareRegister>) {
    if (spills union successful.keys != registersInteraction.allRegisters) {
        throw RegisterAllocationException("Spills and successful registers do not cover all registers")
    }

    if ((spills intersect successful.keys).isNotEmpty()) {
        throw RegisterAllocationException("Spills and successful registers intersect")
    }

    val illegalAllocation = successful.filter { it.key is Register.VirtualRegister && it.value !in allowedRegisters }.entries.firstOrNull()
    if (illegalAllocation != null) {
        throw RegisterAllocationException(
            "Not allowed hardware register was used for virtual register allocation: " +
                "${illegalAllocation.key} -> ${illegalAllocation.value}",
        )
    }

    if (
        successful
            .filter { it.key is Register.FixedRegister }
            .mapKeys { it.key as Register.FixedRegister }
            .any { it.value !== it.key.hardwareRegister }
    ) {
        throw RegisterAllocationException("Hardware register was not allocated to itself")
    }

    for ((reg1, interferences) in registersInteraction.interference.entries) {
        for (reg2 in interferences) {
            val hw1 = successful[reg1]
            val hw2 = successful[reg2]
            if (hw1 != null && hw2 != null && hw1 == hw2) {
                throw RegisterAllocationException("Interfering registers $reg1 and $reg2 received the same allocation: $hw1")
            }
        }
    }

    registersInteraction.allRegisters.filterIsInstance<Register.FixedRegister>().forEach {
        if (!successful.containsKey(it)) {
            throw RegisterAllocationException("Fixed register $it was not allocated.")
        }
    }
}
