package cacophony.semantic

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.utils.Diagnostics

fun generateCallGraph(
    ast: AST,
    diagnostics: Diagnostics,
    resolvedVariables: ResolvedVariables,
    types: TypeCheckingResult,
): CallGraph {
    TODO()
}

typealias CallGraph = Map<Definition.FunctionDeclaration, Set<Definition.FunctionDeclaration>>
