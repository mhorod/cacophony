package cacophony.semantic

import cacophony.grammar.syntaxtree.Expression

interface OverloadSet {
    operator fun get(arity: Int): Expression.Definition.FunctionDeclaration?

    fun withDeclaration(
        arity: Int,
        declaration: Expression.Definition.FunctionDeclaration,
    ): OverloadSet
}
