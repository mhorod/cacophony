@file:Suppress("ktlint:standard:no-wildcard-imports")

package cacophony.parser

import cacophony.semantic.syntaxtree.*
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import kotlin.reflect.KClass

enum class CacophonyGrammarSymbol(
    val syntaxTreeClass: KClass<out Any>?,
) {
    // special characters
    LEFT_PARENTHESIS(null),
    RIGHT_PARENTHESIS(null),
    LEFT_BRACKET(null),
    RIGHT_BRACKET(null),
    ARROW(null),
    DOUBLE_ARROW(null),
    COLON(null),
    SEMICOLON(null),
    COMMA(null),

    // keywords
    KEYWORD_LET(null),
    KEYWORD_IF(null),
    KEYWORD_THEN(null),
    KEYWORD_ELSE(null),
    KEYWORD_WHILE(null),
    KEYWORD_DO(null),
    KEYWORD_BREAK(Statement.BreakStatement::class),
    KEYWORD_RETURN(Statement.ReturnStatement::class),

    // operators
    OPERATOR_EQUALS(OperatorBinary.Equals::class),
    OPERATOR_NOT_EQUALS(OperatorBinary.NotEquals::class),
    OPERATOR_ASSIGNMENT(OperatorBinary.Assignment::class),
    OPERATOR_LESS(OperatorBinary.Less::class),
    OPERATOR_GREATER(OperatorBinary.Greater::class),
    OPERATOR_LESS_EQUAL(OperatorBinary.LessEqual::class),
    OPERATOR_GREATER_EQUAL(OperatorBinary.GreaterEqual::class),
    OPERATOR_ADDITION(OperatorBinary.Addition::class),
    OPERATOR_SUBTRACTION(OperatorBinary.Subtraction::class),
    OPERATOR_ADDITION_ASSIGNMENT(OperatorBinary.AdditionAssignment::class),
    OPERATOR_SUBTRACTION_ASSIGNMENT(OperatorBinary.SubtractionAssignment::class),
    OPERATOR_MULTIPLICATION(OperatorBinary.Multiplication::class),
    OPERATOR_DIVISION(OperatorBinary.Division::class),
    OPERATOR_MODULO(OperatorBinary.Modulo::class),
    OPERATOR_MULTIPLICATION_ASSIGNMENT(OperatorBinary.MultiplicationAssignment::class),
    OPERATOR_DIVISION_ASSIGNMENT(OperatorBinary.DivisionAssignment::class),
    OPERATOR_MODULO_ASSIGNMENT(OperatorBinary.ModuloAssignment::class),
    OPERATOR_LOGICAL_OR(OperatorBinary.LogicalOr::class),
    OPERATOR_LOGICAL_AND(OperatorBinary.LogicalAnd::class),
    OPERATOR_LOGICAL_NOT(OperatorUnary.Negation::class),

    // literals
    INT_LITERAL(Literal.IntLiteral::class),
    BOOL_LITERAL(Literal.BoolLiteral::class),
    TYPE_IDENTIFIER(Type.Basic::class),
    VARIABLE_IDENTIFIER(null),

    // others
    START(Block::class),
    FUNCTION_CALL(FunctionCall::class),
    WHILE_CLAUSE(Statement.WhileStatement::class),
    IF_CLAUSE(Statement.IfElseStatement::class),
    DECLARATION_TYPED(Definition::class),
    DECLARATION_UNTYPED(Definition::class),
    VARIABLE_DECLARATION(Definition.VariableDeclaration::class),
    FUNCTION_DECLARATION(Definition.FunctionDeclaration::class),
    FUNCTION_ARGUMENT(Definition.FunctionArgument::class),
    TYPE(Type::class),
    ASSIGNMENT(OperatorBinary::class),
    UNARY(OperatorUnary::class),

//    STATEMENT(Expression::class),
    RETURN_STATEMENT(Expression::class),
    BLOCK(Block::class),

    // levels
    STATEMENT_LEVEL(null),
    SEMICOLON_LEVEL(null),
    DECLARATION_LEVEL(null),
    ASSIGNMENT_LEVEL(null),
    LOGICAL_OPERATOR_LEVEL(null),
    COMPARATOR_LEVEL(null),
    EQUALITY_LEVEL(null),
    ADDITION_LEVEL(null),
    MULTIPLICATION_LEVEL(null),
    UNARY_LEVEL(null),
    CALL_LEVEL(null),
    ATOM_LEVEL(null),
    ;

    companion object {
        fun fromLexerToken(lexerToken: Token<TokenCategorySpecific>): Token<CacophonyGrammarSymbol> =
            Token(CacophonyGrammarSymbol.valueOf(lexerToken.category.name), lexerToken.context, lexerToken.rangeFrom, lexerToken.rangeTo)
    }
}
