package cacophony.semantic

import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Definition
import cacophony.utils.Diagnostics

fun analyzeFunctions(
    ast: Block,
    diagnostics: Diagnostics,
    resolvedVariables: ResolvedVariables,
    types: TypeCheckingResult,
    callGraph: CallGraph,
): FunctionAnalysisResult {
    TODO()
}

typealias FunctionAnalysisResult = Map<Definition.FunctionDeclaration, AnalyzedFunction>

class AnalyzedFunction(
    val staticDepth: Int,
    val variables: Set<AnalyzedVariable>,
)

class AnalyzedVariable(
    val declaration: Definition.VariableDeclaration,
    val definedIn: Definition.FunctionDeclaration,
    val readOnly: Boolean,
)
