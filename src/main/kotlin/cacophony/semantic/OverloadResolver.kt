package cacophony.semantic

import cacophony.grammar.syntaxtree.Expression
import cacophony.utils.Diagnostics

fun resolveOverloads(
    ast: Expression,
    diagnostics: Diagnostics,
    nr: NameResolutionResult,
    ty: TypeCheckingResult,
): ResolvedVariables {
    TODO()
}

typealias ResolvedVariables = Map<Expression.VariableUse, Expression.Definition>
