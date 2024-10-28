package cacophony.grammar.syntaxtree

import cacophony.utils.Location

sealed class Expression(val range: Pair<Location, Location>) { // everything in cacophony is an expression
    // artificial instance, can be useful when calculating values of nested expressions
    class EmptyExpression(
        range: Pair<Location, Location>,
    ) : Expression(range)

    class Variable(
        range: Pair<Location, Location>,
    ) : Expression(range)

    // should Type also inherit expression? if so, can we do for example "; Bool;" in our language?
    class Type(
        range: Pair<Location, Location>,
    ) : Expression(range)

    class VariableDeclaration(
        range: Pair<Location, Location>,
        val keywordLet: Keyword.KeywordLet,
    ) : Expression(range)

    class FunctionArgument(
        range: Pair<Location, Location>,
        type: Type,
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
        class KeywordBreak(
            range: Pair<Location, Location>,
        ) : Expression(range)

        // it is possible that following keywords are unnecessary, in this case break would be probably moved to statement
        class KeywordReturn(
            range: Pair<Location, Location>,
        ) : Expression(range)

        class KeywordLet(
            range: Pair<Location, Location>,
        ) : Expression(range)

        class KeywordIf(
            range: Pair<Location, Location>,
        ) : Expression(range)

        class KeywordThen(
            range: Pair<Location, Location>,
        ) : Expression(range)

        class KeywordElse(
            range: Pair<Location, Location>,
        ) : Expression(range)

        class KeywordWhile(
            range: Pair<Location, Location>,
        ) : Expression(range)

        class KeywordDo(
            range: Pair<Location, Location>,
        ) : Expression(range)
    }

    // expression in parenthesis
    class NestedExpression(
        range: Pair<Location, Location>,
        val expression: Expression,
    ) : Expression(range)

    // series of expressions separated by semicolons, returning value of the last one
    // they can maybe be merged with NestedExpression, but I don't know if that would be convenient
    // we agreed that (x; y;) returns Unit - should we create artificial class for empty expression after the last semicolon,
    // or will we handle it in a different way?
    class SubsequentExpressions(
        range: Pair<Location, Location>,
        vararg val expressions: Expression,
    ) : Expression(range)

    sealed class Statement(
        range: Pair<Location, Location>,
    ) : Expression(range) {
        class IfStatement(
            range: Pair<Location, Location>,
            val keywordIf: Keyword.KeywordIf,
            val testExpression: Expression,
            val keywordThen: Keyword.KeywordThen,
            val doExpression: Expression,
        ) : Statement(range)

        class IfElseStatement(
            range: Pair<Location, Location>,
            val keywordIf: Keyword.KeywordIf,
            val testExpression: Expression,
            val keywordThen: Keyword.KeywordThen,
            val doExpression: Expression,
            val keywordElse: Keyword.KeywordElse,
            val elseExpression: Expression,
        ) : Statement(range)

        class WhileStatement(
            range: Pair<Location, Location>,
            val keywordWhile: Keyword.KeywordWhile,
            val testExpression: Expression,
            val keywordDo: Keyword.KeywordDo,
            val doExpression: Expression,
        ) : Statement(range)

        class ReturnStatement(
            range: Pair<Location, Location>,
            val keywordReturn: Keyword.KeywordReturn,
            val value: Expression,
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
            val lhs: Expression,
            val rhs: Expression,
        ) : Operator(range) {
            class MultiplicationOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class DivisionOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class ModuloOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class AdditionOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class SubtractionOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class LessOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class GreaterOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class LessEqualOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class GreaterEqualOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class EqualsOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class NotEqualsOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class LogicalAndOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class LogicalOrOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class AssignmentOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class AdditionAssignmentOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class SubtractionAssignmentOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class MultiplicationAssignmentOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class DivisionAssignmentOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)

            class ModuloAssignmentOperator(
                range: Pair<Location, Location>,
                lhs: Expression,
                rhs: Expression,
            ) : BinaryOperator(range, lhs, rhs)
        }
    }
}
