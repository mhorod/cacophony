package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.MemoryAddress
import cacophony.codegen.instructions.RegisterByte
import cacophony.codegen.instructions.cacophonyInstructions.CacophonyInstructions
import cacophony.codegen.patterns.SlotFill
import cacophony.controlflow.ConstantLabel
import cacophony.controlflow.Register
import cacophony.controlflow.RegisterLabel
import cacophony.controlflow.ValueLabel

class InstructionBuilder(val slotFill: SlotFill) {
    private val instructions = mutableListOf<Instruction>()

    fun instructions(): List<Instruction> = instructions.toList()

    fun mov(destination: Register, source: Register) {
        instructions.add(CacophonyInstructions.MovRegReg(destination, source))
    }

    fun mov(destination: Register, value: Int) {
        instructions.add(CacophonyInstructions.MovRegImm(destination, value))
    }

    fun mov(destination: Register, label: ValueLabel) = mov(destination, slotFill.valueFill.getValue(label))

    fun mov(destination: Register, memory: MemoryAddress) {
        instructions.add(CacophonyInstructions.MovRegMem(destination, memory))
    }

    fun mov(memory: MemoryAddress, source: Register) {
        instructions.add(CacophonyInstructions.MovMemReg(memory, source))
    }

    fun add(destination: Register, source: Register) {
        instructions.add(CacophonyInstructions.AddRegReg(destination, source))
    }

    fun add(destination: Register, value: Int) {
        instructions.add(CacophonyInstructions.AddRegImm(destination, value))
    }

    fun sub(destination: Register, source: Register) {
        instructions.add(CacophonyInstructions.SubRegReg(destination, source))
    }

    fun sub(destination: Register, value: Int) {
        instructions.add(CacophonyInstructions.SubRegImm(destination, value))
    }

    fun imul(destination: Register, source: Register) {
        instructions.add(CacophonyInstructions.IMulRegReg(destination, source))
    }

    fun cqo() {
        instructions.add(CacophonyInstructions.Cqo())
    }

    fun idiv(source: Register) {
        instructions.add(CacophonyInstructions.IDiv(source))
    }

    fun xor(destination: Register, source: Register) {
        instructions.add(CacophonyInstructions.XorRegReg(destination, source))
    }

    fun xor(destination: Register, value: Int) {
        instructions.add(CacophonyInstructions.XorRegImm(destination, value))
    }

    fun test(lhs: Register, rhs: Register) {
        instructions.add(CacophonyInstructions.TestRegReg(lhs, rhs))
    }

    fun push(source: Register) {
        instructions.add(CacophonyInstructions.PushReg(source))
    }

    fun pop(destination: Register) {
        instructions.add(CacophonyInstructions.Pop(destination))
    }

    fun cmp(lhs: Register, rhs: Register) {
        instructions.add(CacophonyInstructions.CmpRegReg(lhs, rhs))
    }

    fun je(label: BlockLabel) {
        instructions.add(CacophonyInstructions.Je(label))
    }

    fun jne(label: BlockLabel) {
        instructions.add(CacophonyInstructions.Jne(label))
    }

    fun jl(label: BlockLabel) {
        instructions.add(CacophonyInstructions.Jl(label))
    }

    fun jle(label: BlockLabel) {
        instructions.add(CacophonyInstructions.Jle(label))
    }

    fun jg(label: BlockLabel) {
        instructions.add(CacophonyInstructions.Jg(label))
    }

    fun jge(label: BlockLabel) {
        instructions.add(CacophonyInstructions.Jge(label))
    }

    fun jz(label: BlockLabel) {
        instructions.add(CacophonyInstructions.Jz(label))
    }

    fun jnz(label: BlockLabel) {
        instructions.add(CacophonyInstructions.Jnz(label))
    }

    fun sete(registerByte: RegisterByte) {
        instructions.add(CacophonyInstructions.Sete(registerByte))
    }

    fun setne(registerByte: RegisterByte) {
        instructions.add(CacophonyInstructions.Setne(registerByte))
    }

    fun setl(registerByte: RegisterByte) {
        instructions.add(CacophonyInstructions.Setl(registerByte))
    }

    fun setle(registerByte: RegisterByte) {
        instructions.add(CacophonyInstructions.Setle(registerByte))
    }

    fun setg(registerByte: RegisterByte) {
        instructions.add(CacophonyInstructions.Setg(registerByte))
    }

    fun setge(registerByte: RegisterByte) {
        instructions.add(CacophonyInstructions.Setge(registerByte))
    }

    fun movzx(register: Register, registerByte: RegisterByte) {
        instructions.add(CacophonyInstructions.MovzxReg64Reg8(register, registerByte))
    }

    fun ret() {
        TODO()
    }

    fun byte(register: Register) = RegisterByte(register)

    fun mem(base: Register) = MemoryAddress(base, null, null, null)

    fun reg(register: RegisterLabel) = slotFill.registerFill.getValue(register)

    fun reg(register: ValueLabel) = slotFill.valueFill.getValue(register)

    fun const(constant: ConstantLabel) = slotFill.constantFill.getValue(constant).value
}

fun instructions(slotFill: SlotFill, init: InstructionBuilder.() -> Unit): List<Instruction> {
    val builder = InstructionBuilder(slotFill)
    builder.init()
    return builder.instructions()
}
