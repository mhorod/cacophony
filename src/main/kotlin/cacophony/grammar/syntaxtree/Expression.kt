package cacophony.grammar.syntaxtree

import cacophony.utils.Location

sealed class Expression(val range: Pair<Location, Location>) { // everything in cacophony is an expression
    class Variable(
        range: Pair<Location, Location>,
    ) : Expression(range)

    class Type( // should Type also inherit expression? if so, can we do for example "; Bool;" in our language?
        range: Pair<Location, Location>,
    ) : Expression(range)

    class VariableDeclaration(
        range: Pair<Location, Location>,
    ) : Expression(range)

    class VariableTypedDeclaration(
        range: Pair<Location, Location>,
        variable: VariableDeclaration,
        type: Type,
    ) : Expression(range)

    class Literal(
        range: Pair<Location, Location>,
    ) : Expression(range)

    class Keyword(
        range: Pair<Location, Location>,
    ) : Expression(range) {
        class KeywordReturn(
            range: Pair<Location, Location>,
        ) : Expression(range)

        class KeywordBreak(
            range: Pair<Location, Location>,
        ) : Expression(range)
    }

    class NestedExpression( // expression in parenthesis
        range: Pair<Location, Location>,
        val expression: Expression,
    ) : Expression(range)

    class SubsequentExpressions( // series of expressions separated by semicolons, returning value of the last one
        // we agreed that (x; y;) returns Unit - should we create artificial class for empty expression after the last semicolon,
        // or will we handle it in a different way?
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
    }

    class FunctionDefinition(
        range: Pair<Location, Location>,
        val functionName: VariableDeclaration,
        vararg val arguments: VariableDeclaration,
        val returnType: Type,
    ) : Expression(range)

    class FunctionCall(
        range: Pair<Location, Location>,
        val functionName: Variable,
        vararg val arguments: Variable,
    ) : Expression(range)

    sealed class Operator(
        range: Pair<Location, Location>,
    ) : Expression(range) {
        sealed class UnaryOperator(
            range: Pair<Location, Location>,
            val expression: Expression,
        ) : Operator(range) {
            class NegationOperator(
                range: Pair<Location, Location>,
                expression: Expression,
            ) : UnaryOperator(range, expression)

            class UnaryMinusOperator(
                range: Pair<Location, Location>,
                expression: Expression,
            ) : UnaryOperator(range, expression)
        }

        sealed class BinaryOperator(
            range: Pair<Location, Location>,
            val expression1: Expression,
            val expression2: Expression,
        ) : Operator(range) {
            class MultiplicationOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class DivisionOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class ModuloOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class AdditionOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class SubtractionOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class LessOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class GreaterOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class LessEqualOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class GreaterEqualOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class EqualsOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class NotEqualsOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class LogicalAndOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class LogicalOrOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class AssignmentOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class AdditionAssignmentOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class SubtractionAssignmentOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class MultiplicationAssignmentOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class DivisionAssignmentOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)

            class ModuloAssignmentOperator(
                range: Pair<Location, Location>,
                expression1: Expression,
                expression2: Expression,
            ) : BinaryOperator(range, expression1, expression2)
        }
    }
}
