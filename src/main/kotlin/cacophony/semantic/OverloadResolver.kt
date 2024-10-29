package cacophony.semantic

import cacophony.grammar.syntaxtree.Expression
import cacophony.utils.Diagnostics

fun resolveOverloads(
    ast: Expression.Subsequent,
    diagnostics: Diagnostics,
    nr: NameResolutionResult,
    types: TypeCheckingResult,
): ResolvedVariables {
    TODO()
}

typealias ResolvedVariables = Map<Expression.VariableUse, Expression.Definition>
