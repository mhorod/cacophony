package cacophony.semantic.syntaxtree

import cacophony.utils.Location
import cacophony.utils.Tree
import cacophony.utils.TreeLeaf

sealed interface SyntaxTree {
    val range: Pair<Location, Location>

    fun isEquivalent(other: SyntaxTree?): Boolean = other != null && range == other.range
}

sealed interface Expression : SyntaxTree, Tree

sealed interface Assignable : Expression

sealed interface LeafExpression : Expression, TreeLeaf

sealed interface Type : SyntaxTree

typealias AST = Expression

fun areEquivalentTypes(lhs: Type?, rhs: Type?): Boolean = lhs?.isEquivalent(rhs) ?: (rhs == null)

fun areEquivalentTypes(lhs: List<Type?>, rhs: List<Type?>): Boolean =
    lhs.size == rhs.size && lhs.zip(rhs).all { areEquivalentTypes(it.first, it.second) }

fun <T> areEquivalentTypes(lhs: Map<T, Type>, rhs: Map<T, Type>): Boolean =
    lhs.size == rhs.size && lhs.all { (k, type) -> areEquivalentTypes(type, rhs[k]) }

fun areEquivalentExpressions(lhs: Expression?, rhs: Expression?): Boolean = lhs?.isEquivalent(rhs) ?: (rhs == null)

fun areEquivalentExpressions(lhs: List<Expression?>, rhs: List<Expression?>): Boolean =
    lhs.size == rhs.size && lhs.zip(rhs).all { areEquivalentExpressions(it.first, it.second) }

fun <T> areEquivalentExpressions(lhs: Map<T, Expression>, rhs: Map<T, Expression>): Boolean =
    lhs.size == rhs.size && lhs.all { (k, expr) -> areEquivalentExpressions(expr, rhs[k]) }

sealed class BaseType(override val range: Pair<Location, Location>) : Type {
    override fun toString(): String = "${this::class.simpleName}@${Integer.toHexString(hashCode())}"

    class Basic(
        range: Pair<Location, Location>,
        val identifier: String,
    ) : BaseType(range) {
        override fun toString() = identifier

        override fun isEquivalent(other: SyntaxTree?): Boolean =
            super.isEquivalent(other) &&
                other is Basic &&
                identifier == other.identifier
    }

    class Functional(
        range: Pair<Location, Location>,
        val argumentsType: List<Type>,
        val returnType: Type,
    ) : BaseType(range) {
        override fun toString() = "[${argumentsType.joinToString(", ")}] => $returnType"

        override fun isEquivalent(other: SyntaxTree?): Boolean =
            super.isEquivalent(other) &&
                other is Functional &&
                areEquivalentTypes(argumentsType, other.argumentsType) &&
                areEquivalentTypes(returnType, other.returnType)
    }

    class Structural(range: Pair<Location, Location>, val fields: Map<String, Type>) : BaseType(range) {
        override fun toString() = "{${fields.map { (k, v) -> "$k: $v" }.joinToString(", ")}}"

        override fun isEquivalent(other: SyntaxTree?) =
            super.isEquivalent(other) &&
                other is Structural &&
                areEquivalentTypes(fields, other.fields)
    }
}

// everything in cacophony is an expression
sealed class BaseExpression(override val range: Pair<Location, Location>) : Expression {
    override fun toString(): String = "${this::class.simpleName}@${Integer.toHexString(hashCode())}"
}

// artificial instance, can be useful when calculating values of nested expressions
class Empty(range: Pair<Location, Location>) : BaseExpression(range), LeafExpression {
    override fun toString() = "empty"

    override fun isEquivalent(other: SyntaxTree?): Boolean = super<BaseExpression>.isEquivalent(other) && other is Empty
}

class VariableUse(
    range: Pair<Location, Location>,
    val identifier: String,
) : BaseExpression(range), Assignable, LeafExpression {
    override fun toString() = identifier

    override fun isEquivalent(other: SyntaxTree?): Boolean =
        super<BaseExpression>.isEquivalent(other) &&
            other is VariableUse &&
            identifier == other.identifier
}

sealed class Definition(
    range: Pair<Location, Location>,
    val identifier: String,
) : BaseExpression(range) {
    override fun isEquivalent(other: SyntaxTree?): Boolean =
        super.isEquivalent(other) &&
            other is Definition &&
            identifier == other.identifier

    class VariableDeclaration(
        range: Pair<Location, Location>,
        identifier: String,
        val type: BaseType.Basic?,
        val value: Expression,
    ) : Definition(range, identifier) {
        override fun toString() = "let $identifier${if (type == null) "" else ": $type"} "

        override fun children() = listOf(value)

        override fun isEquivalent(other: SyntaxTree?): Boolean =
            super.isEquivalent(other) &&
                other is VariableDeclaration &&
                areEquivalentTypes(type, other.type)
    }

    class FunctionDeclaration(
        range: Pair<Location, Location>,
        identifier: String,
        val type: BaseType.Functional?,
        val arguments: List<FunctionArgument>,
        val returnType: Type,
        val body: Expression,
    ) : Definition(range, identifier) {
        override fun toString() = "let $identifier${if (type == null) "" else ": $type"} = [${arguments.joinToString(", ")}] -> $returnType"

        override fun children() = listOf(body)

        override fun isEquivalent(other: SyntaxTree?): Boolean =
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
    ) : Definition(range, identifier), LeafExpression {
        override fun toString() = "$identifier: $type"

        override fun isEquivalent(other: SyntaxTree?): Boolean =
            super<Definition>.isEquivalent(other) &&
                other is FunctionArgument &&
                areEquivalentTypes(type, other.type)
    }
}

class FunctionCall(
    range: Pair<Location, Location>,
    val function: Expression,
    val arguments: List<Expression>,
) : BaseExpression(range) {
    override fun toString() = "FunctionCall"

    override fun children() = listOf(function) + arguments

    override fun isEquivalent(other: SyntaxTree?): Boolean =
        super.isEquivalent(other) &&
            other is FunctionCall &&
            areEquivalentExpressions(function, other.function) &&
            areEquivalentExpressions(arguments, other.arguments)
}

class StructField(range: Pair<Location, Location>, val name: String, val type: Type?) : BaseExpression(range), LeafExpression {
    override fun toString() = "field ${name}${type?.let{": $it"} ?: ""}"

    override fun isEquivalent(other: SyntaxTree?): Boolean =
        super<BaseExpression>.isEquivalent(other) &&
            other is StructField &&
            name == other.name &&
            areEquivalentTypes(type, other.type)

    override fun equals(other: Any?) = other is Expression? && this.isEquivalent(other)
}

sealed class FieldRef(range: Pair<Location, Location>, val field: String) : BaseExpression(range) {
    override fun children() = listOf(struct())

    abstract fun struct(): Expression

    override fun isEquivalent(other: SyntaxTree?): Boolean =
        super.isEquivalent(other) &&
            other is FieldRef &&
            field == other.field &&
            areEquivalentExpressions(struct(), other.struct())

    class LValue(range: Pair<Location, Location>, val obj: Assignable, field: String) : FieldRef(range, field), Assignable {
        override fun toString() = "assignable .$field"

        override fun struct() = obj

        override fun isEquivalent(other: SyntaxTree?) = other is FieldRef.LValue && super<FieldRef>.isEquivalent(other)
    }

    class RValue(range: Pair<Location, Location>, val obj: Expression, field: String) : FieldRef(range, field) {
        override fun toString() = "const .$field"

        override fun struct() = obj

        override fun isEquivalent(other: SyntaxTree?) = other is FieldRef.RValue && super.isEquivalent(other)
    }
}

class Struct(range: Pair<Location, Location>, val fields: Map<StructField, Expression>) : BaseExpression(range) {
    override fun toString() = "Struct"

    override fun children() = fields.entries.flatMap { (k, v) -> listOf(k, v) }

    override fun isEquivalent(other: SyntaxTree?): Boolean =
        super.isEquivalent(other) &&
            other is Struct &&
            areEquivalentExpressions(fields, other.fields)
}

sealed class Literal(range: Pair<Location, Location>) : BaseExpression(range), LeafExpression {
    override fun isEquivalent(other: SyntaxTree?): Boolean = super<BaseExpression>.isEquivalent(other) && other is Literal

    class IntLiteral(
        range: Pair<Location, Location>,
        val value: Int,
    ) : Literal(range) {
        override fun toString() = value.toString()

        override fun isEquivalent(other: SyntaxTree?): Boolean =
            super.isEquivalent(other) &&
                other is IntLiteral &&
                value == other.value
    }

    class BoolLiteral(
        range: Pair<Location, Location>,
        val value: Boolean,
    ) : Literal(range) {
        override fun toString() = value.toString()

        override fun isEquivalent(other: SyntaxTree?): Boolean =
            super.isEquivalent(other) &&
                other is BoolLiteral &&
                value == other.value
    }
}

// expression in parentheses and whole program
class Block(
    range: Pair<Location, Location>,
    val expressions: List<Expression>,
) : BaseExpression(range) {
    override fun toString() = "BLOCK"

    override fun children() = expressions

    override fun isEquivalent(other: SyntaxTree?): Boolean =
        super.isEquivalent(other) &&
            other is Block &&
            areEquivalentExpressions(expressions, other.expressions)
}

sealed class Statement(range: Pair<Location, Location>) : BaseExpression(range) {
    override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Statement

    class IfElseStatement(
        range: Pair<Location, Location>,
        val testExpression: Expression,
        val doExpression: Expression,
        val elseExpression: Expression?,
    ) : Statement(range) {
        override fun toString() = "IfElseStmnt"

        override fun children() = listOfNotNull(testExpression, doExpression, elseExpression)
    }

    class WhileStatement(
        range: Pair<Location, Location>,
        val testExpression: Expression,
        val doExpression: Expression,
    ) : Statement(range) {
        override fun toString() = "WhileStmnt"

        override fun children() = listOf(testExpression, doExpression)

        override fun isEquivalent(other: SyntaxTree?): Boolean =
            super.isEquivalent(other) &&
                other is WhileStatement &&
                areEquivalentExpressions(testExpression, other.testExpression) &&
                areEquivalentExpressions(doExpression, other.doExpression)
    }

    class ReturnStatement(
        range: Pair<Location, Location>,
        val value: Expression,
    ) : Statement(range) {
        override fun toString() = "Return"

        override fun children() = listOf(value)

        override fun isEquivalent(other: SyntaxTree?): Boolean =
            super.isEquivalent(other) &&
                other is ReturnStatement &&
                areEquivalentExpressions(value, other.value)
    }

    class BreakStatement(range: Pair<Location, Location>) : Statement(range), LeafExpression {
        override fun toString() = "Break"

        override fun isEquivalent(other: SyntaxTree?): Boolean = super<Statement>.isEquivalent(other) && other is BreakStatement
    }
}

sealed class OperatorUnary(
    range: Pair<Location, Location>,
    val expression: Expression,
) : BaseExpression(range) {
    override fun isEquivalent(other: SyntaxTree?): Boolean =
        super.isEquivalent(other) &&
            other is OperatorUnary &&
            areEquivalentExpressions(expression, other.expression)

    override fun toString() = this::class.simpleName!!

    class Negation(
        range: Pair<Location, Location>,
        expression: Expression,
    ) : OperatorUnary(range, expression) {
        override fun children() = listOf(expression)

        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Negation
    }

    class Minus(
        range: Pair<Location, Location>,
        expression: Expression,
    ) : OperatorUnary(range, expression) {
        override fun children() = listOf(expression)

        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Minus
    }
}

sealed class OperatorBinary(
    range: Pair<Location, Location>,
    val lhs: Expression,
    val rhs: Expression,
) : BaseExpression(range) {
    sealed class ArithmeticOperator(range: Pair<Location, Location>, lhs: Expression, rhs: Expression) : OperatorBinary(range, lhs, rhs)

    sealed class LValueOperator(range: Pair<Location, Location>, lhs: Assignable, rhs: Expression) : OperatorBinary(range, lhs, rhs)

    sealed class ArithmeticAssignmentOperator(range: Pair<Location, Location>, lhs: Assignable, rhs: Expression) :
        LValueOperator(range, lhs, rhs),
        Assignable

    sealed class LogicalOperator(range: Pair<Location, Location>, lhs: Expression, rhs: Expression) : OperatorBinary(range, lhs, rhs)

    override fun toString() = this::class.simpleName!!

    override fun children() = listOf(lhs, rhs)

    override fun isEquivalent(other: SyntaxTree?): Boolean =
        super.isEquivalent(other) &&
            other is OperatorBinary &&
            areEquivalentExpressions(lhs, other.lhs) &&
            areEquivalentExpressions(rhs, other.rhs)

    class Multiplication(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Multiplication
    }

    class Division(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Division
    }

    class Modulo(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Modulo
    }

    class Addition(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Addition
    }

    class Subtraction(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : ArithmeticOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Subtraction
    }

    class Less(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Less
    }

    class Greater(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Greater
    }

    class LessEqual(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is LessEqual
    }

    class GreaterEqual(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is GreaterEqual
    }

    class Equals(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is Equals
    }

    class NotEquals(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is NotEquals
    }

    class LogicalAnd(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is LogicalAnd
    }

    class LogicalOr(
        range: Pair<Location, Location>,
        lhs: Expression,
        rhs: Expression,
    ) : LogicalOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is LogicalOr
    }

    class Assignment(
        range: Pair<Location, Location>,
        lhs: Assignable,
        rhs: Expression,
    ) : LValueOperator(range, lhs, rhs), Assignable {
        override fun isEquivalent(other: SyntaxTree?): Boolean =
            super<OperatorBinary.LValueOperator>.isEquivalent(other) && other is Assignment
    }

    class AdditionAssignment(
        range: Pair<Location, Location>,
        lhs: Assignable,
        rhs: Expression,
    ) : ArithmeticAssignmentOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is AdditionAssignment
    }

    class SubtractionAssignment(
        range: Pair<Location, Location>,
        lhs: Assignable,
        rhs: Expression,
    ) : ArithmeticAssignmentOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is SubtractionAssignment
    }

    class MultiplicationAssignment(
        range: Pair<Location, Location>,
        lhs: Assignable,
        rhs: Expression,
    ) : ArithmeticAssignmentOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is MultiplicationAssignment
    }

    class DivisionAssignment(
        range: Pair<Location, Location>,
        lhs: Assignable,
        rhs: Expression,
    ) : ArithmeticAssignmentOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is DivisionAssignment
    }

    class ModuloAssignment(
        range: Pair<Location, Location>,
        lhs: Assignable,
        rhs: Expression,
    ) : ArithmeticAssignmentOperator(range, lhs, rhs) {
        override fun isEquivalent(other: SyntaxTree?): Boolean = super.isEquivalent(other) && other is ModuloAssignment
    }
}
