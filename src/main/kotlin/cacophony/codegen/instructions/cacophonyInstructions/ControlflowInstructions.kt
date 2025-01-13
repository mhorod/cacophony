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

        require(hardwareReg != null) { "No hardware register mapping found for $reg" }

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

        require(hardwareReg != null) { "No hardware register mapping found for $reg" }

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

        require(lhsHardwareReg != null) { "No hardware register mapping found for $lhs" }
        require(rhsHardwareReg != null) { "No hardware register mapping found for $rhs" }

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

        require(lhsHardwareReg != null) { "No hardware register mapping found for $lhs" }
        require(rhsHardwareReg != null) { "No hardware register mapping found for $rhs" }

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

private fun Definition.FunctionDeclaration.argumentRegisters(): Set<Register.FixedRegister> {
    val argumentCount =
        when (this) {
            is Definition.FunctionDefinition -> arguments.size + 1
            is Definition.ForeignFunctionDeclaration -> type!!.argumentsType.size
        }
    return REGISTER_ARGUMENT_ORDER.take(argumentCount).map { Register.FixedRegister(it) }.toSet()
}

data class Call(val function: Definition.FunctionDeclaration) : InstructionTemplates.FixedRegistersInstruction() {
    override val registersRead =
        setOf(
            Register.FixedRegister(HardwareRegister.RSP),
            Register.FixedRegister(HardwareRegister.RBP),
        ) union function.argumentRegisters()
    override val registersWritten: Set<Register> =
        HardwareRegister
            .entries
            .filterNot(SystemVAMD64CallConvention.preservedRegisters()::contains)
            .filterNot { it == HardwareRegister.RSP }
            .map(Register::FixedRegister)
            .toSet()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "call ${functionBodyLabel(function).name}"
}

/**
 * A raw call to a function which does not create a separate stack frame.
 * It can be used for jumping to a reusable piece of code, almost as if it was inlined at the call site, and the returning.
 * One argument can be passed in RDI.
 *
 * The destination block must handle the stack just like a function would, e.g. it should assume that at the beginning, the top of the
 * stack holds the return address.
 *
 * IMPORTANT: Values of all GPRs immediately after returning from the call MUST be the same as their values right before the call!
 * If this assumption is not satisfied, VERY BAD things will happen. This is because the instruction declares that it does not write any
 * registers so that it can act as an opaque step.
 *
 * @param label The label to call
 */
data class RawCall(val label: BlockLabel) : InstructionTemplates.FixedRegistersInstruction() {
    override val registersRead: Set<Register> =
        setOf(
            Register.FixedRegister(HardwareRegister.RDI),
            Register.FixedRegister(HardwareRegister.RSP),
            Register.FixedRegister(HardwareRegister.RSP),
        )

    override val registersWritten: Set<Register> = emptySet()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String = "call ${label.name}"
}

// Utility class to make asm a bit more readable.
data class Comment(private val comment: String) : InstructionTemplates.FixedRegistersInstruction() {
    override val registersRead: Set<Register> = emptySet()
    override val registersWritten: Set<Register> = emptySet()

    // This class is not marked as noop, as we do not want to remove it.
    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String = "; $comment"
}

data class Ret(val resultSize: Int) : InstructionTemplates.FixedRegistersInstruction() {
    override val registersRead: Set<Register> =
        (
            setOf(
                HardwareRegister.RSP,
            ) + REGISTER_RETURN_ORDER.take(resultSize) + SystemVAMD64CallConvention.preservedRegisters()
        ).map(Register::FixedRegister).toSet()
    override val registersWritten = setOf<Register>()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "ret"
}

data class LocalLabel(val label: BlockLabel) : InstructionTemplates.FixedRegistersInstruction() {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf()

    override fun isNoop(hardwareRegisterMapping: HardwareRegisterMapping, usedLocalLabels: Set<BlockLabel>): Boolean =
        label !in usedLocalLabels

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = ".${label.name}:"
}

data class Label(val label: BlockLabel) : InstructionTemplates.FixedRegistersInstruction() {
    override val registersRead: Set<Register> = setOf()
    override val registersWritten: Set<Register> = setOf()

    override fun toAsm(hardwareRegisterMapping: HardwareRegisterMapping) = "${label.name}:"
}
