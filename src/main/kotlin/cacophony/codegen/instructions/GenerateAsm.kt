package cacophony.codegen.instructions

import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.registers.RegisterAllocation

fun generateAsm(block: BasicBlock, registerAllocation: RegisterAllocation): String =
    "${block.label()}:\n" +
        block
            .instructions()
            .filterNot(Instruction::isNoop)
            .map { it.toAsm(registerAllocation.successful) }
            .filter(String::isNotEmpty)
            .joinToString("\n")

fun generateAsm(func: String, blocks: LoweredCFGFragment, registerAllocation: RegisterAllocation) =
    when (func) {
        "<program>" -> "main:\nglobal main"
        else -> "$func:"
    } + "\n" +
        blocks
            .map {
                generateAsm(it, registerAllocation)
            }.joinToString("\n")
