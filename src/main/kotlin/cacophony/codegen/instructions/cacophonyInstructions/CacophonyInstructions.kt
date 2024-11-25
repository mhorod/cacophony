package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.MemoryAddress
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

    data class IMulRegReg(
        override val lhs: Register,
        override val rhs: Register,
    ) : InstructionTemplates.BinaryRegRegInstruction(
            lhs,
            rhs,
            "IMUL",
        )

    data class MovRegReg(
        val lhs: Register,
        val rhs: Register,
    ) : Instruction {
        override val registersRead: Set<Register> = setOf(lhs, rhs)
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
        override val registersRead: Set<Register> = setOf(lhs)
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
        override val registersRead: Set<Register> = setOf(lhs)
        override val registersWritten: Set<Register> = setOf(lhs)

        override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
            val lhsHardwareReg = hardwareRegisterMapping[lhs]
            return "MOV $lhsHardwareReg ${mem.toAsm()}"
        }
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
