package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.matching.InstructionMatcher
import cacophony.controlflow.CFGNode

class CacophonyInstructionCovering(val instructionMatcher: InstructionMatcher) : InstructionCovering {
    override fun coverWithInstructions(node: CFGNode): List<Instruction> {
        TODO("Not yet implemented")
    }

    override fun coverWithInstructionsAndJump(node: CFGNode, label: BlockLabel): List<Instruction> {
        TODO("Not yet implemented")
    }
}
