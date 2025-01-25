package cacophony.controlflow.functions

import cacophony.semantic.syntaxtree.LambdaExpression

/**
 * Callable handlers split for closures and static functions
 */
class CallableHandlers(
    val closureHandlers: Map<LambdaExpression, ClosureHandler>,
    val staticFunctionHandlers: Map<LambdaExpression, StaticFunctionHandler>,
) {
    fun getCallableHandler(callable: LambdaExpression): CallableHandler =
        if (closureHandlers.containsKey(callable)) {
            closureHandlers[callable] ?: error("value was just in map")
        } else if (staticFunctionHandlers.containsKey(callable)) {
            staticFunctionHandlers[callable] ?: error("value was just in map")
        } else {
            error("No handler found for callable $callable")
        }

    fun getClosureHandler(callable: LambdaExpression): ClosureHandler =
        closureHandlers[callable] ?: error("No closure handler found for callable $callable")

    fun getStaticFunctionHandler(callable: LambdaExpression): StaticFunctionHandler =
        staticFunctionHandlers[callable] ?: error("No static function handler found for callable $callable")

    fun getAllCallables() = closureHandlers.keys + staticFunctionHandlers.keys

    fun getAll() = closureHandlers + staticFunctionHandlers
}
