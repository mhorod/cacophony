package cacophony.controlflow.functions

import cacophony.semantic.syntaxtree.LambdaExpression

class CallableHandlers(
    val lambdaHandlers: Map<LambdaExpression, LambdaHandler>,
    val functionHandlers: Map<LambdaExpression, FunctionHandler>,
) {
    fun getCallableHandler(callable: LambdaExpression): CallableHandler =
        if (lambdaHandlers.containsKey(callable)) {
            lambdaHandlers[callable] ?: error("value was just in map")
        } else if (functionHandlers.containsKey(callable)) {
            functionHandlers[callable] ?: error("value was just in map")
        } else {
            error("No handler found for callable $callable")
        }

    fun getLambdaHandler(callable: LambdaExpression): LambdaHandler =
        lambdaHandlers[callable] ?: error("No lambda handler found for callable $callable")

    fun getFunctionHandler(callable: LambdaExpression): FunctionHandler =
        functionHandlers[callable] ?: error("No function handler found for callable $callable")

    fun getAllCallables() = lambdaHandlers.keys + functionHandlers.keys
}
