package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.controlflow.CFGNode

interface InstructionCovering {
    fun coverWithInstructions(node: CFGNode): List<Instruction>

    fun coverWithInstructionsAndJump(node: CFGNode, label: BlockLabel): List<Instruction>
}
