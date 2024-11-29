package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.MemoryAddress
import cacophony.codegen.instructions.RegisterByte
import cacophony.codegen.instructions.cacophonyInstructions.*
import cacophony.codegen.patterns.SlotFill
import cacophony.controlflow.*

class InstructionBuilder(val slotFill: SlotFill) {
    private val instructions = mutableListOf<Instruction>()

    fun instructions(): List<Instruction> = instructions.toList()

    fun mov(destination: Register, source: Register) {
        instructions.add(MovRegReg(destination, source))
    }

    fun mov(destination: Register, value: Int) {
        instructions.add(MovRegImm(destination, value))
    }

    fun mov(destination: Register, label: ValueLabel) = mov(destination, slotFill.valueFill.getValue(label))

    fun mov(destination: Register, memory: MemoryAddress) {
        instructions.add(MovRegMem(destination, memory))
    }

    fun mov(memory: MemoryAddress, source: Register) {
        instructions.add(MovMemReg(memory, source))
    }

    fun add(destination: Register, source: Register) {
        instructions.add(AddRegReg(destination, source))
    }

    fun add(destination: Register, value: Int) {
        instructions.add(AddRegImm(destination, value))
    }

    fun sub(destination: Register, source: Register) {
        instructions.add(SubRegReg(destination, source))
    }

    fun sub(destination: Register, value: Int) {
        instructions.add(SubRegImm(destination, value))
    }

    fun imul(destination: Register, source: Register) {
        instructions.add(IMulRegReg(destination, source))
    }

    fun cqo() {
        instructions.add(Cqo())
    }

    fun idiv(source: Register) {
        instructions.add(IDiv(source))
    }

    fun xor(destination: Register, source: Register) {
        instructions.add(XorRegReg(destination, source))
    }

    fun xor(destination: Register, value: Int) {
        instructions.add(XorRegImm(destination, value))
    }

    fun test(lhs: Register, rhs: Register) {
        instructions.add(TestRegReg(lhs, rhs))
    }

    fun push(source: Register) {
        instructions.add(PushReg(source))
    }

    fun pop(destination: Register) {
        instructions.add(Pop(destination))
    }

    fun cmp(lhs: Register, rhs: Register) {
        instructions.add(CmpRegReg(lhs, rhs))
    }

    fun je(label: BlockLabel) {
        instructions.add(Je(label))
    }

    fun jne(label: BlockLabel) {
        instructions.add(Jne(label))
    }

    fun jl(label: BlockLabel) {
        instructions.add(Jl(label))
    }

    fun jle(label: BlockLabel) {
        instructions.add(Jle(label))
    }

    fun jg(label: BlockLabel) {
        instructions.add(Jg(label))
    }

    fun jge(label: BlockLabel) {
        instructions.add(Jge(label))
    }

    fun jz(label: BlockLabel) {
        instructions.add(Jz(label))
    }

    fun jnz(label: BlockLabel) {
        instructions.add(Jnz(label))
    }

    fun sete(registerByte: RegisterByte) {
        instructions.add(Sete(registerByte))
    }

    fun setne(registerByte: RegisterByte) {
        instructions.add(Setne(registerByte))
    }

    fun setl(registerByte: RegisterByte) {
        instructions.add(Setl(registerByte))
    }

    fun setle(registerByte: RegisterByte) {
        instructions.add(Setle(registerByte))
    }

    fun setg(registerByte: RegisterByte) {
        instructions.add(Setg(registerByte))
    }

    fun setge(registerByte: RegisterByte) {
        instructions.add(Setge(registerByte))
    }

    fun movzx(register: Register, registerByte: RegisterByte) {
        instructions.add(MovzxReg64Reg8(register, registerByte))
    }

    fun call(label: FunctionLabel) {
        instructions.add(
            Call(
                slotFill.functionFill.getValue(label).function
                    ?: error("Creating function body label of a pattern node"),
            ),
        )
    }

    fun ret() {
        instructions.add(Ret())
    }

    fun byte(register: Register) = RegisterByte(register)

    fun mem(base: Register) = MemoryAddress(base, null, null, null)

    fun memWithDisplacement(base: Register, displacement: Int) = MemoryAddress(base, null, null, displacement)

    fun reg(register: RegisterLabel) = slotFill.registerFill.getValue(register)

    fun reg(register: ValueLabel) = slotFill.valueFill.getValue(register)

    fun const(constant: ConstantLabel) = slotFill.constantFill.getValue(constant).value
}

fun instructions(slotFill: SlotFill, init: InstructionBuilder.() -> Unit): List<Instruction> {
    val builder = InstructionBuilder(slotFill)
    builder.init()
    return builder.instructions()
}
