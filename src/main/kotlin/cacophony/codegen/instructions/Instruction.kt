package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.controlflow.*

data class RegisterByte(val register: Register) {
    fun map(hardwareRegisterMapping: HardwareRegisterMapping): HardwareRegisterByte {
        val hardwareRegister = hardwareRegisterMapping[register] ?: error("Register $register not mapped")
        return HardwareRegisterByte.fromRegister(hardwareRegister)
    }

    private val byteName =
        mapOf(
            "RAX" to "al",
            "RBX" to "bl",
            "RCX" to "cl",
            "RDX" to "dl",
            "RSP" to "spl",
            "RBP" to "bpl",
            "RSI" to "sil",
            "RDI" to "dil",
            "R8" to "r8b",
            "R9" to "r9b",
            "R10" to "r10b",
            "R11" to "r11b",
            "R12" to "r12b",
            "R13" to "r13b",
            "R14" to "r14b",
            "R15" to "r15b",
        )

    override fun toString() = byteName.getOrDefault(register.toString(), "RegisterByte($register)")
}

enum class RegisterSize {
    BYTE,
    WORD,
    DWORD,
    QWORD,
}

data class MemoryAddress(val base: Register, val index: Register?, val scale: CFGNode.Constant?, val displacement: CFGNode.Constant?) {
    fun registers() = setOf(base, index).filterNotNull().toSet()
}

interface Instruction {
    val registersRead: Set<Register>
    val registersWritten: Set<Register>

    fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String

    fun substituteRegisters(map: Map<Register, Register>): Instruction

    fun isNoop(hardwareRegisterMapping: HardwareRegisterMapping, usedLocalLabels: Set<BlockLabel>): Boolean = false
}

interface CopyInstruction : Instruction {
    fun copyInto(): Register

    fun copyFrom(): Register
}

typealias InstructionMaker = (ValueSlotMapping) -> List<Instruction>
