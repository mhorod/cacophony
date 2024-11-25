package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.RegisterByte
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

data class AddRegReg(
    override val lhs: Register,
    override val rhs: Register,
) : InstructionTemplates.BinaryRegRegInstruction(
        lhs,
        rhs,
        "ADD",
    )

data class AddRegImm(
    override val lhs: Register,
    override val imm: Int,
) : InstructionTemplates.BinaryRegConstInstruction(
        lhs,
        imm,
        "ADD",
    )

data class SubRegReg(
    override val lhs: Register,
    override val rhs: Register,
) : InstructionTemplates.BinaryRegRegInstruction(
        lhs,
        rhs,
        "SUB",
    )

data class SubRegImm(
    override val lhs: Register,
    override val imm: Int,
) : InstructionTemplates.BinaryRegConstInstruction(
        lhs,
        imm,
        "SUB",
    )

data class XorRegReg(
    override val lhs: Register,
    override val rhs: Register,
) : InstructionTemplates.BinaryRegRegInstruction(
        lhs,
        rhs,
        "XOR",
    )

data class XorRegImm(
    override val lhs: Register,
    override val imm: Int,
) : InstructionTemplates.BinaryRegConstInstruction(
        lhs,
        imm,
        "XOR",
    )

data class IMulRegReg(
    override val lhs: Register,
    override val rhs: Register,
) : InstructionTemplates.BinaryRegRegInstruction(
        lhs,
        rhs,
        "IMUL",
    )

class Cqo : Instruction {
    override val registersRead: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RAX))
    override val registersWritten: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RDX))

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "CQO"
}

data class IDiv(val reg: Register) : Instruction {
    override val registersRead: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RAX), reg)
    override val registersWritten: Set<Register> =
        setOf(Register.FixedRegister(HardwareRegister.RAX), Register.FixedRegister(HardwareRegister.RDX))

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val hardwareReg = hardwareRegisterMapping[reg]
        return "IDIV $hardwareReg"
    }
}

data class Sete(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETE")

data class Setne(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETNE")

data class Setl(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETL")

data class Setle(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETLE")

data class Setg(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETG")

data class Setge(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETGE")
