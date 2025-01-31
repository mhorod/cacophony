package cacophony.parser

import cacophony.grammars.Grammar
import cacophony.grammars.produces
import cacophony.parser.CacophonyGrammarSymbol.*
import cacophony.utils.AlgebraicRegex.Companion.atomic

class CacophonyGrammar {
    companion object {
        val grammar: Grammar<CacophonyGrammarSymbol> =
            Grammar(
                START,
                listOf(
                    START produces
                        (
                            atomic(SEMICOLON).star() concat (
                                (
                                    atomic(DECLARATION_LEVEL) concat
                                        (
                                            (atomic(SEMICOLON) concat atomic(SEMICOLON).star()) concat
                                                atomic(DECLARATION_LEVEL)
                                        ).star()
                                ) or
                                    (
                                        atomic(DECLARATION_LEVEL) concat
                                            (atomic(SEMICOLON) concat atomic(SEMICOLON).star())
                                    ).star()
                            )
                        ),
                    RETURN_STATEMENT produces
                        (
                            atomic(KEYWORD_RETURN)
                                concat atomic(DECLARATION_LEVEL)
                        ),
                    FUNCTION_CALL produces (
                        (atomic(LEFT_BRACKET) concat atomic(RIGHT_BRACKET)) or
                            (
                                atomic(LEFT_BRACKET) concat
                                    atomic(DECLARATION_LEVEL) concat
                                    (atomic(COMMA) concat atomic(DECLARATION_LEVEL)).star() concat
                                    atomic(RIGHT_BRACKET)
                            )
                    ),
                    WHILE_CLAUSE produces (
                        atomic(KEYWORD_WHILE) concat atomic(DECLARATION_LEVEL) concat atomic(KEYWORD_DO) concat atomic(DECLARATION_LEVEL)
                    ),
                    IF_CLAUSE produces (
                        (atomic(KEYWORD_IF) concat atomic(DECLARATION_LEVEL) concat atomic(KEYWORD_THEN) concat atomic(DECLARATION_LEVEL))
                            or
                            (
                                atomic(KEYWORD_IF) concat atomic(DECLARATION_LEVEL) concat atomic(KEYWORD_THEN) concat
                                    atomic(DECLARATION_LEVEL) concat
                                    atomic(KEYWORD_ELSE) concat
                                    atomic(DECLARATION_LEVEL)
                            )
                    ),
                    DECLARATION_LEVEL produces
                        (
                            atomic(ASSIGNMENT_LEVEL) or
                                (
                                    atomic(KEYWORD_LET) concat
                                        atomic(VARIABLE_IDENTIFIER) concat
                                        (
                                            atomic(DECLARATION_TYPED)
                                                or
                                                atomic(DECLARATION_UNTYPED)
                                        )
                                ) or
                                (
                                    atomic(KEYWORD_FOREIGN) concat
                                        atomic(VARIABLE_IDENTIFIER) concat
                                        atomic(FOREIGN_DECLARATION)
                                )
                        ),
                    FOREIGN_DECLARATION produces
                        (
                            atomic(COLON) concat
                                atomic(TYPE)
                        ),
                    DECLARATION_TYPED produces
                        (
                            atomic(COLON)
                                concat
                                atomic(TYPE)
                                concat atomic(OPERATOR_ASSIGNMENT)
                                concat atomic(VARIABLE_DECLARATION)
                        ),
                    DECLARATION_UNTYPED produces
                        (
                            atomic(OPERATOR_ASSIGNMENT) concat atomic(VARIABLE_DECLARATION)
                        ),
                    VARIABLE_DECLARATION produces
                        (
                            atomic(DECLARATION_LEVEL)
                        ),
                    LAMBDA_EXPRESSION produces
                        (
                            atomic(LEFT_BRACKET)
                                concat
                                (
                                    (
                                        atomic(FUNCTION_ARGUMENT) concat
                                            (
                                                atomic(COMMA) concat
                                                    atomic(FUNCTION_ARGUMENT)
                                            ).star()
                                            concat
                                            atomic(RIGHT_BRACKET)
                                    )
                                        or atomic(RIGHT_BRACKET)
                                )
                                concat
                                atomic(ARROW)
                                concat
                                atomic(TYPE)
                                concat
                                atomic(DOUBLE_ARROW)
                                concat
                                atomic(DECLARATION_LEVEL)
                        ),
                    FUNCTION_ARGUMENT produces (
                        atomic(VARIABLE_IDENTIFIER) concat atomic(COLON) concat atomic(TYPE)
                    ),
                    STRUCT produces (
                        (
                            atomic(LEFT_CURLY_BRACE) concat
                                atomic(STRUCT_FIELD) concat (atomic(COMMA) concat atomic(STRUCT_FIELD)).star()
                                concat atomic(RIGHT_CURLY_BRACE)
                        ) or (atomic(LEFT_CURLY_BRACE) concat atomic(RIGHT_CURLY_BRACE))
                    ),
                    STRUCT_FIELD produces (
                        atomic(VARIABLE_IDENTIFIER) concat
                            (atomic(STRUCT_FIELD_VALUE_TYPED) or atomic(STRUCT_FIELD_VALUE_UNTYPED))
                    ),
                    STRUCT_FIELD_VALUE_TYPED produces
                        (
                            atomic(COLON)
                                concat
                                atomic(TYPE)
                                concat atomic(OPERATOR_ASSIGNMENT)
                                concat atomic(DECLARATION_LEVEL)
                        ),
                    STRUCT_FIELD_VALUE_UNTYPED produces
                        (
                            atomic(OPERATOR_ASSIGNMENT)
                                concat atomic(DECLARATION_LEVEL)
                        ),
                    TYPE produces
                        (
                            atomic(TYPE_IDENTIFIER) or
                                atomic(FUNCTION_TYPE) or
                                atomic(STRUCT_TYPE) or
                                atomic(REFERENCE_TYPE)
                        ),
                    FUNCTION_TYPE produces (
                        (
                            atomic(LEFT_BRACKET) concat
                                (
                                    atomic(TYPE) concat
                                        (
                                            atomic(COMMA) concat
                                                atomic(TYPE)
                                        ).star()
                                ) concat
                                atomic(RIGHT_BRACKET) concat
                                atomic(ARROW) concat
                                atomic(TYPE)
                        ) or
                            (
                                atomic(LEFT_BRACKET) concat
                                    atomic(RIGHT_BRACKET) concat
                                    atomic(ARROW) concat
                                    atomic(TYPE)
                            )
                    ),
                    STRUCT_TYPE produces (
                        (
                            atomic(LEFT_CURLY_BRACE) concat
                                (
                                    atomic(VARIABLE_IDENTIFIER) concat atomic(COLON) concat atomic(TYPE) concat
                                        (
                                            atomic(COMMA) concat atomic(VARIABLE_IDENTIFIER) concat atomic(COLON) concat
                                                atomic(TYPE)
                                        ).star()
                                ) concat
                                atomic(RIGHT_CURLY_BRACE)
                        ) or (
                            atomic(LEFT_CURLY_BRACE) concat atomic(RIGHT_CURLY_BRACE)
                        )

                    ),
                    REFERENCE_TYPE produces (
                        atomic(AMPERSAND) concat atomic(TYPE)
                    ),
                    ASSIGNMENT_LEVEL produces
                        (
                            atomic(LOGICAL_OPERATOR_LEVEL) or
                                (
                                    atomic(LOGICAL_OPERATOR_LEVEL) concat
                                        (
                                            atomic(OPERATOR_ASSIGNMENT) or
                                                atomic(OPERATOR_ADDITION_ASSIGNMENT) or
                                                atomic(OPERATOR_DIVISION_ASSIGNMENT) or
                                                atomic(OPERATOR_MODULO_ASSIGNMENT) or
                                                atomic(OPERATOR_MULTIPLICATION_ASSIGNMENT) or
                                                atomic(OPERATOR_SUBTRACTION_ASSIGNMENT)
                                        ) concat
                                        atomic(ASSIGNMENT_LEVEL)
                                )
                        ),
                    LOGICAL_OPERATOR_LEVEL produces
                        (
                            atomic(COMPARATOR_LEVEL) concat
                                (
                                    (
                                        atomic(OPERATOR_LOGICAL_OR) or
                                            atomic(OPERATOR_LOGICAL_AND)
                                    ) concat
                                        atomic(COMPARATOR_LEVEL)
                                ).star()
                        ),
                    OPERATOR_LOGICAL_AND produces
                        (
                            atomic(AMPERSAND) concat atomic(AMPERSAND)
                        ),
                    COMPARATOR_LEVEL produces
                        (
                            atomic(EQUALITY_LEVEL) concat
                                (
                                    (
                                        atomic(OPERATOR_LESS) or
                                            atomic(OPERATOR_LESS_EQUAL) or
                                            atomic(OPERATOR_GREATER) or
                                            atomic(OPERATOR_GREATER_EQUAL)
                                    ) concat
                                        atomic(EQUALITY_LEVEL)
                                ).star()
                        ),
                    EQUALITY_LEVEL produces
                        (
                            atomic(ADDITION_LEVEL) concat
                                (
                                    (
                                        atomic(OPERATOR_EQUALS) or
                                            atomic(OPERATOR_NOT_EQUALS)
                                    ) concat
                                        atomic(ADDITION_LEVEL)
                                ).star()
                        ),
                    ADDITION_LEVEL produces
                        (
                            atomic(MULTIPLICATION_LEVEL) concat
                                (
                                    (
                                        atomic(OPERATOR_ADDITION) or
                                            atomic(OPERATOR_SUBTRACTION)
                                    ) concat
                                        atomic(MULTIPLICATION_LEVEL)
                                ).star()
                        ),
                    MULTIPLICATION_LEVEL produces
                        (
                            atomic(UNARY_LEVEL) concat
                                (
                                    (
                                        atomic(OPERATOR_MULTIPLICATION) or
                                            atomic(OPERATOR_DIVISION) or
                                            atomic(OPERATOR_MODULO)
                                    ) concat
                                        atomic(UNARY_LEVEL)
                                ).star()
                        ),
                    UNARY_LEVEL produces
                        (
                            atomic(STATEMENT_LEVEL) or
                                (
                                    atomic(UNARY) concat
                                        atomic(LITERAL_LEVEL)
                                )

                        ),
                    UNARY produces
                        (
                            atomic(OPERATOR_SUBTRACTION) or
                                (
                                    atomic(OPERATOR_LOGICAL_NOT)
                                )

                        ),
                    STATEMENT_LEVEL produces
                        (
                            atomic(ALLOCATION_LEVEL) or
                                atomic(RETURN_STATEMENT) or
                                atomic(WHILE_CLAUSE) or
                                atomic(IF_CLAUSE) or
                                atomic(LAMBDA_EXPRESSION)
                        ),
                    ALLOCATION_LEVEL produces
                        (
                            atomic(CALL_LEVEL) or
                                (atomic(DOLLAR) concat atomic(ALLOCATION_LEVEL))
                        ),
                    CALL_LEVEL produces
                        (
                            atomic(DEREFERENCE_LEVEL) concat
                                (
                                    atomic(FUNCTION_CALL) or
                                        (atomic(PERIOD) concat atomic(VARIABLE_IDENTIFIER))
                                ).star()

                        ),
                    DEREFERENCE_LEVEL produces
                        (
                            atomic(LITERAL_LEVEL) or
                                (atomic(AT) concat atomic(DEREFERENCE_LEVEL))
                        ),
                    LITERAL_LEVEL produces
                        (
                            atomic(KEYWORD_BREAK) or
                                atomic(BOOL_LITERAL) or
                                atomic(INT_LITERAL) or
                                atomic(ATOM_LEVEL)
                        ),
                    ATOM_LEVEL produces
                        (
                            atomic(VARIABLE_IDENTIFIER) or
                                atomic(STRUCT) or
                                atomic(BLOCK)
                        ),
                    BLOCK produces
                        (
                            atomic(LEFT_PARENTHESIS) concat (
                                atomic(SEMICOLON).star() concat (
                                    (
                                        atomic(DECLARATION_LEVEL) concat
                                            (
                                                (atomic(SEMICOLON) concat atomic(SEMICOLON).star()) concat
                                                    atomic(DECLARATION_LEVEL)
                                            ).star()
                                    ) or
                                        (
                                            atomic(DECLARATION_LEVEL) concat
                                                (atomic(SEMICOLON) concat atomic(SEMICOLON).star())
                                        ).star()
                                )
                            ) concat atomic(RIGHT_PARENTHESIS)

                        ),
                ),
            )
    }
}
