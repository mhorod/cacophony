package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.functionBodyLabel
import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.*
import cacophony.controlflow.functions.SystemVAMD64CallConvention
import cacophony.semantic.syntaxtree.Definition

data class PushReg(
    val reg: Register,
) : Instruction {
    override val registersRead: Set<Register> = setOf(reg, Register.FixedRegister(HardwareRegister.RSP))
    override val registersWritten: Set<Register> = setOf(Register.FixedRegister(HardwareRegister.RSP))

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
        val hardwareReg = hardwareRegisterMapping[reg]
        return "push $hardwareReg"
    }

    override fun substituteRegisters(map: Map<Register, Register>): PushReg = PushReg(reg.substitute(map))
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

    override fun substituteRegisters(map: Map<Register, Register>): Pop = Pop(reg.substitute(map))
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
        return "test $lhsHardwareReg, $rhsHardwareReg"
    }

    override fun substituteRegisters(map: Map<Register, Register>): TestRegReg = TestRegReg(lhs.substitute(map), rhs.substitute(map))
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

    override fun substituteRegisters(map: Map<Register, Register>): CmpRegReg = CmpRegReg(lhs.substitute(map), rhs.substitute(map))
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

data class Call(val function: Definition.FunctionDeclaration) : InstructionTemplates.FixedRegistersInstruction() {
    override val registersRead =
        setOf(Register.FixedRegister(HardwareRegister.RSP)) union
            SystemVAMD64CallConvention.preservedRegisters().map(Register::FixedRegister)
    override val registersWritten: Set<Register> =
        HardwareRegister
            .entries
            .filterNot(SystemVAMD64CallConvention.preservedRegisters()::contains)
            .map(Register::FixedRegister)
            .toSet()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "call ${functionBodyLabel(function).name}"
}

class Ret : InstructionTemplates.FixedRegistersInstruction() {
    override val registersRead =
        setOf(Register.FixedRegister(HardwareRegister.RSP), Register.FixedRegister(SystemVAMD64CallConvention.returnRegister()))
    override val registersWritten = setOf<Register>()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "ret"
}

data class LocalLabel(val label: BlockLabel) : InstructionTemplates.FixedRegistersInstruction() {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = ".${label.name}:"
}

data class Label(val label: BlockLabel) : InstructionTemplates.FixedRegistersInstruction() {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "${label.name}:"
}
