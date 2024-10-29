package cacophony.semantic

import cacophony.grammar.syntaxtree.Expression
import cacophony.utils.Diagnostics

fun checkTypes(
    ast: Expression.Subsequent,
    diagnostics: Diagnostics,
    nr: NameResolutionResult,
): TypeCheckingResult {
    TODO()
}

class Type {
    // TODO: Define Type class
}

typealias TypeCheckingResult = Map<Expression, Type>
