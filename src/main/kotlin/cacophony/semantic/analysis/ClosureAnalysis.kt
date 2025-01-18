package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.semantic.syntaxtree.LambdaExpression

/**
 * Set of captured variables
 */
typealias ClosureAnalysisResult = Map<LambdaExpression, Set<Variable>>

fun analyseClosures(): ClosureAnalysisResult {
    return emptyMap() // TODO - implement closure analysis
}
