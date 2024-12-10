package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Assignable
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.types.TypeCheckingResult

data class VariablesMap(
    val lvalues: Map<Assignable, Variable>,
    val definitions: Map<Definition.VariableDeclaration, Variable>,
)

fun createVariablesMap(ast: AST, resolvedVariables: ResolvedVariables, types: TypeCheckingResult): VariablesMap {
    TODO()
}
