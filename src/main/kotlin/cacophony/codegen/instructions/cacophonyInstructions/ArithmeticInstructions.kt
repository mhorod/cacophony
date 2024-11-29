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
        "add",
    )

data class AddRegImm(
    override val lhs: Register,
    override val imm: Int,
) : InstructionTemplates.BinaryRegConstInstruction(
        lhs,
        imm,
        "add",
    )

data class SubRegReg(
    override val lhs: Register,
    override val rhs: Register,
) : InstructionTemplates.BinaryRegRegInstruction(
        lhs,
        rhs,
        "sub",
    )

data class SubRegImm(
    override val lhs: Register,
    override val imm: Int,
) : InstructionTemplates.BinaryRegConstInstruction(
        lhs,
        imm,
        "sub",
    )

data class XorRegReg(
    override val lhs: Register,
    override val rhs: Register,
) : InstructionTemplates.BinaryRegRegInstruction(
        lhs,
        rhs,
        "xor",
    )

data class XorRegImm(
    override val lhs: Register,
    override val imm: Int,
) : InstructionTemplates.BinaryRegConstInstruction(
        lhs,
        imm,
        "xor",
    )

data class IMulRegReg(
    override val lhs: Register,
    override val rhs: Register,
) : InstructionTemplates.BinaryRegRegInstruction(
        lhs,
        rhs,
        "imul",
    )

class Cqo : Instruction {
    override val registersRead: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RAX))
    override val registersWritten: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RDX))

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "cqo"
}

data class IDiv(val reg: Register) : Instruction {
    override val registersRead: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RAX), reg)
    override val registersWritten: Set<Register> =
        setOf(Register.FixedRegister(HardwareRegister.RAX), Register.FixedRegister(HardwareRegister.RDX))

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val hardwareReg = hardwareRegisterMapping[reg]
        return "idiv $hardwareReg"
    }
}

data class Sete(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "sete")

data class Setne(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "setne")

data class Setl(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "setl")

data class Setle(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "setle")

data class Setg(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "setg")

data class Setge(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "setge")