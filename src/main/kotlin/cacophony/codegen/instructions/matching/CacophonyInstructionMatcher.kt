package cacophony.codegen.instructions.matching

import cacophony.codegen.BlockLabel
import cacophony.controlflow.*

class CacophonyInstructionMatcher : InstructionMatcher {
    private val innerInstructionMatcher =
        InstructionMatcherImpl(
            cacophony.codegen.patterns.cacophonyPatterns.valuePatterns,
            cacophony.codegen.patterns.cacophonyPatterns.sideEffectPatterns,
            cacophony.codegen.patterns.cacophonyPatterns.conditionPatterns,
        )

    override fun findMatchesForValue(node: CFGNode, destinationRegister: Register): Set<Match> {
        return innerInstructionMatcher.findMatchesForValue(node, destinationRegister)
    }

    override fun findMatchesForSideEffects(node: CFGNode): Set<Match> {
        return innerInstructionMatcher.findMatchesForSideEffects(node)
    }

    override fun findMatchesForCondition(node: CFGNode, destinationLabel: BlockLabel, jumpIf: Boolean): Set<Match> {
        return innerInstructionMatcher.findMatchesForCondition(node, destinationLabel, jumpIf)
    }
}
