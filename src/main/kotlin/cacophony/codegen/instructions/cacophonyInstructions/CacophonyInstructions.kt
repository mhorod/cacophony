package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.MemoryAddress
import cacophony.codegen.instructions.RegisterByte
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

class CacophonyInstructions {
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

    data class Sete(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETE")

    data class Setne(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETNE")

    data class Setl(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETL")

    data class Setle(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETLE")

    data class Setg(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETG")

    data class Setge(override val byte: RegisterByte) : InstructionTemplates.SetccInstruction(byte, "SETGE")

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

    class Ret : Instruction {
        override val registersRead: Set<Register> = setOf()
        override val registersWritten: Set<Register> = setOf()

        override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "RET"
    }
}

fun MemoryAddress.toAsm(): String {
    val builder = StringBuilder()
    builder.append("[").append(base)
    if (index != null && scale != null) {
        builder.append("+$scale*$index")
    }
    if (displacement != null) {
        builder.append("+$displacement")
    }
    builder.append("]")
    return builder.toString()
}
