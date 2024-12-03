package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.cacophonyInstructions.Label
import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.registers.RegisterAllocation

fun generateAsm(block: BasicBlock, registerAllocation: RegisterAllocation): String =
    block
        .instructions()
        .filterNot { it.isNoop(registerAllocation.successful) }
        .joinToString("\n") { it.toAsm(registerAllocation.successful) }

fun generateAsm(func: BlockLabel, blocks: LoweredCFGFragment, registerAllocation: RegisterAllocation) =
    Label(func).toAsm(registerAllocation.successful) + "\n" + (if (func.name == "main") "global main\n" else "") +
        blocks.joinToString("\n") { generateAsm(it, registerAllocation) }
