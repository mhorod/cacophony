package cacophony.semantic

import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Empty
import cacophony.semantic.syntaxtree.Expression
import cacophony.semantic.syntaxtree.FunctionCall
import cacophony.semantic.syntaxtree.OperatorBinary
import cacophony.semantic.syntaxtree.Type
import cacophony.semantic.syntaxtree.VariableUse
import cacophony.utils.Location

fun mockRange() = Pair(Location(0), Location(0))

fun functionDeclaration(
    identifier: String,
    body: Expression,
) = Definition.FunctionDeclaration(
    mockRange(),
    identifier,
    null,
    emptyList(),
    Type.Basic(mockRange(), "Unit"),
    body,
)

fun variableDeclaration(
    identifier: String,
    value: Expression,
) = Definition.VariableDeclaration(
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

fun call(variableUse: VariableUse) = FunctionCall(mockRange(), variableUse, emptyList())

fun astOf(vararg expressions: Expression) = Block(mockRange(), expressions.toList())

fun callGraph(vararg calls: Pair<Definition.FunctionDeclaration, Definition.FunctionDeclaration>): CallGraph =
    calls.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
