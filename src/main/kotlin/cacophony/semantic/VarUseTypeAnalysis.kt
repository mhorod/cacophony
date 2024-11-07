package cacophony.semantic

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression

typealias UseTypeAnalysisResult = Map<Expression, Map<Definition, VariableUseType>>

fun analyzeVarUseTypes(
    ast: AST,
    resolvedVariables: ResolvedVariables,
    callGraph: CallGraph,
): UseTypeAnalysisResult {
    TODO()
}
