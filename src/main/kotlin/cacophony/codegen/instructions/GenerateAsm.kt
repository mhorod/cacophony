package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.cacophonyInstructions.InstructionTemplates
import cacophony.codegen.instructions.cacophonyInstructions.Label
import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.registers.RegisterAllocation

fun generateAsm(block: BasicBlock, registerAllocation: RegisterAllocation, usedLocalLabels: Set<BlockLabel>): String =
    block
        .instructions()
        .filterNot { it.isNoop(registerAllocation.successful, usedLocalLabels) }
        .joinToString("\n") { it.toAsm(registerAllocation.successful) }

fun generateAsm(func: BlockLabel, blocks: LoweredCFGFragment, registerAllocation: RegisterAllocation): String {
    val usedLocalLabels =
        blocks.flatMap {
            it.instructions()
        }.filterIsInstance<InstructionTemplates.JccInstruction>().map { it.label }.toSet()

    return Label(func).toAsm(registerAllocation.successful) + "\n" + (if (func.name == "main") "global main\n" else "") +
        blocks.map { generateAsm(it, registerAllocation, usedLocalLabels) }.filter { it.isNotBlank() }.joinToString("\n")
}
