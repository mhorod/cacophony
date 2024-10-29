package cacophony.semantic

import cacophony.grammar.syntaxtree.Expression
import cacophony.utils.Diagnostics

fun generateCallGraph(
    ast: Expression.Subsequent,
    diagnostics: Diagnostics,
    resolvedVariables: ResolvedVariables,
    types: TypeCheckingResult,
): CallGraph {
    TODO()
}

typealias CallGraph = Map<Expression.Definition.FunctionDeclaration, Set<Expression.Definition.FunctionDeclaration>>
