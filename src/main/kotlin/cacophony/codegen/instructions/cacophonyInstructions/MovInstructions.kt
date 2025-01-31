package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.CopyInstruction
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.MemoryAddress
import cacophony.codegen.instructions.RegisterByte
import cacophony.controlflow.CFGNode
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
        return "mov $lhsHardwareReg, $rhsHardwareReg"
    }

    override fun isNoop(hardwareRegisterMapping: HardwareRegisterMapping, usedLocalLabels: Set<BlockLabel>) =
        hardwareRegisterMapping[lhs] == hardwareRegisterMapping[rhs]

    override fun substituteRegisters(map: Map<Register, Register>): MovRegReg = MovRegReg(lhs.substitute(map), rhs.substitute(map))

    override fun copyInto(): Register = lhs

    override fun copyFrom(): Register = rhs
}

data class MovRegImm(
    val lhs: Register,
    val imm: CFGNode.Constant,
) : Instruction {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]

        require(lhsHardwareReg != null) { "No hardware register mapping found for $lhs" }

        return "mov $lhsHardwareReg, ${imm.value}"
    }

    override fun substituteRegisters(map: Map<Register, Register>): MovRegImm = MovRegImm(lhs.substitute(map), imm)
}

data class MovRegMem(
    val lhs: Register,
    val mem: MemoryAddress,
) : Instruction {
    override val registersRead: Set<Register> = mem.registers()
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]

        require(lhsHardwareReg != null) { "No hardware register mapping found for $lhs" }

        return "mov $lhsHardwareReg, ${mem.toAsm(hardwareRegisterMapping)}"
    }

    override fun substituteRegisters(map: Map<Register, Register>): MovRegMem = MovRegMem(lhs.substitute(map), mem.substituteRegisters(map))
}

data class MovMemReg(
    val mem: MemoryAddress,
    val rhs: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(rhs).union(mem.registers())
    override val registersWritten: Set<Register> = setOf()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val rhsHardwareReg = hardwareRegisterMapping[rhs]

        require(rhsHardwareReg != null) { "No hardware register mapping found for $rhs" }

        return "mov ${mem.toAsm(hardwareRegisterMapping)}, $rhsHardwareReg"
    }

    override fun substituteRegisters(map: Map<Register, Register>): MovMemReg = MovMemReg(mem.substituteRegisters(map), rhs.substitute(map))
}

data class MovzxReg64Reg8(val lhs: Register, val rhs: RegisterByte) : Instruction {
    override val registersRead: Set<Register> = setOf(rhs.register)
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        val rhsHardwareReg = hardwareRegisterMapping[rhs.register]

        require(lhsHardwareReg != null) { "No hardware register mapping found for $lhs" }
        require(rhsHardwareReg != null) { "No hardware register mapping found for $rhs" }

        return "movzx $lhsHardwareReg, ${RegisterByte(Register.FixedRegister(rhsHardwareReg))}"
    }

    override fun substituteRegisters(map: Map<Register, Register>): MovzxReg64Reg8 =
        MovzxReg64Reg8(
            lhs.substitute(map),
            RegisterByte(rhs.register.substitute(map)),
        )
}

data class MovRegLabel(val lhs: Register, val dataLabel: String) : Instruction {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf(lhs)

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val lhsHardwareReg = hardwareRegisterMapping[lhs]
        return "mov $lhsHardwareReg, $dataLabel"
    }

    override fun substituteRegisters(map: Map<Register, Register>): Instruction = MovRegLabel(lhs.substitute(map), dataLabel)
}
