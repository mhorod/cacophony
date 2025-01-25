package cacophony.controlflow.functions

import cacophony.controlflow.Variable
import cacophony.semantic.analysis.ClosureAnalysisResult
import cacophony.semantic.analysis.EscapeAnalysisResult
import cacophony.semantic.analysis.FunctionAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.utils.CompileException

fun generateFunctionHandlers(
    analyzedFunctions: FunctionAnalysisResult,
    staticLinkCallables: Set<LambdaExpression>,
    callConvention: CallConvention,
    variablesMap: VariablesMap,
    escapedVariables: Set<Variable>,
): Map<LambdaExpression, FunctionHandler> {
    val handlers = mutableMapOf<LambdaExpression, FunctionHandler>()
    val order = analyzedFunctions.filter { staticLinkCallables.contains(it.key) }.entries.sortedBy { it.value.staticDepth }

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
                    escapedVariables,
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
                    escapedVariables,
                )
            ancestorHandlers[function] = functionAncestorHandlers
        }
    }

    return handlers
}

fun generateLambdaHandlers(
    analyzedFunctions: FunctionAnalysisResult,
    closureCallables: Set<LambdaExpression>,
    callConvention: CallConvention,
    variablesMap: VariablesMap,
    escapeAnalysisResult: EscapeAnalysisResult,
    closureAnalysisResult: ClosureAnalysisResult,
): Map<LambdaExpression, LambdaHandler> {
    val handlers = mutableMapOf<LambdaExpression, LambdaHandler>()
    for (lambda in closureCallables) {
        handlers[lambda] =
            LambdaHandlerImpl(
                callConvention,
                lambda,
                analyzedFunctions[lambda]!!,
                variablesMap,
                escapeAnalysisResult.escapedVariables,
                closureAnalysisResult,
            )
    }
    return handlers
}
