package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.functions.CallGenerator
import cacophony.controlflow.functions.FunctionHandler
import cacophony.semantic.analysis.UseTypeAnalysisResult
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.Definition

typealias ProgramCFG = Map<Definition.FunctionDefinition, CFGFragment>

fun generateCFG(
    resolvedVariables: ResolvedVariables,
    analyzedUseTypes: UseTypeAnalysisResult,
    functionHandlers: Map<Definition.FunctionDefinition, FunctionHandler>,
    callGenerator: CallGenerator,
): ProgramCFG {
    val result =
        functionHandlers.mapValues { (function, _) ->
            generateFunctionCFG(function, functionHandlers, resolvedVariables, analyzedUseTypes, callGenerator)
        }
    return result
}

internal fun generateFunctionCFG(
    function: Definition.FunctionDefinition,
    functionHandlers: Map<Definition.FunctionDefinition, FunctionHandler>,
    resolvedVariables: ResolvedVariables,
    analyzedUseTypes: UseTypeAnalysisResult,
    callGenerator: CallGenerator,
): CFGFragment {
    val generator = CFGGenerator(resolvedVariables, analyzedUseTypes, function, functionHandlers, callGenerator)
    return generator.generateFunctionCFG()
}
