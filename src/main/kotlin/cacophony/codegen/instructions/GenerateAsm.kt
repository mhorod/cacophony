package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.registers.RegisterAllocation

fun generateAsm(block: BasicBlock, registerAllocation: RegisterAllocation): String =
    ".${block.label().name}:\n" +
        block
            .instructions()
            .filterNot { it.isNoop(registerAllocation.successful) }
            .map { it.toAsm(registerAllocation.successful) }
            .filter(String::isNotEmpty)
            .joinToString("\n")

fun generateAsm(func: BlockLabel, blocks: LoweredCFGFragment, registerAllocation: RegisterAllocation) =
    "${func.name}:\n" + (func.name.takeIf { it == "main" }?.let { "global main\n" } ?: "") +
        blocks
            .map {
                generateAsm(it, registerAllocation)
            }.joinToString("\n")
