package cacophony.semantic

import cacophony.grammar.syntaxtree.Expression

fun generateCallGraph(
    ast: Expression,
    resolvedVariables: ResolvedVariables,
    types: TypeCheckingResult,
): CallGraph {
    TODO()
}

typealias CallGraph = Map<Expression.Definition.FunctionDeclaration, Set<Expression.Definition.FunctionDeclaration>>
