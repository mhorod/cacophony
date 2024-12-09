package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.cacophonyInstructions.Comment
import cacophony.codegen.instructions.cacophonyInstructions.Label
import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.registers.RegisterAllocation
import cacophony.controlflow.CFGNode

fun generateAsm(block: BasicBlock, registerAllocation: RegisterAllocation): String =
    block
        .instructions()
//        .filterNot { it.isNoop(registerAllocation.successful) }
//        .map { it.toAsm(registerAllocation.successful) }
        .map {
            val asm = it.toAsm(registerAllocation.successful)
            if (it.isNoop(registerAllocation.successful))
                Comment(asm).toAsm(registerAllocation.successful)
            else
                asm
        }
        .joinToString("\n")

fun generateAsm(func: BlockLabel, blocks: LoweredCFGFragment, registerAllocation: RegisterAllocation) =
    Label(func).toAsm(registerAllocation.successful) + "\n" + (if (func.name == "main") "global main\n" else "") +
        blocks.map { generateAsm(it, registerAllocation) }.joinToString("\n")
