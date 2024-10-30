package cacophony.semantic

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.VariableUse
import cacophony.utils.Diagnostics

fun resolveNames(
    ast: AST,
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
