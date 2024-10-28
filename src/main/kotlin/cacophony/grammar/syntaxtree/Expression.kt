package cacophony.grammar.syntaxtree

import cacophony.utils.Location

sealed class Expression(val range: Pair<Location, Location>) { // everything in cacophony is an expression
    // artificial instance, can be useful when calculating values of nested expressions
    class Empty(
        range: Pair<Location, Location>,
    ) : Expression(range)

    class Variable(
        range: Pair<Location, Location>,
        val identifier: String,
    ) : Expression(range)

    // should Type also inherit expression? if so, can we do for example "; Bool;" in our language?
    class Type(
        range: Pair<Location, Location>,
        val identifier: String,
    ) : Expression(range)

    class VariableDeclaration(
        range: Pair<Location, Location>,
        val identifier: String,
    ) : Expression(range)

    class FunctionArgument(
        range: Pair<Location, Location>,
        val type: Type,
    )

    class VariableTypedDeclaration(
        range: Pair<Location, Location>,
        val variable: VariableDeclaration,
        val type: Type,
    ) : Expression(range)

    class Literal(
        range: Pair<Location, Location>,
    ) : Expression(range)

    // expression in parenthesis
    class ParenthesisGroup(
        range: Pair<Location, Location>,
        val expression: Expression,
    ) : Expression(range)

    // series of expressions separated by semicolons, returning value of the last one
    // they can maybe be merged with NestedExpression, but I don't know if that would be convenient
    // we agreed that (x; y;) returns Unit - should we create artificial class for empty expression after the last semicolon,
    // or will we handle it in a different way?
    class Subsequent(
        range: Pair<Location, Location>,
        vararg val expressions: Expression,
    ) : Expression(range)

    sealed class Statement(
        range: Pair<Location, Location>,
    ) : Expression(range) {
        class IfStatement(
            range: Pair<Location, Location>,
            val testExpression: Expression,
            val doExpression: Expression,
        ) : Statement(range)

        class IfElseStatement(
            range: Pair<Location, Location>,
            val testExpression: Expression,
            val doExpression: Expression,
            val elseExpression: Expression,
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

    class FunctionDefinition(
        range: Pair<Location, Location>,
        val functionName: VariableDeclaration,
        vararg val arguments: FunctionArgument,
        val returnType: Type,
    ) : Expression(range)

    class FunctionCall(
        range: Pair<Location, Location>,
        val function: Expression,
        vararg val arguments: Variable,
    ) : Expression(range)

    sealed class Operator(
        range: Pair<Location, Location>,
    ) : Expression(range) {
        sealed class Unary(
            range: Pair<Location, Location>,
            val expression: Expression,
        ) : Operator(range) {
            class Negation(
                range: Pair<Location, Location>,
                expression: Expression,
            ) : Unary(range, expression)

            class UnaryMinus(
                range: Pair<Location, Location>,
                expression: Expression,
            ) : Unary(range, expression)
        }

        sealed class Binary(
            range: Pair<Location, Location>,
            val lhs: Expression,
            val rhs: Expression,
        ) : Operator(range) {
            class Multiplication(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class Division(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class Modulo(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class Addition(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class Subtraction(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class Less(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class Greater(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class LessEqual(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class GreaterEqual(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class Equals(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class NotEquals(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class LogicalAnd(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class LogicalOr(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class Assignment(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class AdditionAssignment(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class SubtractionAssignment(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class MultiplicationAssignment(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class DivisionAssignment(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)

            class ModuloAssignment(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : Binary(range, lhs, rhs)
        }
    }
}
