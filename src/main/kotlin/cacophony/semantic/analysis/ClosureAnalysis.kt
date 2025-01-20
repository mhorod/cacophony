package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.semantic.syntaxtree.LambdaExpression

/**
 * Set of captured variables
 */
typealias ClosureAnalysisResult = Map<LambdaExpression, Set<Variable>>

fun analyseClosures(escapeAnalysis: EscapeAnalysisResult /*, lambdaAnalysis: LambdaAnalysisResult */): ClosureAnalysisResult {
//    return lambdaAnalysis.mapValues { (_, analysis) ->
//        analysis.declaredVariables().map { it.origin }.filter { escapeAnalysis.contains(it) }.toSet()
//    }
    return emptyMap()
}
