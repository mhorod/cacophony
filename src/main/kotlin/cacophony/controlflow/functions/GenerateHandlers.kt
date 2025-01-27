package cacophony.controlflow.functions

import cacophony.semantic.analysis.ClosureAnalysisResult
import cacophony.semantic.analysis.EscapeAnalysisResult
import cacophony.semantic.analysis.FunctionAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.LambdaExpression

fun generateCallableHandlers(
    analyzedFunctions: FunctionAnalysisResult,
    escapeAnalysis: EscapeAnalysisResult,
    callConvention: CallConvention,
    variablesMap: VariablesMap,
    closureAnalysis: ClosureAnalysisResult,
): CallableHandlers {
    val closureHandlers = mutableMapOf<LambdaExpression, ClosureHandler>()
    val staticFunctionHandlers = mutableMapOf<LambdaExpression, StaticFunctionHandler>()

    fun getHandler(lambda: LambdaExpression): CallableHandler =
        closureHandlers[lambda] ?: staticFunctionHandlers[lambda] ?: error("Handler for $lambda not found")

    val ancestorHandlers = mutableMapOf<LambdaExpression, List<CallableHandler>>()

    analyzedFunctions.entries.sortedBy { it.value.staticDepth }.forEach { (lambda, analyzed) ->
        if (closureAnalysis.closures.contains(lambda)) {
            closureHandlers[lambda] =
                ClosureHandlerImpl(
                    callConvention,
                    lambda,
                    analyzed,
                    variablesMap,
                    escapeAnalysis,
                )
        } else if (closureAnalysis.staticFunctions.contains(lambda)) {
            val handlerChain =
                analyzed.parentLink?.let { link ->
                    val parent = link.parent
                    val parentHandler = getHandler(parent)
                    listOf(parentHandler) + (ancestorHandlers[parent] ?: emptyList())
                } ?: emptyList()
            staticFunctionHandlers[lambda] =
                StaticFunctionHandlerImpl(
                    lambda,
                    analyzed,
                    handlerChain,
                    callConvention,
                    variablesMap,
                    escapeAnalysis,
                )
            ancestorHandlers[lambda] = handlerChain
        } else {
            error("Lambda expression $lambda is neither a closure nor a static function")
        }
    }

    return CallableHandlers(closureHandlers, staticFunctionHandlers)
}
