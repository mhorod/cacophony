package cacophony.codegen.instructions

import cacophony.controlflow.HardwareRegisterByte
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register
import cacophony.controlflow.ValueSlotMapping

data class RegisterByte(val register: Register) {
    fun map(hardwareRegisterMapping: HardwareRegisterMapping): HardwareRegisterByte {
        val hardwareRegister = hardwareRegisterMapping[register] ?: error("Register $register not mapped")
        return HardwareRegisterByte.fromRegister(hardwareRegister)
    }
}

enum class RegisterSize {
    BYTE,
    WORD,
    DWORD,
    QWORD,
}

data class MemoryAddress(val base: Register, val index: Register?, val scale: Int?, val displacement: Int?) {
    fun registers() = setOf(base, index).filterNotNull().toSet()
}

interface Instruction {
    val registersRead: Set<Register>

    val registersWritten: Set<Register>

    fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String

    fun isNoop(hardwareRegisterMapping: HardwareRegisterMapping): Boolean = false
}

interface CopyInstruction : Instruction

typealias InstructionMaker = (ValueSlotMapping) -> List<Instruction>
