package cacophony.semantic

import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Expression
import cacophony.utils.Diagnostics

fun checkTypes(
    ast: Block,
    diagnostics: Diagnostics,
    resolvedVariables: ResolvedVariables,
): TypeCheckingResult {
    TODO()
}

class Type {
    // TODO: Define Type class
}

typealias TypeCheckingResult = Map<Expression, Type>
