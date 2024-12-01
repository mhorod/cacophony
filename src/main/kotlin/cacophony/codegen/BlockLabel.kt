package cacophony.codegen

import cacophony.semantic.syntaxtree.Definition
import kotlin.math.absoluteValue

data class BlockLabel(val name: String)

fun functionBodyLabel(function: Definition.FunctionDeclaration): BlockLabel {
    return BlockLabel(
        when (function.identifier) {
            "<program>" -> "start"
            else -> "${function.identifier}_${function.arguments.size}_${function.hashCode().absoluteValue}"
        },
    )
}
