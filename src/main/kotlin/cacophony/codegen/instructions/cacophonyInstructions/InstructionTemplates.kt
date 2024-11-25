package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

class InstructionTemplates {
    abstract class BinaryRegRegInstruction(
        open val lhs: Register,
        open val rhs: Register,
        private val op: String,
    ) : Instruction {
        override val registersRead: Set<Register> = setOf(lhs, rhs)
        override val registersWritten: Set<Register> = setOf(lhs)

        override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
            val lhsHardwareReg = hardwareRegisterMapping[lhs]
            val rhsHardwareReg = hardwareRegisterMapping[rhs]
            return "$op $lhsHardwareReg $rhsHardwareReg"
        }
    }

    abstract class BinaryRegConstInstruction(
        open val lhs: Register,
        open val imm: Int,
        private val op: String,
    ) : Instruction {
        override val registersRead: Set<Register> = setOf(lhs)
        override val registersWritten: Set<Register> = setOf(lhs)

        override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
            val lhsHardwareReg = hardwareRegisterMapping[lhs]
            return "$op $lhsHardwareReg $imm"
        }
    }
}
