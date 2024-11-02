package cacophony.semantic.syntaxtree

import cacophony.utils.Location
import cacophony.utils.Tree
import cacophony.utils.TreeLeaf

typealias AST = Block

sealed class Type(
    val range: Pair<Location, Location>,
) {
    class Basic(
        range: Pair<Location, Location>,
        val identifier: String,
    ) : Type(range) {
        override fun toString() = identifier
    }

    class Functional(
        range: Pair<Location, Location>,
        val argumentsType: List<Type>,
        val returnType: Type,
    ) : Type(range) {
        override fun toString() = "[${argumentsType.joinToString(", ")}] => $returnType"
    }
}

// everything in cacophony is an expression
sealed class Expression(
    val range: Pair<Location, Location>,
) : Tree

// artificial instance, can be useful when calculating values of nested expressions
class Empty(
    range: Pair<Location, Location>,
) : Expression(range),
    TreeLeaf {
    override fun toString() = "empty"
}

class VariableUse(
    range: Pair<Location, Location>,
    val identifier: String,
) : Expression(range),
    TreeLeaf {
    override fun toString() = identifier
}

sealed class Definition(
    range: Pair<Location, Location>,
    val identifier: String,
) : Expression(range) {
    class VariableDeclaration(
        range: Pair<Location, Location>,
        identifier: String,
        val type: Type?,
        val expression: Expression,
    ) : Definition(range, identifier),
        Tree {
        override fun toString() = "let $identifier${if (type == null) "" else ": $type"} "

        override fun children() = listOf(expression)

        override fun isLeaf() = false
    }

    class FunctionDeclaration(
        range: Pair<Location, Location>,
        identifier: String,
        val type: Type?,
        val arguments: List<FunctionArgument>,
        val returnType: Type,
        val body: Expression,
    ) : Definition(range, identifier),
        Tree {
        override fun toString() = "let $identifier${if (type == null) "" else ": $type"} = [${arguments.joinToString(", ")}] -> $returnType"

        override fun children() = listOf(body)

        override fun isLeaf() = false
    }

    class FunctionArgument(
        range: Pair<Location, Location>,
        identifier: String,
        val type: Type,
    ) : Definition(range, identifier),
        TreeLeaf {
        override fun toString() = "$identifier: $type"
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

    override fun isLeaf() = false
}

sealed class Literal(
    range: Pair<Location, Location>,
) : Expression(range) {
    class IntLiteral(
        range: Pair<Location, Location>,
        val value: Int,
    ) : Literal(range),
        TreeLeaf {
        override fun toString() = value.toString()
    }

    class BoolLiteral(
        range: Pair<Location, Location>,
        val value: Boolean,
    ) : Literal(range),
        TreeLeaf {
        override fun toString() = value.toString()
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

    override fun isLeaf() = false
}

sealed class Statement(
    range: Pair<Location, Location>,
) : Expression(range) {
    class IfElseStatement(
        range: Pair<Location, Location>,
        val testExpression: Expression,
        val doExpression: Expression,
        val elseExpression: Expression?,
    ) : Statement(range),
        Tree {
        override fun toString() = "IfElseStmnt"

        override fun children() = listOfNotNull(testExpression, doExpression, elseExpression)

        override fun isLeaf() = false
    }

    class WhileStatement(
        range: Pair<Location, Location>,
        val testExpression: Expression,
        val doExpression: Expression,
    ) : Statement(range),
        Tree {
        override fun toString() = "WhileStmnt"

        override fun children() = listOf(testExpression, doExpression)

        override fun isLeaf() = false
    }

    class ReturnStatement(
        range: Pair<Location, Location>,
        val value: Expression,
    ) : Statement(range),
        Tree {
        override fun toString() = "Return"

        override fun children() = listOf(value)

        override fun isLeaf() = false
    }

    class BreakStatement(
        range: Pair<Location, Location>,
    ) : Statement(range),
        TreeLeaf {
        override fun toString() = "Break"
    }
}

sealed class OperatorUnary(
    range: Pair<Location, Location>,
    val expression: Expression,
) : Expression(range) {
    class Negation(
        range: Pair<Location, Location>,
        expression: Expression,
    ) : OperatorUnary(range, expression),
        Tree {
        override fun toString() = "Negation"

        override fun children() = listOf(expression)

        override fun isLeaf() = false
    }

    class Minus(
        range: Pair<Location, Location>,
        expression: Expression,
    ) : OperatorUnary(range, expression),
        Tree {
        override fun toString() = "Unary minus"

        override fun children() = listOf(expression)

        override fun isLeaf() = false
    }
}

sealed class OperatorBinary(
    range: Pair<Location, Location>,
    val lhs: Expression,
    val rhs: Expression,
) : Expression(range) {
    // TODO : do sth smarter
    override fun toString() = this::class.simpleName!!

    override fun children() = listOf(lhs, rhs)

    override fun isLeaf() = false

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
