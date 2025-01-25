package cacophony.semantic

import cacophony.controlflow.Variable
import cacophony.semantic.analysis.*
import cacophony.semantic.syntaxtree.*

fun program(ast: Block) = ast.expressions[0] as Definition.FunctionDefinition

fun programCall(ast: Block) = (ast.expressions[1] as FunctionCall).function as VariableUse

fun programResolvedName(ast: Block) =
    (
        programCall(ast)
            to program(ast)
    )

fun programStaticRelation() = StaticFunctionRelations(null, 0, emptySet(), emptySet())

fun programFunctionAnalysis(ast: Block) =
    (
        program(ast).value
            to analyzedFunction(program(ast), 0, emptySet())
    )


fun analyzedFunction(function: Definition.FunctionDefinition, staticDepth: Int, variables: Set<AnalyzedVariable>) =
    AnalyzedFunction(
        function.value,
        null,
        variables,
        function.value.arguments,
        mutableSetOf(),
        staticDepth,
        emptySet(),
    )

fun createVariablesMap(definitions: Map<Definition, Variable> = emptyMap(), lvalues: Map<Assignable, Variable> = emptyMap()): VariablesMap =
    VariablesMap(lvalues, definitions)
