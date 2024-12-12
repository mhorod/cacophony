package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.cacophonyInstructions.InstructionTemplates
import cacophony.codegen.instructions.cacophonyInstructions.Label
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.registers.RegisterAllocation

fun generateAsm(func: BlockLabel, blocks: LoweredCFGFragment, registerAllocation: RegisterAllocation): String {
    val usedLocalLabels =
        blocks.flatMap {
            it.instructions()
        }.filterIsInstance<InstructionTemplates.JccInstruction>().map { it.label }.toSet()

    val instructions = listOf(Label(func)) + blocks.flatMap { it.instructions() }

    return instructions.filterNot { it.isNoop(registerAllocation.successful, usedLocalLabels) }
        .joinToString("\n") { it.toAsm(registerAllocation.successful) }
}
