package cacophony.semantic.names

import cacophony.semantic.syntaxtree.Definition

interface OverloadSet {
    operator fun get(arity: Int): Definition.FunctionDefinition?

    fun toMap(): Map<Int, Definition.FunctionDefinition>

    fun withDeclaration(arity: Int, declaration: Definition.FunctionDefinition): OverloadSet
}
