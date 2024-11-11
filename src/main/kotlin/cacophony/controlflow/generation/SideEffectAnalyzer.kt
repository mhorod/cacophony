package cacophony.controlflow.generation

import cacophony.semantic.UseTypeAnalysisResult
import cacophony.semantic.VariableUseType
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression

internal class SideEffectAnalyzer(
    private val analyzedUseTypes: UseTypeAnalysisResult,
) {
    fun hasClashingSideEffects(
        e1: Expression,
        e2: Expression,
    ): Boolean {
        val firstWrites = writtenVariables(e1)
        val firstUses = analyzedUseTypes[e1]!!.keys

        val secondWrites = writtenVariables(e2)
        val secondUses = analyzedUseTypes[e2]!!.keys

        return (firstUses intersect secondWrites).isNotEmpty() || (secondUses intersect firstWrites).isNotEmpty()
    }

    fun hasSideEffects(expression: Expression): Boolean = writtenVariables(expression).isNotEmpty()

    private fun writtenVariables(expression: Expression): Set<Definition> =
        analyzedUseTypes[expression]!!.entries
            .filter { it.value == VariableUseType.WRITE || it.value == VariableUseType.READ_WRITE }
            .map { it.key }.toSet()
}
