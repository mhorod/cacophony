package cacophony.semantic

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Expression
import cacophony.utils.Diagnostics

fun checkTypes(
    ast: AST,
    diagnostics: Diagnostics,
    resolvedVariables: ResolvedVariables,
): TypeCheckingResult {
    TODO()
}

class Type {
    // TODO: Define Type class
}

typealias TypeCheckingResult = Map<Expression, Type>
