package cacophony.codegen

import cacophony.semantic.syntaxtree.Definition

data class BlockLabel(val name: String) {
    companion object {
        val cleanReferences = BlockLabel("clean_refs")

        val builtins = listOf(cleanReferences)
    }
}

fun functionBodyLabel(function: Definition.FunctionDeclaration): BlockLabel = BlockLabel(function.getLabel())
