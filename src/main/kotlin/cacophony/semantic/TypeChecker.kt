package cacophony.semantic

import cacophony.grammar.syntaxtree.Expression
import cacophony.grammar.syntaxtree.Subsequent
import cacophony.utils.Diagnostics

fun checkTypes(
    ast: Subsequent,
    diagnostics: Diagnostics,
    resolvedVariables: ResolvedVariables,
): TypeCheckingResult {
    TODO()
}

class Type {
    // TODO: Define Type class
}

typealias TypeCheckingResult = Map<Expression, Type>
