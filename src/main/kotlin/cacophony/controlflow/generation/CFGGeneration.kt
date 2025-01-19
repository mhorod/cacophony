package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.functions.CallGenerator
import cacophony.controlflow.functions.FunctionHandler
import cacophony.semantic.analysis.UseTypeAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.rtti.LambdaOutlineLocation
import cacophony.semantic.rtti.ObjectOutlineLocation
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.types.TypeCheckingResult

typealias ProgramCFG = Map<Definition.FunctionDefinition, CFGFragment>

fun generateCFG(
    resolvedVariables: ResolvedVariables,
    analyzedUseTypes: UseTypeAnalysisResult,
    functionHandlers: Map<Definition.FunctionDefinition, FunctionHandler>,
    variablesMap: VariablesMap,
    typeCheckingResult: TypeCheckingResult,
    callGenerator: CallGenerator,
    objectOutlineLocation: ObjectOutlineLocation,
    lambdaOutlineLocation: LambdaOutlineLocation,
): ProgramCFG {
    val result =
        functionHandlers.mapValues { (function, _) ->
            generateFunctionCFG(
                function,
                functionHandlers,
                resolvedVariables,
                analyzedUseTypes,
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
    function: Definition.FunctionDefinition,
    functionHandlers: Map<Definition.FunctionDefinition, FunctionHandler>,
    resolvedVariables: ResolvedVariables,
    analyzedUseTypes: UseTypeAnalysisResult,
    variablesMap: VariablesMap,
    typeCheckingResult: TypeCheckingResult,
    callGenerator: CallGenerator,
    objectOutlineLocation: ObjectOutlineLocation,
    lambdaOutlineLocation: LambdaOutlineLocation,
): CFGFragment {
    val generator =
        CFGGenerator(
            resolvedVariables,
            analyzedUseTypes,
            function,
            functionHandlers,
            variablesMap,
            typeCheckingResult,
            callGenerator,
            objectOutlineLocation,
            lambdaOutlineLocation,
        )
    return generator.generateFunctionCFG()
}
