package cacophony.controlflow.functions

import cacophony.semantic.analysis.ClosureAnalysisResult
import cacophony.semantic.analysis.EscapeAnalysisResult
import cacophony.semantic.analysis.FunctionAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.utils.CompileException

fun generateCallableHandlers(
    analyzedFunctions: FunctionAnalysisResult,
    escapeAnalysis: EscapeAnalysisResult,
    callConvention: CallConvention,
    variablesMap: VariablesMap,
    closureAnalysis: ClosureAnalysisResult,
): CallableHandlers {
    // TODO: use closure analysis to generate lambda and function handlers separately
    TODO()
}

private fun generateFunctionHandlers(
    analyzedFunctions: FunctionAnalysisResult,
    staticLinkCallables: Set<LambdaExpression>,
    callConvention: CallConvention,
    variablesMap: VariablesMap,
    escapeAnalysis: EscapeAnalysisResult,
): Map<LambdaExpression, StaticFunctionHandler> {
    val handlers = mutableMapOf<LambdaExpression, StaticFunctionHandler>()
    val order = analyzedFunctions.filter { staticLinkCallables.contains(it.key) }.entries.sortedBy { it.value.staticDepth }

    val ancestorHandlers = mutableMapOf<LambdaExpression, List<CallableHandler>>()

    // TODO: Lambda can also be a parent, take that into consideration here
    for ((function, analyzedFunction) in order) {
        if (analyzedFunction.parentLink == null) {
            handlers[function] =
                StaticFunctionHandlerImpl(
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
                StaticFunctionHandlerImpl(
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

private fun generateLambdaHandlers(
    analyzedFunctions: FunctionAnalysisResult,
    closureCallables: Set<LambdaExpression>,
    callConvention: CallConvention,
    variablesMap: VariablesMap,
    escapeAnalysisResult: EscapeAnalysisResult,
): Map<LambdaExpression, ClosureHandler> =
    closureCallables.associateWith { lambda ->
        ClosureHandlerImpl(
            callConvention,
            lambda,
            analyzedFunctions[lambda]!!,
            variablesMap,
            escapeAnalysisResult,
        )
    }
