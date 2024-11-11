package cacophony.controlflow.generation

import cacophony.semantic.FunctionAnalysisResult
import cacophony.semantic.UseTypeAnalysisResult
import cacophony.semantic.syntaxtree.Expression

internal class SideEffectAnalyzer(
    private val analyzedFunctions: FunctionAnalysisResult,
    private val analyzedUseTypes: UseTypeAnalysisResult,
) {
    fun hasClashingSideEffects(
        e1: Expression,
        e2: Expression,
    ): Boolean {
        TODO() // use analysedUseTypes to determine if variable uses clash
    }

    fun hasSideEffects(expression: Expression): Boolean = TODO()
}
