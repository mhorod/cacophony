package cacophony.semantic.types

import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression

data class TypeCheckingResult(
    val expressionTypes: Map<Expression, TypeExpr>,
    val definitionTypes: Map<Definition, TypeExpr>,
)
