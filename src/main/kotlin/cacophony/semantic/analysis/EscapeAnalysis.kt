package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.AST

typealias EscapeAnalysisResult = Set<Variable>

fun escapeAnalysis(
    ast: AST,
    resolvedVariables: ResolvedVariables,
    functionAnalysis: FunctionAnalysisResult,
    variablesMap: VariablesMap,
): EscapeAnalysisResult {
    TODO()
}
