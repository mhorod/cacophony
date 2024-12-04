package cacophony.controlflow.functions

import cacophony.semantic.analysis.FunctionAnalysisResult
import cacophony.semantic.syntaxtree.Definition
import cacophony.utils.CompileException

fun generateFunctionHandlers(
    analyzedFunctions: FunctionAnalysisResult,
    callConvention: CallConvention,
): Map<Definition.FunctionDefinition, FunctionHandler> {
    val handlers = mutableMapOf<Definition.FunctionDefinition, FunctionHandler>()
    val order = analyzedFunctions.entries.sortedBy { it.value.staticDepth }

    val ancestorHandlers = mutableMapOf<Definition.FunctionDefinition, List<FunctionHandler>>()

    for ((function, analyzedFunction) in order) {
        if (analyzedFunction.parentLink == null) {
            handlers[function] = FunctionHandlerImpl(function, analyzedFunction, emptyList(), callConvention)
        } else {
            val parentHandler =
                handlers[analyzedFunction.parentLink.parent]
                    ?: throw CompileException("Parent function handler not found")
            val functionAncestorHandlers =
                listOf(parentHandler) + (ancestorHandlers[analyzedFunction.parentLink.parent] ?: emptyList())
            handlers[function] = FunctionHandlerImpl(function, analyzedFunction, functionAncestorHandlers, callConvention)
            ancestorHandlers[function] = functionAncestorHandlers
        }
    }

    return handlers
}
