package cacophony.semantic.analysis

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.LambdaExpression

/**
 * Distinction between functions that are semantically closures and static functions
 *
 * @property closures Set of `LambdaExpression` that are semantically closures
 * @property staticFunctions Set of `LambdaExpression` that are semantically static
 */
class ClosureAnalysisResult(val closures: Set<LambdaExpression>, val staticFunctions: Set<LambdaExpression>)

// TODO: Implement distinction between closures and static functions
//  Static functions are the ones that:
//      - are syntactically static i.e. let f = [] => ();
//      - don't escape i.e. it makes sense to use static link
fun analyseClosures(ast: AST, escapeAnalysis: EscapeAnalysisResult): ClosureAnalysisResult {
    val namedFunctionInfo = getNamedFunctions(ast)
    return ClosureAnalysisResult(emptySet(), emptySet())
}
