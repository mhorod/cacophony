package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.functions.CallGenerator
import cacophony.controlflow.functions.CallableHandlers
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.rtti.LambdaOutlineLocation
import cacophony.semantic.rtti.ObjectOutlineLocation
import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.semantic.types.ResolvedVariables
import cacophony.semantic.types.TypeCheckingResult

typealias ProgramCFG = Map<LambdaExpression, CFGFragment>

fun generateCFG(
    resolvedVariables: ResolvedVariables,
    callableHandlers: CallableHandlers,
    variablesMap: VariablesMap,
    typeCheckingResult: TypeCheckingResult,
    callGenerator: CallGenerator,
    objectOutlineLocation: ObjectOutlineLocation,
    lambdaOutlineLocation: LambdaOutlineLocation,
): ProgramCFG {
    val result =
        callableHandlers
            .getAllCallables()
            .map { function ->
                function to
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
            }.toMap()
    return result
}

internal fun generateFunctionCFG(
    function: LambdaExpression,
    callableHandlers: CallableHandlers,
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
            variablesMap,
            typeCheckingResult,
            callGenerator,
            objectOutlineLocation,
            lambdaOutlineLocation,
        )
    return generator.generateFunctionCFG()
}
