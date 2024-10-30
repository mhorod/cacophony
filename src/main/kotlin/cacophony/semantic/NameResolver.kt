package cacophony.semantic

import cacophony.grammar.syntaxtree.Definition
import cacophony.grammar.syntaxtree.Subsequent
import cacophony.grammar.syntaxtree.VariableUse
import cacophony.utils.Diagnostics

fun resolveNames(
    ast: Subsequent,
    diagnostics: Diagnostics,
): NameResolutionResult {
    TODO()
}

typealias NameResolutionResult = Map<VariableUse, ResolvedName>

sealed interface ResolvedName {
    class Variable(
        val def: Definition.VariableDeclaration,
    ) : ResolvedName

    class Argument(
        val def: Definition.FunctionArgument,
    ) : ResolvedName

    class Function(
        val def: OverloadSet,
    ) : ResolvedName
}
