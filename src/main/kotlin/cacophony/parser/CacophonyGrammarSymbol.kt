package cacophony.parser

import cacophony.semantic.syntaxtree.*
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import kotlin.reflect.KClass

enum class CacophonyGrammarSymbol(
    val syntaxTreeClass: KClass<out SyntaxTree>?,
) {
    // special characters
    LEFT_PARENTHESIS(null),
    RIGHT_PARENTHESIS(null),
    LEFT_BRACKET(null),
    RIGHT_BRACKET(null),
    LEFT_CURLY_BRACE(null),
    RIGHT_CURLY_BRACE(null),
    ARROW(null),
    DOUBLE_ARROW(null),
    COLON(null),
    SEMICOLON(null),
    COMMA(null),
    PERIOD(null),
    AMPERSAND(null),

    // keywords
    KEYWORD_LET(null),
    KEYWORD_IF(null),
    KEYWORD_THEN(null),
    KEYWORD_ELSE(null),
    KEYWORD_WHILE(null),
    KEYWORD_DO(null),
    KEYWORD_BREAK(Statement.BreakStatement::class),
    KEYWORD_RETURN(null),
    KEYWORD_FOREIGN(null),

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
    AT(Dereference::class),
    DOLLAR(Allocation::class),

    // literals
    INT_LITERAL(Literal.IntLiteral::class),
    BOOL_LITERAL(Literal.BoolLiteral::class),
    TYPE_IDENTIFIER(BaseType.Basic::class),
    VARIABLE_IDENTIFIER(VariableUse::class),

    // others
    START(Block::class),
    FUNCTION_CALL(FunctionCall::class),
    STRUCT(Struct::class),
    STRUCT_FIELD(StructField::class),
    STRUCT_FIELD_VALUE_TYPED(StructField::class),
    STRUCT_FIELD_VALUE_UNTYPED(StructField::class),
    WHILE_CLAUSE(Statement.WhileStatement::class),
    IF_CLAUSE(Statement.IfElseStatement::class),
    DECLARATION_TYPED(Definition::class),
    DECLARATION_UNTYPED(Definition::class),
    VARIABLE_DECLARATION(Definition.VariableDeclaration::class),
    LAMBDA_EXPRESSION(Definition.FunctionDefinition::class),
    FOREIGN_DECLARATION(Definition.ForeignFunctionDeclaration::class),
    FUNCTION_ARGUMENT(Definition.FunctionArgument::class),
    TYPE(Type::class),
    FUNCTION_TYPE(BaseType.Functional::class),
    STRUCT_TYPE(BaseType.Structural::class),
    REFERENCE_TYPE(BaseType.Referential::class),
    ASSIGNMENT(OperatorBinary::class),
    UNARY(OperatorUnary::class),

    //    STATEMENT(Expression::class),
    RETURN_STATEMENT(Statement.ReturnStatement::class),
    BLOCK(Block::class),
    EMPTY_EXPRESSION(Empty::class),
    NON_EXISTENT_SYMBOL(null),

    // levels
    SEMICOLON_LEVEL(null),
    DECLARATION_LEVEL(null),
    ASSIGNMENT_LEVEL(null),
    LOGICAL_OPERATOR_LEVEL(null),
    COMPARATOR_LEVEL(null),
    EQUALITY_LEVEL(null),
    ADDITION_LEVEL(null),
    MULTIPLICATION_LEVEL(null),
    UNARY_LEVEL(null),
    STATEMENT_LEVEL(null),
    ALLOCATION_LEVEL(null),
    CALL_LEVEL(null),
    DEREFERENCE_LEVEL(null),
    LITERAL_LEVEL(null),
    ATOM_LEVEL(null),
    ;

    companion object {
        fun fromLexerToken(lexerToken: Token<TokenCategorySpecific>): Token<CacophonyGrammarSymbol> =
            Token(valueOf(lexerToken.category.name), lexerToken.context, lexerToken.rangeFrom, lexerToken.rangeTo)
    }
}
