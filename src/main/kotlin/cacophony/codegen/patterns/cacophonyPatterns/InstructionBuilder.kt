package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.MemoryAddress
import cacophony.codegen.instructions.RegisterByte
import cacophony.codegen.patterns.SlotFill
import cacophony.controlflow.ConstantLabel
import cacophony.controlflow.Register
import cacophony.controlflow.RegisterLabel
import cacophony.controlflow.ValueLabel

class InstructionBuilder(val slotFill: SlotFill) {
    private val instructions = mutableListOf<Instruction>()

    fun instructions(): List<Instruction> = instructions.toList()

    fun mov(destination: Register, source: Register) {
        TODO()
    }

    fun mov(destination: Register, value: Int) {
        TODO()
    }

    fun mov(destination: Register, label: ValueLabel) = mov(destination, slotFill.valueFill.getValue(label))

    fun mov(destination: Register, memory: MemoryAddress) {
        TODO()
    }

    fun mov(memory: MemoryAddress, source: Register) {
        TODO()
    }

    fun add(destination: Register, source: Register) {
        TODO()
    }

    fun add(destination: Register, value: Int) {
        TODO()
    }

    fun sub(destination: Register, source: Register) {
        TODO()
    }

    fun sub(destination: Register, value: Int) {
        TODO()
    }

    fun imul(destination: Register, source: Register) {
        TODO()
    }

    fun cqo() {
        TODO()
    }

    fun idiv(source: Register) {
        TODO()
    }

    fun xor(destination: Register, source: Register) {
        TODO()
    }

    fun xor(destination: Register, value: Int) {
        TODO()
    }

    fun test(lhs: Register, rhs: Register) {
        TODO()
    }

    fun push(source: Register) {
        TODO()
    }

    fun pop(destination: Register) {
        TODO()
    }

    fun cmp(lhs: Register, rhs: Register) {
        TODO()
    }

    fun je(label: BlockLabel) {
        TODO()
    }

    fun jne(label: BlockLabel) {
        TODO()
    }

    fun jl(label: BlockLabel) {
        TODO()
    }

    fun jle(label: BlockLabel) {
        TODO()
    }

    fun jg(label: BlockLabel) {
        TODO()
    }

    fun jge(label: BlockLabel) {
        TODO()
    }

    fun jz(label: BlockLabel) {
        TODO()
    }

    fun jnz(label: BlockLabel) {
        TODO()
    }

    fun sete(registerByte: RegisterByte) {
        TODO()
    }

    fun setne(registerByte: RegisterByte) {
        TODO()
    }

    fun setl(registerByte: RegisterByte) {
        TODO()
    }

    fun setle(registerByte: RegisterByte) {
        TODO()
    }

    fun setg(registerByte: RegisterByte) {
        TODO()
    }

    fun setge(registerByte: RegisterByte) {
        TODO()
    }

    fun movzx(register: Register, registerByte: RegisterByte) {
        TODO()
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
