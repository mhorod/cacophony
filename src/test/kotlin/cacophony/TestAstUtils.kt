package cacophony

import cacophony.semantic.syntaxtree.*
import cacophony.semantic.types.StructType
import cacophony.semantic.types.TypeExpr
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

fun unitType() = BaseType.Basic(mockRange(), "Unit")

fun intType() = BaseType.Basic(mockRange(), "Int")

fun boolType() = BaseType.Basic(mockRange(), "Bool")

fun empty() = Empty(mockRange())

/**
 * Declares a function of type [*args] -> returnType
 */
fun functionDefinition(identifier: String, args: List<Definition.FunctionArgument>, body: Expression, returnType: Type) =
    Definition.FunctionDefinition(
        mockRange(),
        identifier,
        BaseType.Functional(mockk(), args.map { it.type }, returnType),
        args,
        returnType,
        body,
    )

fun unitFunctionDefinition(identifier: String, body: Expression) = functionDefinition(identifier, emptyList(), body, unitType())

fun unitFunctionDefinition(identifier: String, arguments: List<Definition.FunctionArgument>, body: Expression) =
    functionDefinition(identifier, arguments, body, unitType())

fun intFunctionDefinition(identifier: String, body: Expression) = functionDefinition(identifier, emptyList(), body, intType())

fun intFunctionDefinition(identifier: String, args: List<Definition.FunctionArgument>, body: Expression) =
    functionDefinition(identifier, args, body, intType())

fun boolFunctionDefinition(identifier: String, body: Expression) = functionDefinition(identifier, emptyList(), body, boolType())

fun typedArg(identifier: String, type: Type) = Definition.FunctionArgument(mockRange(), identifier, type)

fun arg(identifier: String) = typedArg(identifier, unitType())

fun intArg(identifier: String) = typedArg(identifier, intType())

fun typedFunctionDefinition(
    identifier: String,
    argsType: BaseType.Functional?,
    arguments: List<Definition.FunctionArgument>,
    outType: Type,
    body: Expression,
) = Definition.FunctionDefinition(
    mockRange(),
    identifier,
    argsType,
    arguments,
    outType,
    body,
)

fun functionDefinition(identifier: String, arguments: List<Definition.FunctionArgument>, body: Expression) =
    typedFunctionDefinition(
        identifier,
        null,
        arguments,
        unitType(),
        body,
    )

fun foreignFunctionDeclaration(identifier: String, argumentsType: List<Type>, returnType: Type) =
    Definition.ForeignFunctionDeclaration(
        mockRange(),
        identifier,
        BaseType.Functional(mockRange(), argumentsType, returnType),
        returnType,
    )

fun typedVariableDeclaration(identifier: String, type: BaseType.Basic?, value: Expression) =
    Definition.VariableDeclaration(
        mockRange(),
        identifier,
        type,
        value,
    )

fun struct(vararg fields: Pair<String, Expression>) = Struct(mockRange(), fields.associate { structField(it.first) to it.second })

fun typedStructField(name: String, type: Type) = StructField(mockRange(), name, type)

fun structField(name: String) = StructField(mockRange(), name, null)

fun structDeclaration(vararg fields: Pair<StructField, Expression>) = Struct(mockRange(), fields.toMap())

fun lvalueFieldRef(lhs: Assignable, field: String) = FieldRef.LValue(mockRange(), lhs, field)

fun rvalueFieldRef(lhs: Expression, field: String) = FieldRef.RValue(mockRange(), lhs, field)

fun typedVariableDeclaration(identifier: String, type: NonFunctionalType?, value: Expression) =
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

fun call(function: Expression, vararg arguments: Expression) = FunctionCall(mockRange(), function, arguments.toList())

fun call(identifier: String, vararg arguments: Expression) = call(variableUse(identifier), *arguments)

fun astOf(vararg expressions: Expression) =
    Block(
        mockRange(),
        listOf(
            unitFunctionDefinition(
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

infix fun Assignable.assign(rhs: Expression) = OperatorBinary.Assignment(mockRange(), this, rhs)

infix fun Assignable.addeq(rhs: Expression) = OperatorBinary.AdditionAssignment(mockRange(), this, rhs)

infix fun Assignable.subeq(rhs: Expression) = OperatorBinary.SubtractionAssignment(mockRange(), this, rhs)

infix fun Assignable.muleq(rhs: Expression) = OperatorBinary.MultiplicationAssignment(mockRange(), this, rhs)

infix fun Assignable.diveq(rhs: Expression) = OperatorBinary.DivisionAssignment(mockRange(), this, rhs)

infix fun Assignable.modeq(rhs: Expression) = OperatorBinary.ModuloAssignment(mockRange(), this, rhs)

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

fun basicType(identifier: String) = BaseType.Basic(mockRange(), identifier)

fun functionalType(argTypes: List<Type>, resType: Type) = BaseType.Functional(mockRange(), argTypes.toList(), resType)

fun structType(vararg fields: Pair<String, Type>) = BaseType.Structural(mockRange(), fields.toMap())

fun structTypeExpr(vararg fields: Pair<String, TypeExpr>) = StructType(fields.toMap())
