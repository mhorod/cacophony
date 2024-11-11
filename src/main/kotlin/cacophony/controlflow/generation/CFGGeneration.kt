package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.FunctionHandler
import cacophony.semantic.FunctionAnalysisResult
import cacophony.semantic.ResolvedVariables
import cacophony.semantic.UseTypeAnalysisResult
import cacophony.semantic.syntaxtree.Definition

fun generateCFG(
    resolvedVariables: ResolvedVariables,
    analyzedFunctions: FunctionAnalysisResult,
    analyzedUseTypes: UseTypeAnalysisResult,
    functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>,
): Map<Definition.FunctionDeclaration, CFGFragment> =
    functionHandlers.mapValues { (function, _) ->
        generateFunctionCFG(function, functionHandlers, resolvedVariables, analyzedFunctions, analyzedUseTypes)
    }

private fun generateFunctionCFG(
    function: Definition.FunctionDeclaration,
    functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>,
    resolvedVariables: ResolvedVariables,
    analyzedFunctions: FunctionAnalysisResult,
    analyzedUseTypes: UseTypeAnalysisResult,
): CFGFragment {
    val generator = CFGGenerator(resolvedVariables, analyzedFunctions, analyzedUseTypes, function, functionHandlers)
    return generator.generateFunctionCFG()
}
