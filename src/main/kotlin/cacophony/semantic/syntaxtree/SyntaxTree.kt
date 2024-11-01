package cacophony.semantic.syntaxtree

import cacophony.utils.Location

typealias AST = Block

sealed class Type(
    val range: Pair<Location, Location>,
) {
    class Basic(
        range: Pair<Location, Location>,
        val identifier: String,
    ) : Type(range)

    class Functional(
        range: Pair<Location, Location>,
        val argumentsType: List<Type>,
        val returnType: Type,
    ) : Type(range)
}

// everything in cacophony is an expression
sealed class Expression(
    val range: Pair<Location, Location>,
)

// artificial instance, can be useful when calculating values of nested expressions
class Empty(
    range: Pair<Location, Location>,
) : Expression(range)

class VariableUse(
    range: Pair<Location, Location>,
    val identifier: String,
) : Expression(range)

sealed class Definition(
    range: Pair<Location, Location>,
    val identifier: String,
) : Expression(range) {
    class VariableDeclaration(
        range: Pair<Location, Location>,
        identifier: String,
        val type: Type?,
        val expression: Expression,
    ) : Definition(range, identifier)

    class FunctionDeclaration(
        range: Pair<Location, Location>,
        identifier: String,
        val type: Type?,
        val arguments: List<FunctionArgument>,
        val returnType: Type,
        val body: Expression,
    ) : Definition(range, identifier)

    class FunctionArgument(
        range: Pair<Location, Location>,
        identifier: String,
        val type: Type,
    ) : Definition(range, identifier)
}

class FunctionCall(
    range: Pair<Location, Location>,
    val function: Expression,
    val arguments: List<Expression>,
) : Expression(range)

sealed class Literal(
    range: Pair<Location, Location>,
) : Expression(range) {
    class IntLiteral(
        range: Pair<Location, Location>,
        val value: Int,
    ) : Literal(range)

    class BoolLiteral(
        range: Pair<Location, Location>,
        val value: Boolean,
    ) : Literal(range)
}

// expression in parentheses and whole program
class Block(
    range: Pair<Location, Location>,
    val expressions: List<Expression>,
) : Expression(range)

sealed class Statement(
    range: Pair<Location, Location>,
) : Expression(range) {
    class IfElseStatement(
        range: Pair<Location, Location>,
        val testExpression: Expression,
        val doExpression: Expression,
        val elseExpression: Expression?,
    ) : Statement(range)

    class WhileStatement(
        range: Pair<Location, Location>,
        val testExpression: Expression,
        val doExpression: Expression,
    ) : Statement(range)

    class ReturnStatement(
        range: Pair<Location, Location>,
        val value: Expression,
    ) : Statement(range)

    class BreakStatement(
        range: Pair<Location, Location>,
    ) : Statement(range)
}

sealed class OperatorUnary(
    range: Pair<Location, Location>,
    val expression: Expression,
) : Expression(range) {
    class Negation(
        range: Pair<Location, Location>,
        expression: Expression,
    ) : OperatorUnary(range, expression)

    class Minus(
        range: Pair<Location, Location>,
        expression: Expression,
    ) : OperatorUnary(range, expression)
}

sealed class OperatorBinary(
    range: Pair<Location, Location>,
    val lhs: Expression,
    val rhs: Expression,
) : Expression(range) {
    class Multiplication(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class Division(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class Modulo(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class Addition(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class Subtraction(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class Less(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class Greater(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class LessEqual(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class GreaterEqual(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class Equals(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class NotEquals(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class LogicalAnd(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class LogicalOr(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class Assignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class AdditionAssignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class SubtractionAssignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class MultiplicationAssignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class DivisionAssignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)

    class ModuloAssignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs)
}
