package cacophony.semantic

import cacophony.grammar.syntaxtree.Expression
import cacophony.utils.Diagnostics

fun analyzeFunctions(
    ast: Expression.Subsequent,
    diagnostics: Diagnostics,
    resolvedVariables: ResolvedVariables,
    types: TypeCheckingResult,
    callGraph: CallGraph,
): FunctionAnalysisResult {
    TODO()
}

typealias FunctionAnalysisResult = Map<Expression.Definition.FunctionDeclaration, AnalyzedFunction>

class AnalyzedFunction(
    val staticDepth: Int,
    val variables: Set<AnalyzedVariable>,
)

class AnalyzedVariable(
    val declaration: Expression.Definition.VariableDeclaration,
    val definedIn: Expression.Definition.FunctionDeclaration,
    val readOnly: Boolean,
)
