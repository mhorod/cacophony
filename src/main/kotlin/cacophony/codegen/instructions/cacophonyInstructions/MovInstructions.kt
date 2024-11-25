package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.MemoryAddress
import cacophony.codegen.instructions.RegisterByte
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

data class MovRegReg(
    val lhs: Register,
    val rhs: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(rhs)
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        val rhsHardwareReg = hardwareRegisterMapping[rhs]
        return "MOV $lhsHardwareReg $rhsHardwareReg"
    }
}

data class MovRegImm(
    val lhs: Register,
    val imm: Int,
) : Instruction {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        return "MOV $lhsHardwareReg $imm"
    }
}

data class MovRegMem(
    val lhs: Register,
    val mem: MemoryAddress,
) : Instruction {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        return "MOV $lhsHardwareReg ${mem.toAsm()}"
    }
}

data class MovMemReg(
    val mem: MemoryAddress,
    val rhs: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(rhs)
    override val registersWritten: Set<Register> = setOf()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val rhsHardwareReg = hardwareRegisterMapping[rhs]
        return "MOV ${mem.toAsm()} $rhsHardwareReg"
    }
}

data class MovzxReg64Reg8(val lhs: Register, val rhs: RegisterByte) : Instruction {
    override val registersRead: Set<Register> = setOf(rhs.register)
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        val rhsHardwareReg = rhs.map(hardwareRegisterMapping)
        return "MOVZX $lhsHardwareReg $rhsHardwareReg"
    }
}
