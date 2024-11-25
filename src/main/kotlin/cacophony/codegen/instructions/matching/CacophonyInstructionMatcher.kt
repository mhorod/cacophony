package cacophony.codegen.instructions.matching

import cacophony.codegen.BlockLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register

class CacophonyInstructionMatcher : InstructionMatcher {
    override fun findMatchesForValue(node: CFGNode, destinationRegister: Register): Set<Match> {
        TODO("Not yet implemented")
    }

    override fun findMatchesForSideEffects(node: CFGNode): Set<Match> {
        TODO("Not yet implemented")
    }

    override fun findMatchesForCondition(node: CFGNode, destinationLabel: BlockLabel, jumpIf: Boolean): Set<Match> {
        TODO("Not yet implemented")
    }
}
