package cacophony.codegen

import cacophony.semantic.syntaxtree.Definition
import kotlin.math.absoluteValue

data class BlockLabel(val name: String)

fun functionBodyLabel(function: Definition.FunctionDeclaration): BlockLabel =
    BlockLabel(
        when (function.identifier) {
            "<program>" -> "main"
            else ->
                when (function) {
                    is Definition.ForeignFunctionDeclaration -> function.identifier
                    is Definition.FunctionDefinition ->
                        "${function.identifier}_${function.arguments.size}_${function.hashCode().absoluteValue}"
                }
        },
    )
