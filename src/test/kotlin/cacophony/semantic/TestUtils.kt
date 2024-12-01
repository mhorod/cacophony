package cacophony.semantic

import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.analysis.AnalyzedVariable
import cacophony.semantic.analysis.CallGraph
import cacophony.semantic.analysis.StaticFunctionRelations
import cacophony.semantic.syntaxtree.*
import cacophony.utils.Location

fun mockRange() = Pair(Location(0), Location(0))

fun unitType() = Type.Basic(mockRange(), "Unit")

fun functionDeclaration(identifier: String, body: Expression) =
    Definition.FunctionDeclaration(
        mockRange(),
        identifier,
        null,
        emptyList(),
        unitType(),
        body,
    )

fun arg(identifier: String) = Definition.FunctionArgument(mockRange(), identifier, unitType())

fun functionDeclaration(identifier: String, arguments: List<Definition.FunctionArgument>, body: Expression) =
    Definition.FunctionDeclaration(
        mockRange(),
        identifier,
        null,
        arguments,
        unitType(),
        body,
    )

fun variableDeclaration(identifier: String, value: Expression) =
    Definition.VariableDeclaration(
        mockRange(),
        identifier,
        null,
        value,
    )

fun variableUse(identifier: String) = VariableUse(mockRange(), identifier)

fun variableWrite(variableUse: VariableUse) =
    OperatorBinary.Assignment(
        mockRange(),
        variableUse,
        Empty(mockRange()),
    )

fun block(vararg expressions: Expression) = Block(mockRange(), expressions.toList())

fun call(variableUse: VariableUse, vararg arguments: Expression) = FunctionCall(mockRange(), variableUse, arguments.toList())

fun astOf(vararg expressions: Expression) =
    Block(
        mockRange(),
        listOf(
            functionDeclaration(
                MAIN_FUNCTION_IDENTIFIER,
                Block(mockRange(), expressions.toList()),
            ),
            call(variableUse(MAIN_FUNCTION_IDENTIFIER)),
        ),
    )

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

fun programFunctionAnalysis(ast: Block, variablesUsedInNestedFunctions: Set<Definition>) =
    (
        program(ast)
            to
            AnalyzedFunction(
                program(ast),
                null,
                emptySet(),
                mutableSetOf(),
                0,
                variablesUsedInNestedFunctions,
            )
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
