package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.instructions.CopyInstruction
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.MemoryAddress
import cacophony.codegen.instructions.RegisterByte
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

data class MovRegReg(
    val lhs: Register,
    val rhs: Register,
) : CopyInstruction {
    override val registersRead: Set<Register> = setOf(rhs)
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        val rhsHardwareReg = hardwareRegisterMapping[rhs]
        if (lhsHardwareReg == rhsHardwareReg) {
            return ""
        }
        return "mov $lhsHardwareReg, $rhsHardwareReg"
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
        return "mov $lhsHardwareReg, $imm"
    }
}

data class MovRegMem(
    val lhs: Register,
    val mem: MemoryAddress,
) : Instruction {
    override val registersRead: Set<Register> = mem.registers()
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        return "mov $lhsHardwareReg, ${mem.toAsm(hardwareRegisterMapping)}"
    }
}

data class MovMemReg(
    val mem: MemoryAddress,
    val rhs: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(rhs).union(mem.registers())
    override val registersWritten: Set<Register> = setOf()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val rhsHardwareReg = hardwareRegisterMapping[rhs]
        return "mov ${mem.toAsm(hardwareRegisterMapping)}, $rhsHardwareReg"
    }
}

data class MovzxReg64Reg8(val lhs: Register, val rhs: RegisterByte) : Instruction {
    override val registersRead: Set<Register> = setOf(rhs.register)
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        val rhsHardwareReg = rhs.map(hardwareRegisterMapping)
        return "movzx $lhsHardwareReg, $rhsHardwareReg"
    }
}
