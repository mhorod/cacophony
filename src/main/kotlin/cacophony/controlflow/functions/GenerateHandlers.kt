package cacophony.controlflow.functions

import cacophony.semantic.analysis.EscapeAnalysisResult
import cacophony.semantic.analysis.FunctionAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.utils.CompileException

fun generateFunctionHandlers(
    analyzedFunctions: FunctionAnalysisResult,
    callConvention: CallConvention,
    variablesMap: VariablesMap,
    escapeAnalysis: EscapeAnalysisResult,
): Map<LambdaExpression, FunctionHandler> {
    val handlers = mutableMapOf<LambdaExpression, FunctionHandler>()
    val order = analyzedFunctions.entries.sortedBy { it.value.staticDepth }

    val ancestorHandlers = mutableMapOf<LambdaExpression, List<FunctionHandler>>()

    for ((function, analyzedFunction) in order) {
        if (analyzedFunction.parentLink == null) {
            handlers[function] =
                FunctionHandlerImpl(
                    function,
                    analyzedFunction,
                    emptyList(),
                    callConvention,
                    variablesMap,
                    escapeAnalysis,
                )
        } else {
            val parentHandler =
                handlers[analyzedFunction.parentLink.parent]
                    ?: throw CompileException("Parent function handler not found")
            val functionAncestorHandlers =
                listOf(parentHandler) + (ancestorHandlers[analyzedFunction.parentLink.parent] ?: emptyList())
            handlers[function] =
                FunctionHandlerImpl(
                    function,
                    analyzedFunction,
                    functionAncestorHandlers,
                    callConvention,
                    variablesMap,
                    escapeAnalysis,
                )
            ancestorHandlers[function] = functionAncestorHandlers
        }
    }

    return handlers
}

fun generateLambdaHandlers(): Map<LambdaExpression, LambdaHandler> {
    // TODO
    return emptyMap()
}
