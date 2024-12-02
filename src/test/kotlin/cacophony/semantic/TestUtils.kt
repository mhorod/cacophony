package cacophony.semantic

import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.analysis.AnalyzedVariable
import cacophony.semantic.analysis.CallGraph
import cacophony.semantic.analysis.StaticFunctionRelations
import cacophony.semantic.syntaxtree.*

fun program(ast: Block) = ast.expressions[0] as Definition.FunctionDeclaration

fun programCall(ast: Block) = (ast.expressions[1] as FunctionCall).function as VariableUse

fun programResolvedName(ast: Block) =
    (
        programCall(ast)
            to program(ast)
    )

fun programStaticRelation() = StaticFunctionRelations(null, 0, emptySet(), emptySet())

fun programFunctionAnalysis(ast: Block) =
    (
        program(ast)
            to analyzedFunction(program(ast), 0, emptySet())
    )

fun callGraph(vararg calls: Pair<Definition.FunctionDeclaration, Definition.FunctionDeclaration>): CallGraph =
    calls.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }

fun analyzedFunction(function: Definition.FunctionDeclaration, staticDepth: Int, variables: Set<AnalyzedVariable>) =
    AnalyzedFunction(
        function,
        null,
        variables,
        mutableSetOf(),
        staticDepth,
        emptySet(),
    )
