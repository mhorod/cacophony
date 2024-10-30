package cacophony.semantic

import cacophony.grammar.syntaxtree.Definition
import cacophony.grammar.syntaxtree.Subsequent
import cacophony.utils.Diagnostics

fun generateCallGraph(
    ast: Subsequent,
    diagnostics: Diagnostics,
    resolvedVariables: ResolvedVariables,
    types: TypeCheckingResult,
): CallGraph {
    TODO()
}

typealias CallGraph = Map<Definition.FunctionDeclaration, Set<Definition.FunctionDeclaration>>
