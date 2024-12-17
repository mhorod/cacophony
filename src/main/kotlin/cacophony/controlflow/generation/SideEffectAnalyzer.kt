package cacophony.controlflow.generation

import cacophony.controlflow.Variable
import cacophony.semantic.analysis.UseTypeAnalysisResult
import cacophony.semantic.analysis.VariableUseType
import cacophony.semantic.syntaxtree.Expression

internal class SideEffectAnalyzer(
    private val analyzedUseTypes: UseTypeAnalysisResult,
) {
    fun hasClashingSideEffects(e1: Expression, e2: Expression): Boolean {
        val firstWrites = writtenVariables(e1)
        val firstUses = analyzedUseTypes.getValue(e1).keys

        val secondWrites = writtenVariables(e2)
        val secondUses = analyzedUseTypes.getValue(e2).keys

        return (firstUses intersect secondWrites).isNotEmpty() || (secondUses intersect firstWrites).isNotEmpty()
    }

    private fun writtenVariables(expression: Expression): Set<Variable> =
        analyzedUseTypes
            .getValue(expression)
            .filter { it.value == VariableUseType.WRITE || it.value == VariableUseType.READ_WRITE }
            .map { it.key }
            .toSet()
}
