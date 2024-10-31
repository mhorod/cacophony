package cacophony.semantic

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Expression
import cacophony.semantic.syntaxtree.Type
import cacophony.utils.Diagnostics

fun checkTypes(
    ast: AST,
    diagnostics: Diagnostics,
    resolvedVariables: ResolvedVariables,
): TypeCheckingResult {
    TODO()
}

typealias TypeCheckingResult = Map<Expression, Type>
