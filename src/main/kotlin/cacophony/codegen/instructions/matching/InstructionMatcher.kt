package cacophony.codegen.instructions.matching

import cacophony.codegen.BlockLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register

interface InstructionMatcher {
    fun findMatchesForValue(node: CFGNode, destinationRegister: Register): Set<Match>

    fun findMatchesForSideEffects(node: CFGNode): Set<Match>

    fun findMatchesForCondition(node: CFGNode, destinationLabel: BlockLabel): Set<Match>
}
