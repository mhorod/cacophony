package cacophony.codegen.registers

import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

/**
 * @param successful Map describing the allocation of registers that made it into hardware registers
 * @param spills Registers that did not make it into hardware registers
 */
data class RegisterAllocation(val successful: HardwareRegisterMapping, val spills: Set<Register>)

/**
 * @throws IllegalArgumentException if there are hardware registers that are expected to be allocated but are not allowed
 */
fun allocateRegisters(liveness: Liveness, allowedRegisters: Set<HardwareRegister>): RegisterAllocation {
    TODO("Not yet implemented")
}
