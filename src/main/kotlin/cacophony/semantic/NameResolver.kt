package cacophony.semantic

import cacophony.grammar.syntaxtree.Expression
import cacophony.utils.Diagnostics

fun resolveNames(
    ast: Expression.Subsequent,
    diagnostics: Diagnostics,
): NameResolutionResult {
    TODO()
}

typealias NameResolutionResult = Map<Expression.VariableUse, ResolvedName>

sealed interface ResolvedName {
    class Variable(
        val def: Expression.Definition.VariableDeclaration,
    ) : ResolvedName

    class Argument(
        val def: Expression.Definition.FunctionArgument,
    ) : ResolvedName

    class Function(
        val def: OverloadSet,
    ) : ResolvedName
}
