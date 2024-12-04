package cacophony.codegen

import cacophony.semantic.syntaxtree.Definition
import kotlin.math.absoluteValue

data class BlockLabel(val name: String)

fun functionBodyLabel(function: Definition.FunctionDefinition): BlockLabel =
    BlockLabel(
        when (function.identifier) {
            "<program>" -> "main"
            else -> "${function.identifier}_${function.arguments.size}_${function.hashCode().absoluteValue}"
        },
    )
