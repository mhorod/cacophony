package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.semantic.syntaxtree.Expression

data class ClosureVariableInfo(val offset: Int)
typealias ClosureAnalysisResult = Map<Expression, Map<Variable, ClosureVariableInfo>>

fun analyseClosures(): ClosureAnalysisResult {
    return emptyMap() // TODO - implement closure analysis
}
