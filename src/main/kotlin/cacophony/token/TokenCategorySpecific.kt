package cacophony.token

enum class TokenCategorySpecific {
    // punctuation
    LEFT_PARENTHESIS,
    RIGHT_PARENTHESIS,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    ARROW,
    DOUBLE_ARROW,
    COLON,
    SEMICOLON,
    COMMA,

    // keywords
    KEYWORD_LET,
    KEYWORD_IF,
    KEYWORD_THEN,
    KEYWORD_ELSE,
    KEYWORD_WHILE,
    KEYWORD_DO,
    KEYWORD_BREAK,
    KEYWORD_RETURN,

    // operators
    OPERATOR_EQUALS,
    OPERATOR_NOT_EQUALS,
    OPERATOR_ASSIGNMENT,
    OPERATOR_LESS,
    OPERATOR_GREATER,
    OPERATOR_LESS_EQUAL,
    OPERATOR_GREATER_EQUAL,
    OPERATOR_ADDITION,
    OPERATOR_SUBTRACTION,
    OPERATOR_ADDITION_ASSIGNMENT,
    OPERATOR_SUBTRACTION_ASSIGNMENT,
    OPERATOR_MULTIPLICATION,
    OPERATOR_DIVISION,
    OPERATOR_MODULO,
    OPERATOR_MULTIPLICATION_ASSIGNMENT,
    OPERATOR_DIVISION_ASSIGNMENT,
    OPERATOR_MODULO_ASSIGNMENT,
    OPERATOR_LOGICAL_OR,
    OPERATOR_LOGICAL_AND,
    OPERATOR_LOGICAL_NOT,

    // and the others
    LITERAL,
    TYPE_IDENTIFIER,
    VARIABLE_IDENTIFIER,
    WHITESPACE,
    COMMENT,
}
