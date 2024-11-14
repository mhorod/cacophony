package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.FunctionHandler
import cacophony.semantic.ResolvedVariables
import cacophony.semantic.UseTypeAnalysisResult
import cacophony.semantic.syntaxtree.Definition

typealias ProgramCFG = Map<Definition.FunctionDeclaration, CFGFragment>

fun generateCFG(
    resolvedVariables: ResolvedVariables,
    analyzedUseTypes: UseTypeAnalysisResult,
    functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>,
): ProgramCFG {
    val result =
        functionHandlers.mapValues { (function, _) ->
            generateFunctionCFG(function, functionHandlers, resolvedVariables, analyzedUseTypes)
        }
    return result
}

internal fun generateFunctionCFG(
    function: Definition.FunctionDeclaration,
    functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>,
    resolvedVariables: ResolvedVariables,
    analyzedUseTypes: UseTypeAnalysisResult,
): CFGFragment {
    val generator = CFGGenerator(resolvedVariables, analyzedUseTypes, function, functionHandlers)
    return generator.generateFunctionCFG()
}
