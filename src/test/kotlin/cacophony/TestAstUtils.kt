package cacophony

import cacophony.semantic.CallGraph
import cacophony.semantic.syntaxtree.*
import cacophony.utils.Location

fun mockRange() = Pair(Location(0), Location(0))

fun unitType() = Type.Basic(mockRange(), "Unit")

fun functionDeclaration(
    identifier: String,
    body: Expression,
) = Definition.FunctionDeclaration(
    mockRange(),
    identifier,
    null,
    emptyList(),
    unitType(),
    body,
)

fun arg(identifier: String) = Definition.FunctionArgument(mockRange(), identifier, unitType())

fun functionDeclaration(
    identifier: String,
    arguments: List<Definition.FunctionArgument>,
    body: Expression,
) = Definition.FunctionDeclaration(
    mockRange(),
    identifier,
    null,
    arguments,
    unitType(),
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

fun variableWrite(
    variableUse: VariableUse,
    value: Expression,
) = OperatorBinary.Assignment(
    mockRange(),
    variableUse,
    value,
)

fun block(vararg expressions: Expression) = Block(mockRange(), expressions.toList())

fun call(variableUse: VariableUse) = FunctionCall(mockRange(), variableUse, emptyList())

fun astOf(vararg expressions: Expression) = Block(mockRange(), expressions.toList())

fun callGraph(vararg calls: Pair<Definition.FunctionDeclaration, Definition.FunctionDeclaration>): CallGraph =
    calls.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }

infix fun Expression.add(rhs: Expression) = OperatorBinary.Addition(mockRange(), this, rhs)

infix fun Expression.sub(rhs: Expression) = OperatorBinary.Subtraction(mockRange(), this, rhs)

infix fun Expression.mul(rhs: Expression) = OperatorBinary.Multiplication(mockRange(), this, rhs)

infix fun Expression.div(rhs: Expression) = OperatorBinary.Division(mockRange(), this, rhs)

infix fun Expression.land(rhs: Expression) = OperatorBinary.LogicalAnd(mockRange(), this, rhs)

infix fun Expression.lor(rhs: Expression) = OperatorBinary.LogicalOr(mockRange(), this, rhs)

infix fun Expression.eq(rhs: Expression) = OperatorBinary.Equals(mockRange(), this, rhs)

infix fun Expression.neq(rhs: Expression) = OperatorBinary.NotEquals(mockRange(), this, rhs)

infix fun Expression.leq(rhs: Expression) = OperatorBinary.LessEqual(mockRange(), this, rhs)

infix fun Expression.lt(rhs: Expression) = OperatorBinary.Less(mockRange(), this, rhs)

infix fun Expression.geq(rhs: Expression) = OperatorBinary.GreaterEqual(mockRange(), this, rhs)

infix fun Expression.gt(rhs: Expression) = OperatorBinary.Greater(mockRange(), this, rhs)

fun lnot(expr: Expression) = OperatorUnary.Negation(mockRange(), expr)

fun lit(int: Int) = Literal.IntLiteral(mockRange(), int)

fun lit(bool: Boolean) = Literal.BoolLiteral(mockRange(), bool)

fun ifThenElse(
    condition: Expression,
    trueExpr: Expression,
    falseExpr: Expression,
) = Statement.IfElseStatement(mockRange(), condition, trueExpr, falseExpr)
