package cacophony.codegen.instructions.matching

import cacophony.codegen.BlockLabel
import cacophony.controlflow.*

class CacophonyInstructionMatcher : InstructionMatcher {
    private val innerInstructionMatcher =
        InstructionMatcherImpl(
            cacophony.codegen.patterns.cacophonyPatterns.valuePatterns,
            cacophony.codegen.patterns.cacophonyPatterns.sideEffectPatterns,
            cacophony.codegen.patterns.cacophonyPatterns.conditionPatterns,
            cacophony.codegen.patterns.cacophonyPatterns.noTemporaryRegistersPatterns,
        )

    override fun findMatchesForValue(node: CFGNode, destinationRegister: Register): Set<Match> =
        innerInstructionMatcher.findMatchesForValue(node, destinationRegister)

    override fun findMatchesForSideEffects(node: CFGNode): Set<Match> = innerInstructionMatcher.findMatchesForSideEffects(node)

    override fun findMatchesWithoutTemporaryRegisters(node: CFGNode): Set<Match> =
        innerInstructionMatcher.findMatchesWithoutTemporaryRegisters(node)

    override fun findMatchesForCondition(node: CFGNode, destinationLabel: BlockLabel, jumpIf: Boolean): Set<Match> =
        innerInstructionMatcher.findMatchesForCondition(node, destinationLabel, jumpIf)
}
