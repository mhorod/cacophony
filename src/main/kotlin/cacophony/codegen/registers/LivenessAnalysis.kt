package cacophony.codegen.registers

import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.controlflow.Register

typealias RegisterRelations = Map<Register, Set<Register>>

data class Liveness(val allRegisters: Set<Register>, val interference: RegisterRelations, val copying: RegisterRelations)

fun analyzeLiveness(cfgFragment: LoweredCFGFragment): Liveness {
    TODO("Not yet implemented")
}
