package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.cacophonyInstructions.InstructionTemplates
import cacophony.codegen.instructions.cacophonyInstructions.Label
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.registers.RegisterAllocation
import cacophony.semantic.syntaxtree.Definition

fun generateAsm(func: BlockLabel, blocks: LoweredCFGFragment, registerAllocation: RegisterAllocation): String {
    val usedLocalLabels =
        blocks.flatMap {
            it.instructions()
        }.filterIsInstance<InstructionTemplates.JccInstruction>().map { it.label }.toSet()

    val instructions = listOf(Label(func)) + blocks.flatMap { it.instructions() }

    return instructions.filterNot { it.isNoop(registerAllocation.successful, usedLocalLabels) }
        .joinToString("\n") { it.toAsm(registerAllocation.successful) }
}

fun generateAsmPreamble(foreignFunctions: Set<Definition.ForeignFunctionDeclaration>, objectOutlines: List<String>): String =
    (
        listOf("SECTION .data") +
            foreignFunctions.map { "extern ${it.identifier}" } +
            BlockLabel.builtins.map { "extern ${it.name}" } +
            objectOutlines +
            listOf("global main", "SECTION .text")
    ).joinToString("\n")
