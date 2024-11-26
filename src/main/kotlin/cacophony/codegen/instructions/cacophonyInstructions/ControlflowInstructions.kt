package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

data class PushReg(
    val reg: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(reg, Register.FixedRegister(HardwareRegister.RSP))
    override val registersWritten: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RSP))

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val hardwareReg = hardwareRegisterMapping[reg]
        return "PUSH $hardwareReg"
    }
}

data class Pop(
    val reg: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RSP))
    override val registersWritten: Set<Register> = setOf(reg, Register.FixedRegister(HardwareRegister.RSP))

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val hardwareReg = hardwareRegisterMapping[reg]
        return "pop $hardwareReg"
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
        return "test $lhsHardwareReg $rhsHardwareReg"
    }
}

data class CmpRegReg(
    val lhs: Register,
    val rhs: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(rhs, lhs)
    override val registersWritten: Set<Register> = setOf() // only rFLAGS are set

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        val rhsHardwareReg = hardwareRegisterMapping[rhs]
        return "cmp $lhsHardwareReg, $rhsHardwareReg"
    }
}

data class Jmp(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "jmp")

data class Je(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "je")

data class Jne(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "jne")

data class Jl(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "jl")

data class Jle(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "jle")

data class Jg(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "jg")

data class Jge(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "jge")

data class Jz(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "jz")

data class Jnz(override val label: BlockLabel) : InstructionTemplates.JccInstruction(label, "jnz")

data class Call(
    val label: BlockLabel,
) : Instruction {
    override val registersRead: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RSP))
    override val registersWritten: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RSP))

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "call ${label.name}"
}

class Ret : Instruction {
    override val registersRead: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RSP))
    override val registersWritten: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RSP))

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "ret"
}
