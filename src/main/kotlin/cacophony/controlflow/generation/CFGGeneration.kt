package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.functions.CallGenerator
import cacophony.controlflow.functions.CallableHandler
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.rtti.LambdaOutlineLocation
import cacophony.semantic.rtti.ObjectOutlineLocation
import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.semantic.types.TypeCheckingResult

typealias ProgramCFG = Map<LambdaExpression, CFGFragment>

fun generateCFG(
    resolvedVariables: ResolvedVariables,
    callableHandlers: Map<LambdaExpression, CallableHandler>,
    variablesMap: VariablesMap,
    typeCheckingResult: TypeCheckingResult,
    callGenerator: CallGenerator,
    objectOutlineLocation: ObjectOutlineLocation,
    lambdaOutlineLocation: LambdaOutlineLocation,
): ProgramCFG {
    val result =
        callableHandlers.mapValues { (function, _) ->
            generateFunctionCFG(
                function,
                callableHandlers,
                resolvedVariables,
                variablesMap,
                typeCheckingResult,
                callGenerator,
                objectOutlineLocation,
                lambdaOutlineLocation,
            )
        }
    return result
}

internal fun generateFunctionCFG(
    function: LambdaExpression,
    callableHandlers: Map<LambdaExpression, CallableHandler>,
    resolvedVariables: ResolvedVariables,
    variablesMap: VariablesMap,
    typeCheckingResult: TypeCheckingResult,
    callGenerator: CallGenerator,
    objectOutlineLocation: ObjectOutlineLocation,
    lambdaOutlineLocation: LambdaOutlineLocation,
): CFGFragment {
    val generator =
        CFGGenerator(
            resolvedVariables,
            function,
            callableHandlers,
            emptyMap(), // TODO: fill LambdaHandlers in
            variablesMap,
            typeCheckingResult,
            callGenerator,
            objectOutlineLocation,
            lambdaOutlineLocation,
        )
    return generator.generateFunctionCFG()
}
