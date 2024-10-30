package cacophony.semantic

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.VariableUse
import cacophony.utils.Diagnostics

fun resolveOverloads(
    ast: AST,
    diagnostics: Diagnostics,
    nr: NameResolutionResult,
): ResolvedVariables {
    TODO()
}

typealias ResolvedVariables = Map<VariableUse, Definition>
