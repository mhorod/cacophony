package cacophony.semantic.names

import cacophony.semantic.syntaxtree.Definition

interface OverloadSet {
    operator fun get(arity: Int): Definition?

    fun toMap(): Map<Int, Definition>

    fun withDeclaration(arity: Int, declaration: Definition): OverloadSet
}
