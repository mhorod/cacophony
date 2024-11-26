package cacophony.codegen.instructions

import cacophony.codegen.registers.RegisterAllocation

fun generateAsm(instruction: Instruction, registerAllocation: RegisterAllocation): String {
    return instruction.toAsm(registerAllocation.successful)
}

fun generateAsm(instructions: List<Instruction>, registerAllocation: RegisterAllocation): String {
    return instructions.joinToString("\n") { generateAsm(it, registerAllocation) }
}
