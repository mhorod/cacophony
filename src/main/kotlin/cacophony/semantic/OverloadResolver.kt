package cacophony.semantic

import cacophony.grammar.syntaxtree.Definition
import cacophony.grammar.syntaxtree.Subsequent
import cacophony.grammar.syntaxtree.VariableUse
import cacophony.utils.Diagnostics

fun resolveOverloads(
    ast: Subsequent,
    diagnostics: Diagnostics,
    nr: NameResolutionResult,
): ResolvedVariables {
    TODO()
}

typealias ResolvedVariables = Map<VariableUse, Definition>
