package cacophony.controlflow.functions

import cacophony.controlflow.Variable
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
    escapedVariables: Set<Variable>,
): Map<LambdaExpression, StaticFunctionHandler> {
    val handlers = mutableMapOf<LambdaExpression, StaticFunctionHandler>()
    val order = analyzedFunctions.filter { staticLinkCallables.contains(it.key) }.entries.sortedBy { it.value.staticDepth }

    val ancestorHandlers = mutableMapOf<LambdaExpression, List<StaticFunctionHandler>>()

    for ((function, analyzedFunction) in order) {
        if (analyzedFunction.parentLink == null) {
            handlers[function] =
                StaticFunctionHandlerImpl(
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
                StaticFunctionHandlerImpl(
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

private fun generateLambdaHandlers(
    analyzedFunctions: FunctionAnalysisResult,
    closureCallables: Set<LambdaExpression>,
    callConvention: CallConvention,
    variablesMap: VariablesMap,
    escapeAnalysisResult: EscapeAnalysisResult,
): Map<LambdaExpression, ClosureHandler> {
    val handlers = mutableMapOf<LambdaExpression, ClosureHandler>()
    for (lambda in closureCallables) {
        handlers[lambda] =
            ClosureHandlerImpl(
                callConvention,
                lambda,
                analyzedFunctions[lambda]!!,
                variablesMap,
                escapeAnalysisResult.escapedVariables,
            )
    }
    return handlers
}
