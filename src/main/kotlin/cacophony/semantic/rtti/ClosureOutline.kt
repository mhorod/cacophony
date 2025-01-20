package cacophony.semantic.rtti

import cacophony.controlflow.functions.LambdaHandler
import cacophony.semantic.syntaxtree.LambdaExpression

typealias LambdaOutlineLocation = Map<LambdaExpression, String>

fun generateClosureOutlines(lambdaHandlers: Map<LambdaExpression, LambdaHandler>): LambdaOutlineLocation =
    lambdaHandlers.mapValues { generateClosureOutline(it.value) }

fun generateClosureOutline(handler: LambdaHandler): String {
    val closureVariables = handler.getCapturedVariableOffsets()
    val outlineSize = closureVariables.maxOfOrNull { (variable, offset) -> offset + variable.size() } ?: 0
    val label = getLambdaClosureLabel(handler.getBodyReference())

    return Array(outlineSize) { false }.let { closureOutline ->
        closureVariables.forEach { (variable, offset) ->
            val primitives = variable.getPrimitives()
            primitives.forEachIndexed { index, primitiveVar -> closureOutline[offset + index] = primitiveVar.holdsReference }
        }
        toAsm(label, closureOutline.toList())
    }
}

fun getLambdaClosureLabel(fn: LambdaExpression) = "closure_${fn.getLabel()}"
