package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

data class PushReg(
    val reg: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(reg)
    override val registersWritten: Set<Register> = setOf()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val hardwareReg = hardwareRegisterMapping[reg]
        return "PUSH $hardwareReg"
    }
}

data class Pop(
    val reg: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf(reg)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val hardwareReg = hardwareRegisterMapping[reg]
        return "POP $hardwareReg"
    }
}

data class TestRegReg(
    val lhs: Register,
    val rhs: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(rhs, lhs)
    override val registersWritten: Set<Register> = setOf() // only rFLAGS are set

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        val rhsHardwareReg = hardwareRegisterMapping[rhs]
        return "TEST $lhsHardwareReg $rhsHardwareReg"
    }
}

data class CmpRegReg(
    val lhs: Register,
    val rhs: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(rhs, lhs)
    override val registersWritten: Set<Register> = setOf()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        val rhsHardwareReg = hardwareRegisterMapping[rhs]
        return "CMP $lhsHardwareReg $rhsHardwareReg"
    }
}

data class Je(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "JE")

data class Jne(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "JNE")

data class Jl(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "JL")

data class Jle(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "JLE")

data class Jg(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "JG")

data class Jge(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "JGE")

data class Jz(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "JZ")

data class Jnz(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "JNZ")

data class Call(
    val label: BlockLabel,
) : Instruction {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "CALL ${label.name}"
}

class Ret : Instruction {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "RET"
}
