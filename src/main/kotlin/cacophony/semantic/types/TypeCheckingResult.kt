package cacophony.semantic.types

import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression
import cacophony.semantic.syntaxtree.VariableUse

typealias ResolvedVariables = Map<VariableUse, Definition>

data class TypeCheckingResult(
    val expressionTypes: Map<Expression, TypeExpr>,
    val definitionTypes: Map<Definition, TypeExpr>,
    val resolvedVariables: ResolvedVariables,
)
