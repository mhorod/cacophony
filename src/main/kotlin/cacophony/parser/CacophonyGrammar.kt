package cacophony.parser

import cacophony.grammars.Grammar
import cacophony.grammars.produces
import cacophony.utils.AlgebraicRegex.Companion.atomic

class CacophonyGrammar {
    companion object {
        val dummyGrammar0: Grammar<CacophonyGrammarSymbol> =
            Grammar(
                CacophonyGrammarSymbol.A,
                listOf(CacophonyGrammarSymbol.A produces atomic(CacophonyGrammarSymbol.VARIABLE_IDENTIFIER)),
            )

        // the simple parens grammar.
        val dummyGrammar1: Grammar<CacophonyGrammarSymbol> =
            Grammar(
                CacophonyGrammarSymbol.A,
                listOf(
                    CacophonyGrammarSymbol.A produces
                        (
                            (
                                atomic(CacophonyGrammarSymbol.LEFT_PARENTHESIS) concat
                                    atomic(CacophonyGrammarSymbol.A) concat
                                    atomic(CacophonyGrammarSymbol.RIGHT_PARENTHESIS) concat
                                    atomic(CacophonyGrammarSymbol.A)
                            ) or atomic(CacophonyGrammarSymbol.VARIABLE_IDENTIFIER)
                        ),
                ),
            )
        val grammar: Grammar<CacophonyGrammarSymbol> =
            Grammar(
                CacophonyGrammarSymbol.START,
                listOf(
                    CacophonyGrammarSymbol.START produces
                        (
                            (
                                atomic(CacophonyGrammarSymbol.RETURN_LEVEL) concat
                                    (
                                        atomic(CacophonyGrammarSymbol.SEMICOLON) concat
                                            atomic(CacophonyGrammarSymbol.RETURN_LEVEL)
                                    ).star()
                            ) or
                                (
                                    atomic(CacophonyGrammarSymbol.RETURN_LEVEL) concat
                                        atomic(CacophonyGrammarSymbol.SEMICOLON)
                                ).star()
                        ),
                    CacophonyGrammarSymbol.RETURN_LEVEL produces
                        (
                            atomic(CacophonyGrammarSymbol.DECLARATION_LEVEL) or
                                (
                                    atomic(CacophonyGrammarSymbol.KEYWORD_RETURN)
                                        concat atomic(CacophonyGrammarSymbol.DECLARATION_LEVEL)
                                )
                        ),
                    CacophonyGrammarSymbol.DECLARATION_LEVEL produces
                        (
                            atomic(CacophonyGrammarSymbol.ASSIGNMENT_LEVEL) or
                                (
                                    atomic(CacophonyGrammarSymbol.KEYWORD_LET) concat
                                        atomic(CacophonyGrammarSymbol.VARIABLE_IDENTIFIER) concat
                                        (
                                            atomic(CacophonyGrammarSymbol.DECLARATION_TYPED)
                                                or
                                                atomic(CacophonyGrammarSymbol.DECLARATION_UNTYPED)
                                        )
                                )
                        ),
                    CacophonyGrammarSymbol.DECLARATION_TYPED produces
                        (
                            atomic(CacophonyGrammarSymbol.COLON)
                                concat
                                atomic(CacophonyGrammarSymbol.TYPE)
                                concat atomic(CacophonyGrammarSymbol.ASSIGNMENT)
                                concat (
                                    atomic(
                                        CacophonyGrammarSymbol.VARIABLE_DECLARATION,
                                    ) or atomic(CacophonyGrammarSymbol.FUNCTION_DECLARATION)
                                )
                        ),
                    CacophonyGrammarSymbol.DECLARATION_UNTYPED produces
                        (
                            atomic(CacophonyGrammarSymbol.ASSIGNMENT) concat (
                                atomic(
                                    CacophonyGrammarSymbol.VARIABLE_DECLARATION,
                                ) or atomic(CacophonyGrammarSymbol.FUNCTION_DECLARATION)
                            )

                        ),
                    CacophonyGrammarSymbol.VARIABLE_DECLARATION produces
                        (
                            atomic(CacophonyGrammarSymbol.ASSIGNMENT_LEVEL)
                        ),
                    CacophonyGrammarSymbol.FUNCTION_DECLARATION produces
                        (
                            (
                                atomic(CacophonyGrammarSymbol.LEFT_BRACKET)
                                    concat
                                    atomic(CacophonyGrammarSymbol.RIGHT_BRACKET)
                                    concat
                                    atomic(CacophonyGrammarSymbol.ARROW)
                                    concat
                                    atomic(CacophonyGrammarSymbol.TYPE)
                                    concat
                                    atomic(CacophonyGrammarSymbol.DOUBLE_ARROW)
                                    concat
                                    atomic(CacophonyGrammarSymbol.RETURN_LEVEL)
                            ) or (
                                atomic(CacophonyGrammarSymbol.ASSIGNMENT)
                                    concat
                                    atomic(CacophonyGrammarSymbol.LEFT_BRACKET)
                                    concat
                                    (
                                        atomic(CacophonyGrammarSymbol.FUNCTION_ARGUMENT) concat
                                            (
                                                atomic(CacophonyGrammarSymbol.COMMA) concat
                                                    atomic(CacophonyGrammarSymbol.FUNCTION_ARGUMENT)
                                            ).star()
                                    )
                                    concat
                                    atomic(CacophonyGrammarSymbol.RIGHT_BRACKET)
                                    concat
                                    atomic(CacophonyGrammarSymbol.ARROW)
                                    concat
                                    atomic(CacophonyGrammarSymbol.TYPE)
                                    concat
                                    atomic(CacophonyGrammarSymbol.DOUBLE_ARROW)
                                    concat
                                    atomic(CacophonyGrammarSymbol.RETURN_LEVEL)
                            )
                        ),
                    CacophonyGrammarSymbol.TYPE produces
                        (
                            atomic(CacophonyGrammarSymbol.TYPE_IDENTIFIER) or
                                (
                                    atomic(CacophonyGrammarSymbol.LEFT_BRACKET) concat
                                        (
                                            atomic(CacophonyGrammarSymbol.TYPE) concat
                                                (
                                                    atomic(CacophonyGrammarSymbol.COMMA) concat
                                                        atomic(CacophonyGrammarSymbol.TYPE)
                                                ).star()
                                        ) concat
                                        atomic(CacophonyGrammarSymbol.RIGHT_BRACKET) concat
                                        atomic(CacophonyGrammarSymbol.ARROW) concat
                                        atomic(CacophonyGrammarSymbol.TYPE)
                                ) or
                                (
                                    atomic(CacophonyGrammarSymbol.LEFT_BRACKET) concat
                                        atomic(CacophonyGrammarSymbol.RIGHT_BRACKET) concat
                                        atomic(CacophonyGrammarSymbol.ARROW) concat
                                        atomic(CacophonyGrammarSymbol.TYPE)
                                )
                        ),
                    CacophonyGrammarSymbol.ASSIGNMENT_LEVEL produces
                        (
                            atomic(CacophonyGrammarSymbol.UNARY_LEVEL) or
                                (
                                    atomic(CacophonyGrammarSymbol.UNARY_LEVEL) concat
                                        atomic(CacophonyGrammarSymbol.ASSIGNMENT) concat
                                        atomic(CacophonyGrammarSymbol.ASSIGNMENT_LEVEL)
                                )
                        ),
                    CacophonyGrammarSymbol.ASSIGNMENT produces
                        (
                            atomic(CacophonyGrammarSymbol.OPERATOR_ASSIGNMENT) or
                                atomic(CacophonyGrammarSymbol.OPERATOR_ADDITION_ASSIGNMENT) or
                                atomic(CacophonyGrammarSymbol.OPERATOR_DIVISION_ASSIGNMENT) or
                                atomic(CacophonyGrammarSymbol.OPERATOR_MODULO_ASSIGNMENT) or
                                atomic(CacophonyGrammarSymbol.OPERATOR_MULTIPLICATION_ASSIGNMENT) or
                                atomic(CacophonyGrammarSymbol.OPERATOR_SUBTRACTION_ASSIGNMENT)
                        ),
                    CacophonyGrammarSymbol.UNARY_LEVEL produces
                        (
                            atomic(CacophonyGrammarSymbol.ATOM_LEVEL) or
                                (
                                    atomic(CacophonyGrammarSymbol.UNARY) concat
                                        atomic(CacophonyGrammarSymbol.ATOM_LEVEL)
                                )

                        ),
                    CacophonyGrammarSymbol.UNARY produces
                        (
                            atomic(CacophonyGrammarSymbol.OPERATOR_SUBTRACTION) or
                                (
                                    atomic(CacophonyGrammarSymbol.OPERATOR_LOGICAL_NOT)
                                )

                        ),
                    CacophonyGrammarSymbol.ATOM_LEVEL produces
                        (
                            atomic(CacophonyGrammarSymbol.KEYWORD_BREAK) or
                                atomic(CacophonyGrammarSymbol.VARIABLE_IDENTIFIER) or
                                atomic(CacophonyGrammarSymbol.BOOL_LITERAL) or
                                atomic(CacophonyGrammarSymbol.INT_LITERAL)
                        ),
                ),
            )
    }
}
