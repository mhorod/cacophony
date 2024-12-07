package cacophony.semantic.syntaxtree

import cacophony.utils.Location
import cacophony.utils.Tree
import cacophony.utils.TreeLeaf

sealed interface LeafExpression

typealias AST = Expression

fun areEquivalentTypes(lhs: Type?, rhs: Type?): Boolean = lhs?.isEquivalent(rhs) ?: (rhs == null)

fun areEquivalentTypes(lhs: List<Type?>, rhs: List<Type?>): Boolean =
    lhs.size == rhs.size && lhs.zip(rhs).all { areEquivalentTypes(it.first, it.second) }

fun areEquivalentExpressions(lhs: Expression?, rhs: Expression?): Boolean = lhs?.isEquivalent(rhs) ?: (rhs == null)

fun areEquivalentExpressions(lhs: List<Expression?>, rhs: List<Expression?>): Boolean =
    lhs.size == rhs.size && lhs.zip(rhs).all { areEquivalentExpressions(it.first, it.second) }

sealed class Type(
    val range: Pair<Location, Location>,
) {
    internal open fun isEquivalent(other: Type?): Boolean = other != null && range == other.range

    override fun toString(): String = "${this::class.simpleName}@${Integer.toHexString(hashCode())}"

    class Basic(
        range: Pair<Location, Location>,
        val identifier: String,
    ) : Type(range) {
        override fun toString() = identifier

        override fun isEquivalent(other: Type?): Boolean =
            super.isEquivalent(other) &&
                other is Basic &&
                identifier == other.identifier
    }

    class Functional(
        range: Pair<Location, Location>,
        val argumentsType: List<Type>,
        val returnType: Type,
    ) : Type(range) {
        override fun toString() = "[${argumentsType.joinToString(", ")}] => $returnType"

        override fun isEquivalent(other: Type?): Boolean =
            super.isEquivalent(other) &&
                other is Functional &&
                areEquivalentTypes(argumentsType, other.argumentsType) &&
                areEquivalentTypes(returnType, other.returnType)
    }
}

// everything in cacophony is an expression
sealed class Expression(
    val range: Pair<Location, Location>,
) : Tree {
    override fun toString(): String = "${this::class.simpleName}@${Integer.toHexString(hashCode())}"

    internal open fun isEquivalent(other: Expression?): Boolean = other != null && range == other.range
}

// artificial instance, can be useful when calculating values of nested expressions
class Empty(
    range: Pair<Location, Location>,
) : Expression(range),
    LeafExpression,
    TreeLeaf {
    override fun toString() = "empty"

    override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Empty
}

class VariableUse(
    range: Pair<Location, Location>,
    val identifier: String,
) : Expression(range),
    LeafExpression,
    TreeLeaf {
    override fun toString() = identifier

    override fun isEquivalent(other: Expression?): Boolean =
        super.isEquivalent(other) &&
            other is VariableUse &&
            identifier == other.identifier
}

sealed class Definition(
    range: Pair<Location, Location>,
    val identifier: String,
) : Expression(range) {
    override fun isEquivalent(other: Expression?): Boolean =
        super.isEquivalent(other) &&
            other is Definition &&
            identifier == other.identifier

    class VariableDeclaration(
        range: Pair<Location, Location>,
        identifier: String,
        val type: Type.Basic?,
        val value: Expression,
    ) : Definition(range, identifier),
        Tree {
        override fun toString() = "let $identifier${if (type == null) "" else ": $type"} "

        override fun children() = listOf(value)

        override fun isEquivalent(other: Expression?): Boolean =
            super.isEquivalent(other) &&
                other is VariableDeclaration &&
                areEquivalentTypes(type, other.type)
    }

    class FunctionDeclaration(
        range: Pair<Location, Location>,
        identifier: String,
        val type: Type.Functional?,
        val arguments: List<FunctionArgument>,
        val returnType: Type,
        val body: Expression,
    ) : Definition(range, identifier),
        Tree {
        override fun toString() = "let $identifier${if (type == null) "" else ": $type"} = [${arguments.joinToString(", ")}] -> $returnType"

        override fun children() = listOf(body)

        override fun isEquivalent(other: Expression?): Boolean =
            super.isEquivalent(other) &&
                other is FunctionDeclaration &&
                areEquivalentTypes(type, other.type) &&
                areEquivalentExpressions(arguments, other.arguments) &&
                areEquivalentTypes(returnType, other.returnType) &&
                areEquivalentExpressions(body, other.body)
    }

    class FunctionArgument(
        range: Pair<Location, Location>,
        identifier: String,
        val type: Type,
    ) : Definition(range, identifier),
        LeafExpression,
        TreeLeaf {
        override fun toString() = "$identifier: $type"

        override fun isEquivalent(other: Expression?): Boolean =
            super.isEquivalent(other) &&
                other is FunctionArgument &&
                areEquivalentTypes(type, other.type)
    }
}

class FunctionCall(
    range: Pair<Location, Location>,
    val function: Expression,
    val arguments: List<Expression>,
) : Expression(range),
    Tree {
    override fun toString() = "FunctionCall"

    override fun children() = listOf(function) + arguments

    override fun isEquivalent(other: Expression?): Boolean =
        super.isEquivalent(other) &&
            other is FunctionCall &&
            areEquivalentExpressions(function, other.function) &&
            areEquivalentExpressions(arguments, other.arguments)
}

sealed class Literal(
    range: Pair<Location, Location>,
) : Expression(range),
    LeafExpression {
    override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Literal

    class IntLiteral(
        range: Pair<Location, Location>,
        val value: Int,
    ) : Literal(range),
        TreeLeaf {
        override fun toString() = value.toString()

        override fun isEquivalent(other: Expression?): Boolean =
            super.isEquivalent(other) &&
                other is IntLiteral &&
                value == other.value
    }

    class BoolLiteral(
        range: Pair<Location, Location>,
        val value: Boolean,
    ) : Literal(range),
        TreeLeaf {
        override fun toString() = value.toString()

        override fun isEquivalent(other: Expression?): Boolean =
            super.isEquivalent(other) &&
                other is BoolLiteral &&
                value == other.value
    }
}

// expression in parentheses and whole program
class Block(
    range: Pair<Location, Location>,
    val expressions: List<Expression>,
) : Expression(range),
    Tree {
    override fun toString() = "BLOCK"

    override fun children() = expressions

    override fun isEquivalent(other: Expression?): Boolean =
        super.isEquivalent(other) &&
            other is Block &&
            areEquivalentExpressions(expressions, other.expressions)
}

sealed class Statement(
    range: Pair<Location, Location>,
) : Expression(range) {
    override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Statement

    class IfElseStatement(
        range: Pair<Location, Location>,
        val testExpression: Expression,
        val doExpression: Expression,
        val elseExpression: Expression?,
    ) : Statement(range),
        Tree {
        override fun toString() = "IfElseStmnt"

        override fun children() = listOfNotNull(testExpression, doExpression, elseExpression)
    }

    class WhileStatement(
        range: Pair<Location, Location>,
        val testExpression: Expression,
        val doExpression: Expression,
    ) : Statement(range),
        Tree {
        override fun toString() = "WhileStmnt"

        override fun children() = listOf(testExpression, doExpression)

        override fun isEquivalent(other: Expression?): Boolean =
            super.isEquivalent(other) &&
                other is WhileStatement &&
                areEquivalentExpressions(testExpression, other.testExpression) &&
                areEquivalentExpressions(doExpression, other.doExpression)
    }

    class ReturnStatement(
        range: Pair<Location, Location>,
        val value: Expression,
    ) : Statement(range),
        Tree {
        override fun toString() = "Return"

        override fun children() = listOf(value)

        override fun isEquivalent(other: Expression?): Boolean =
            super.isEquivalent(other) &&
                other is ReturnStatement &&
                areEquivalentExpressions(value, other.value)
    }

    class BreakStatement(
        range: Pair<Location, Location>,
    ) : Statement(range),
        LeafExpression,
        TreeLeaf {
        override fun toString() = "Break"

        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is BreakStatement
    }
}

sealed class OperatorUnary(
    range: Pair<Location, Location>,
    val expression: Expression,
) : Expression(range) {
    override fun isEquivalent(other: Expression?): Boolean =
        super.isEquivalent(other) &&
            other is OperatorUnary &&
            areEquivalentExpressions(expression, other.expression)

    override fun toString() = this::class.simpleName!!

    class Negation(
        range: Pair<Location, Location>,
        expression: Expression,
    ) : OperatorUnary(range, expression),
        Tree {
        override fun children() = listOf(expression)

        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Negation
    }

    class Minus(
        range: Pair<Location, Location>,
        expression: Expression,
    ) : OperatorUnary(range, expression),
        Tree {
        override fun children() = listOf(expression)

        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Minus
    }
}

sealed class OperatorBinary(
    range: Pair<Location, Location>,
    val lhs: Expression,
    val rhs: Expression,
) : Expression(range) {
    sealed class ArithmeticOperator(range: Pair<Location, Location>, lhs: Expression, rhs: Expression) : OperatorBinary(range, lhs, rhs)

    sealed class ArithmeticAssignmentOperator(range: Pair<Location, Location>, lhs: Expression, rhs: Expression) : OperatorBinary(
        range,
        lhs,
        rhs,
    )

    sealed class LogicalOperator(range: Pair<Location, Location>, lhs: Expression, rhs: Expression) : OperatorBinary(range, lhs, rhs)

    override fun toString() = this::class.simpleName!!

    override fun children() = listOf(lhs, rhs)

    override fun isEquivalent(other: Expression?): Boolean =
        super.isEquivalent(other) &&
            other is OperatorBinary &&
            areEquivalentExpressions(lhs, other.lhs) &&
            areEquivalentExpressions(rhs, other.rhs)

    class Multiplication(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Multiplication
    }

    class Division(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Division
    }

    class Modulo(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Modulo
    }

    class Addition(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Addition
    }

    class Subtraction(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Subtraction
    }

    class Less(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Less
    }

    class Greater(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Greater
    }

    class LessEqual(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is LessEqual
    }

    class GreaterEqual(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is GreaterEqual
    }

    class Equals(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Equals
    }

    class NotEquals(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is NotEquals
    }

    class LogicalAnd(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is LogicalAnd
    }

    class LogicalOr(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is LogicalOr
    }

    class Assignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : OperatorBinary(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is Assignment
    }

    class AdditionAssignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticAssignmentOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is AdditionAssignment
    }

    class SubtractionAssignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticAssignmentOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is SubtractionAssignment
    }

    class MultiplicationAssignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticAssignmentOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is MultiplicationAssignment
    }

    class DivisionAssignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticAssignmentOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is DivisionAssignment
    }

    class ModuloAssignment(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticAssignmentOperator(range, lhs, rhs) {
        override fun isEquivalent(other: Expression?): Boolean = super.isEquivalent(other) && other is ModuloAssignment
    }
}
