package cacophony.semantic

import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.VariableUse
import cacophony.utils.Diagnostics

fun resolveOverloads(
    ast: Block,
    diagnostics: Diagnostics,
    nr: NameResolutionResult,
): ResolvedVariables {
    TODO()
}

typealias ResolvedVariables = Map<VariableUse, Definition>
