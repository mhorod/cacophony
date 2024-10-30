package cacophony.semantic

import cacophony.semantic.syntaxtree.Definition

interface OverloadSet {
    operator fun get(arity: Int): Definition.FunctionDeclaration?

    fun toMap(): Map<Int, Definition.FunctionDeclaration>

    fun withDeclaration(
        arity: Int,
        declaration: Definition.FunctionDeclaration,
    ): OverloadSet
}
