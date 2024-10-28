package cacophony.parser

import cacophony.token.Token
import cacophony.token.TokenCategorySpecific

class Utils {
    companion object {
        fun lexerTokenToParserToken(lexerToken: Token<TokenCategorySpecific>): Token<CacophonyGrammarSymbol> =
            Token(CacophonyGrammarSymbol.valueOf(lexerToken.category.name), lexerToken.context, lexerToken.rangeFrom, lexerToken.rangeTo)
    }
}
