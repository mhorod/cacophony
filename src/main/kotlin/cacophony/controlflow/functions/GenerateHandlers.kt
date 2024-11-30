package cacophony.controlflow.functions

import cacophony.semantic.analysis.FunctionAnalysisResult
import cacophony.semantic.syntaxtree.Definition
import cacophony.utils.CompileException

fun generateFunctionHandlers(analyzedFunctions: FunctionAnalysisResult): Map<Definition.FunctionDeclaration, FunctionHandler> {
    val handlers = mutableMapOf<Definition.FunctionDeclaration, FunctionHandler>()
    val order = analyzedFunctions.entries.sortedBy { it.value.staticDepth }

    val ancestorHandlers = mutableMapOf<Definition.FunctionDeclaration, List<FunctionHandler>>()

    for ((function, analyzedFunction) in order) {
        if (analyzedFunction.parentLink == null) {
            handlers[function] = FunctionHandlerImpl(function, analyzedFunction, emptyList())
        } else {
            val parentHandler =
                handlers[analyzedFunction.parentLink.parent]
                    ?: throw CompileException("Parent function handler not found")
            val functionAncestorHandlers =
                listOf(parentHandler) + (ancestorHandlers[analyzedFunction.parentLink.parent] ?: emptyList())
            handlers[function] = FunctionHandlerImpl(function, analyzedFunction, functionAncestorHandlers)
            ancestorHandlers[function] = functionAncestorHandlers
        }
    }

    return handlers
}
