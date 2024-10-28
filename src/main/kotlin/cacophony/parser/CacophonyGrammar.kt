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
    }
}
