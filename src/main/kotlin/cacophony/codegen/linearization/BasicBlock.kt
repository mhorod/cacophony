package cacophony.codegen.linearization

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction

class BasicBlock(
    val label: BlockLabel,
    val instructions: List<Instruction>,
    val successors: Set<BasicBlock>,
    val predecessors: MutableSet<BasicBlock>,
)
