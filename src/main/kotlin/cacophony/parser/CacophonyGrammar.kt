@file:Suppress("ktlint:standard:no-wildcard-imports")

package cacophony.parser

import cacophony.grammars.Grammar
import cacophony.grammars.produces
import cacophony.parser.CacophonyGrammarSymbol.*
import cacophony.utils.AlgebraicRegex.Companion.atomic

class CacophonyGrammar {
    companion object {
        val dummyGrammar1: Grammar<CacophonyGrammarSymbol> =
            Grammar(
                CacophonyGrammarSymbol.START,
                listOf(
                    CacophonyGrammarSymbol.START produces
                        (
                            (
                                atomic(CacophonyGrammarSymbol.LEFT_PARENTHESIS) concat
                                    atomic(CacophonyGrammarSymbol.START) concat
                                    atomic(CacophonyGrammarSymbol.RIGHT_PARENTHESIS)
                            ) or atomic(CacophonyGrammarSymbol.VARIABLE_IDENTIFIER)
                        ),
                ),
            )
        val grammar: Grammar<CacophonyGrammarSymbol> =
            Grammar(
                START,
                listOf(
                    START produces
                        (
                            (
                                atomic(STATEMENT_LEVEL) concat
                                    (
                                        atomic(SEMICOLON) concat
                                            atomic(STATEMENT_LEVEL)
                                    ).star()
                            ) or
                                (
                                    atomic(STATEMENT_LEVEL) concat
                                        atomic(SEMICOLON)
                                ).star()
                        ),
                    STATEMENT_LEVEL produces
                        (
                            atomic(DECLARATION_LEVEL) or
                                atomic(RETURN_STATEMENT) // or
                            // atomic(WHILE_CLAUSE) // or
                            // atomic(IF_CLAUSE)
                        ),
                    RETURN_STATEMENT produces
                        (
                            atomic(KEYWORD_RETURN)
                                concat atomic(STATEMENT_LEVEL)
                        ),
//                    FUNCTION_CALL produces (
//                        atomic(VARIABLE_IDENTIFIER) concat
//                            (atomic(LEFT_BRACKET) concat atomic(RIGHT_BRACKET)) or
//                            (atomic(LEFT_BRACKET) concat atomic(STATEMENT_LEVEL) concat atomic(RIGHT_BRACKET))
//                    ),
//                    WHILE_CLAUSE produces (
//                        atomic(KEYWORD_WHILE) concat atomic(STATEMENT_LEVEL) concat atomic(KEYWORD_DO) concat atomic(STATEMENT_LEVEL)
//                    ),
//                    IF_CLAUSE produces (
//                      (atomic(KEYWORD_IF) concat atomic(STATEMENT_LEVEL) concat atomic(KEYWORD_THEN) concat atomic(STATEMENT_LEVEL))
//                          or
//                          (
//                              atomic(KEYWORD_IF) concat atomic(STATEMENT_LEVEL) concat atomic(KEYWORD_THEN) concat
//                                  atomic(STATEMENT_LEVEL) concat
//                                  atomic(KEYWORD_ELSE) concat
//                                  atomic(STATEMENT_LEVEL)
//                          )
//                    ),
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
                                )
                        ),
                    DECLARATION_TYPED produces
                        (
                            atomic(COLON)
                                concat
                                atomic(TYPE_IDENTIFIER)
                                concat atomic(OPERATOR_ASSIGNMENT)
                                concat (
                                    atomic(
                                        VARIABLE_DECLARATION,
                                    ) or atomic(FUNCTION_DECLARATION)
                                )
                        ),
                    DECLARATION_UNTYPED produces
                        (
                            atomic(OPERATOR_ASSIGNMENT) concat (
                                atomic(
                                    VARIABLE_DECLARATION,
                                ) or atomic(FUNCTION_DECLARATION)
                            )

                        ),
                    VARIABLE_DECLARATION produces
                        (
                            atomic(STATEMENT_LEVEL)
                        ),
                    FUNCTION_DECLARATION produces
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
                                atomic(STATEMENT_LEVEL)
                        ),
                    FUNCTION_ARGUMENT produces (
                        atomic(VARIABLE_IDENTIFIER) concat atomic(COLON) concat atomic(TYPE)
                    ),
                    TYPE produces
                        (
                            atomic(TYPE_IDENTIFIER) or
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
                    ASSIGNMENT_LEVEL produces
                        (
                            atomic(FUNCTION_CALL) or
                                atomic(WHILE_CLAUSE) or
                                atomic(IF_CLAUSE) or
                                atomic(UNARY_LEVEL) or
                                (
                                    atomic(UNARY_LEVEL) concat
                                        atomic(ASSIGNMENT) concat
                                        atomic(ASSIGNMENT_LEVEL)
                                )
                        ),
                    ASSIGNMENT produces
                        (
                            atomic(OPERATOR_ASSIGNMENT) or
                                atomic(OPERATOR_ADDITION_ASSIGNMENT) or
                                atomic(OPERATOR_DIVISION_ASSIGNMENT) or
                                atomic(OPERATOR_MODULO_ASSIGNMENT) or
                                atomic(OPERATOR_MULTIPLICATION_ASSIGNMENT) or
                                atomic(OPERATOR_SUBTRACTION_ASSIGNMENT)
                        ),
                    UNARY_LEVEL produces
                        (
                            atomic(ATOM_LEVEL) or
                                (
                                    atomic(UNARY) concat
                                        atomic(ATOM_LEVEL)
                                )

                        ),
                    UNARY produces
                        (
                            atomic(OPERATOR_SUBTRACTION) or
                                (
                                    atomic(OPERATOR_LOGICAL_NOT)
                                )

                        ),
                    ATOM_LEVEL produces
                        (
                            atomic(KEYWORD_BREAK) or
                                atomic(VARIABLE_IDENTIFIER) or
                                atomic(BOOL_LITERAL) or
                                atomic(INT_LITERAL) or
                                atomic(BLOCK)
                        ),
                    BLOCK produces
                        (
                            atomic(LEFT_PARENTHESIS) concat (
                                (
                                    atomic(STATEMENT_LEVEL) concat
                                        (
                                            atomic(SEMICOLON) concat
                                                atomic(STATEMENT_LEVEL)
                                        ).star()
                                ) or
                                    (
                                        atomic(STATEMENT_LEVEL) concat
                                            atomic(SEMICOLON)
                                    ).star()
                            ) concat atomic(RIGHT_PARENTHESIS)
                        ),
                ),
            )
    }
}
