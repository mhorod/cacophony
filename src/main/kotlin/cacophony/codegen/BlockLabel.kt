package cacophony.codegen

import cacophony.semantic.syntaxtree.Definition

data class BlockLabel(val name: String)

fun functionBodyLabel(function: Definition.FunctionDeclaration): BlockLabel = BlockLabel(function.getLabel())
