package cacophony.codegen.instructions

import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register
import cacophony.controlflow.ValueSlotMapping

interface Instruction {
    val registersRead: Set<Register>

    val registersWritten: Set<Register>

    fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String
}

interface CopyInstruction : Instruction

typealias InstructionMaker = (ValueSlotMapping) -> List<Instruction>
