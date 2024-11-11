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
): CFGFragment {
    val fragments =
        functionHandlers.map { (function, _) ->
            generateFunctionCFG(function, functionHandlers, resolvedVariables, analyzedFunctions, analyzedUseTypes)
        }

    return fragments.flatMap { it.entries }.associate { it.toPair() }
}

private fun generateFunctionCFG(
    function: Definition.FunctionDeclaration,
    functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>,
    resolvedVariables: ResolvedVariables,
    analyzedFunctions: FunctionAnalysisResult,
    analyzedUseTypes: UseTypeAnalysisResult,
): CFGFragment {
    val generator = CFGGenerator(resolvedVariables, analyzedFunctions, analyzedUseTypes, function, functionHandlers)
    generator.run(function)
    return generator.getCFGFragment()
}
