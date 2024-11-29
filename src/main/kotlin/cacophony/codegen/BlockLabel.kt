package cacophony.codegen

import cacophony.semantic.syntaxtree.Definition

data class BlockLabel(val name: String)

object FunctionBodyLabel {
    private val functionToId = mutableMapOf<Definition.FunctionDeclaration, Int>()
    private var lastId = 0

    operator fun invoke(function: Definition.FunctionDeclaration): BlockLabel {
        if (!functionToId.containsKey(function)) {
            functionToId[function] = lastId++
        }

        return BlockLabel(
            when (function.identifier) {
                "<program>" -> "start"
                else -> "${function.identifier}_${function.arguments.size}_id${functionToId[function]}"
            },
        )
    }
}
