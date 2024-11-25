package cacophony.codegen.linearization

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction

interface BasicBlock {
    fun label(): BlockLabel

    fun instructions(): List<Instruction>

    fun successors(): Set<BasicBlock>

    fun predecessors(): Set<BasicBlock>
}
