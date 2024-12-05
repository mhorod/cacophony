package cacophony

import cacophony.semantic.syntaxtree.*
import cacophony.utils.Location
import io.mockk.every
import io.mockk.mockk

fun mockRange(): Pair<Location, Location> {
    val mock = mockk<Pair<Location, Location>>()
    every { mock.toString() } returns "(any)"
    every { mock == any() } returns true
    every { mock.first } returns Location(-1)
    every { mock.second } returns Location(-1)
    return mock
}

fun unitType() = Type.Basic(mockRange(), "Unit")

fun empty() = Empty(mockRange())

fun functionDeclaration(identifier: String, body: Expression) =
    Definition.FunctionDeclaration(
        mockRange(),
        identifier,
        null,
        emptyList(),
        unitType(),
        body,
    )

fun typedArg(identifier: String, type: Type) = Definition.FunctionArgument(mockRange(), identifier, type)

fun arg(identifier: String) = typedArg(identifier, unitType())

fun typedFunctionDeclaration(
    identifier: String,
    argsType: Type.Functional?,
    arguments: List<Definition.FunctionArgument>,
    outType: Type,
    body: Expression,
) = Definition.FunctionDeclaration(
    mockRange(),
    identifier,
    argsType,
    arguments,
    outType,
    body,
)

fun functionDeclaration(identifier: String, arguments: List<Definition.FunctionArgument>, body: Expression) =
    typedFunctionDeclaration(
        identifier,
        null,
        arguments,
        unitType(),
        body,
    )

fun typedStructField(name: String, type: Type) = StructField(mockRange(), name, type)

fun structField(name: String) = StructField(mockRange(), name, null)

fun structDeclaration(vararg fields: Pair<StructField, Expression>) = Struct(mockRange(), fields.toMap())

fun typedVariableDeclaration(identifier: String, type: Type.Basic?, value: Expression) =
    Definition.VariableDeclaration(
        mockRange(),
        identifier,
        type,
        value,
    )

fun variableDeclaration(identifier: String, value: Expression) = typedVariableDeclaration(identifier, null, value)

fun variableUse(identifier: String) = VariableUse(mockRange(), identifier)

fun variableWrite(variableUse: VariableUse) =
    OperatorBinary.Assignment(
        mockRange(),
        variableUse,
        Empty(mockRange()),
    )

fun variableWrite(variableUse: VariableUse, value: Expression) =
    OperatorBinary.Assignment(
        mockRange(),
        variableUse,
        value,
    )

fun block(vararg expressions: Expression) = Block(mockRange(), expressions.toList())

fun call(variableUse: VariableUse, vararg arguments: Expression) = FunctionCall(mockRange(), variableUse, arguments.toList())

fun call(identifier: String, vararg arguments: Expression) = call(variableUse(identifier), *arguments)

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

infix fun Expression.add(rhs: Expression) = OperatorBinary.Addition(mockRange(), this, rhs)

infix fun Expression.sub(rhs: Expression) = OperatorBinary.Subtraction(mockRange(), this, rhs)

infix fun Expression.mul(rhs: Expression) = OperatorBinary.Multiplication(mockRange(), this, rhs)

infix fun Expression.div(rhs: Expression) = OperatorBinary.Division(mockRange(), this, rhs)

infix fun Expression.mod(rhs: Expression) = OperatorBinary.Modulo(mockRange(), this, rhs)

infix fun Expression.land(rhs: Expression) = OperatorBinary.LogicalAnd(mockRange(), this, rhs)

infix fun Expression.lor(rhs: Expression) = OperatorBinary.LogicalOr(mockRange(), this, rhs)

infix fun Expression.eq(rhs: Expression) = OperatorBinary.Equals(mockRange(), this, rhs)

infix fun Expression.neq(rhs: Expression) = OperatorBinary.NotEquals(mockRange(), this, rhs)

infix fun Expression.leq(rhs: Expression) = OperatorBinary.LessEqual(mockRange(), this, rhs)

infix fun Expression.lt(rhs: Expression) = OperatorBinary.Less(mockRange(), this, rhs)

infix fun Expression.geq(rhs: Expression) = OperatorBinary.GreaterEqual(mockRange(), this, rhs)

infix fun Expression.gt(rhs: Expression) = OperatorBinary.Greater(mockRange(), this, rhs)

infix fun Expression.addeq(rhs: Expression) = OperatorBinary.AdditionAssignment(mockRange(), this, rhs)

infix fun Expression.subeq(rhs: Expression) = OperatorBinary.SubtractionAssignment(mockRange(), this, rhs)

infix fun Expression.muleq(rhs: Expression) = OperatorBinary.MultiplicationAssignment(mockRange(), this, rhs)

infix fun Expression.diveq(rhs: Expression) = OperatorBinary.DivisionAssignment(mockRange(), this, rhs)

infix fun Expression.modeq(rhs: Expression) = OperatorBinary.ModuloAssignment(mockRange(), this, rhs)

fun minus(expression: Expression) = OperatorUnary.Minus(mockRange(), expression)

fun lnot(expr: Expression) = OperatorUnary.Negation(mockRange(), expr)

fun lit(int: Int) = Literal.IntLiteral(mockRange(), int)

fun lit(bool: Boolean) = Literal.BoolLiteral(mockRange(), bool)

fun ifThenElse(condition: Expression, trueExpr: Expression, falseExpr: Expression) =
    Statement.IfElseStatement(mockRange(), condition, trueExpr, falseExpr)

fun ifThen(condition: Expression, trueExpr: Expression) = Statement.IfElseStatement(mockRange(), condition, trueExpr, null)

fun whileLoop(testExpression: Expression, doExpression: Expression) = Statement.WhileStatement(mockRange(), testExpression, doExpression)

fun breakStatement() = Statement.BreakStatement(mockRange())

fun returnStatement(value: Expression) = Statement.ReturnStatement(mockRange(), value)

fun basicType(identifier: String) = Type.Basic(mockRange(), identifier)

fun functionalType(argTypes: List<Type>, resType: Type) = Type.Functional(mockRange(), argTypes.toList(), resType)

fun structType(vararg fields: Pair<String, Type>) = Type.Structural(mockRange(), fields.toMap())
