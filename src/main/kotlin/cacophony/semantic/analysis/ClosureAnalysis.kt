package cacophony.semantic.analysis

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.LambdaExpression

/**
 * Distinction between functions that are semantically closures and static functions
 *
 * @property closures Set of `LambdaExpression` that are semantically closures
 * @property staticFunctions Set of `LambdaExpression` that are semantically static
 */
data class ClosureAnalysisResult(val closures: Set<LambdaExpression>, val staticFunctions: Set<LambdaExpression>)

fun analyzeClosures(ast: AST, variablesMap: VariablesMap, escapeAnalysis: EscapeAnalysisResult): ClosureAnalysisResult {
    val functionIdentities = getFunctionIdentities(ast)
    val (dynamic, static) =
        functionIdentities
            .namedFunctions
            .entries
            .partition { (_, definition) -> escapeAnalysis.contains(variablesMap.definitions[definition]!!) }
            .let { (first, second) -> Pair(first.map { it.key }.toSet(), second.map { it.key }.toSet()) }
    return ClosureAnalysisResult(dynamic union functionIdentities.anonymousFunctions, static)
}
