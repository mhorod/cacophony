package cacophony.codegen

import cacophony.semantic.syntaxtree.LambdaExpression

data class BlockLabel(val name: String) {
    companion object {
        val cleanReferences = BlockLabel("clean_refs")

        val builtins = listOf(cleanReferences)
    }
}

fun functionBodyLabel(function: LambdaExpression): BlockLabel = BlockLabel(function.getLabel())
