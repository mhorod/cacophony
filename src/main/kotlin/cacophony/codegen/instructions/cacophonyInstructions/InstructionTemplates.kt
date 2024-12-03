package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.RegisterByte
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

class InstructionTemplates {
    abstract class FixedRegistersInstruction : Instruction {
        override fun substituteRegisters(map: Map<Register, Register>): Instruction = this
    }

    open class BinaryRegRegInstruction(
        open val lhs: Register,
        open val rhs: Register,
        private val op: String,
    ) : Instruction {
        override val registersRead: Set<Register> by lazy { setOf(lhs, rhs) }
        override val registersWritten: Set<Register> by lazy { setOf(lhs) }

        override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
            val lhsHardwareReg = hardwareRegisterMapping[lhs]
            val rhsHardwareReg = hardwareRegisterMapping[rhs]
            return "$op $lhsHardwareReg, $rhsHardwareReg"
        }

        override fun substituteRegisters(map: Map<Register, Register>): BinaryRegRegInstruction =
            BinaryRegRegInstruction(
                lhs.substitute(map),
                rhs.substitute(map),
                op,
            )
    }

    open class BinaryRegConstInstruction(
        open val lhs: Register,
        open val imm: Int,
        private val op: String,
    ) : Instruction {
        override val registersRead: Set<Register> by lazy { setOf(lhs) }
        override val registersWritten: Set<Register> by lazy { setOf(lhs) }

        override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
            val lhsHardwareReg = hardwareRegisterMapping[lhs]
            return "$op $lhsHardwareReg, $imm"
        }

        override fun substituteRegisters(map: Map<Register, Register>): BinaryRegConstInstruction =
            BinaryRegConstInstruction(
                lhs.substitute(map),
                imm,
                op,
            )
    }

    abstract class JccInstruction(
        open val label: BlockLabel,
        private val op: String,
    ) : FixedRegistersInstruction() {
        override val registersRead: Set<Register> = setOf()
        override val registersWritten: Set<Register> = setOf()

        override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "$op .${label.name}"
    }

    open class SetccInstruction(
        open val byte: RegisterByte,
        private val op: String,
    ) : Instruction {
        override val registersRead: Set<Register> = setOf()
        override val registersWritten: Set<Register> by lazy { setOf(byte.register) }

        override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "$op ${byte.map(hardwareRegisterMapping)}"

        override fun substituteRegisters(map: Map<Register, Register>): SetccInstruction =
            SetccInstruction(
                RegisterByte(
                    byte.register.substitute(map),
                ),
                op,
            )
    }
}
