package cacophony.lexer

import cacophony.regex.RegexStrings
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import cacophony.utils.AlgebraicRegex
import cacophony.utils.Diagnostics
import cacophony.utils.Input

class CacophonyLexer : Lexer<TokenCategorySpecific> {
    private val innerLexer: Lexer<TokenCategorySpecific>

    init {
        val regexes =
            RegexStrings.specificCategoryMap.map {
                Pair(it.key, AlgebraicRegex.fromString(it.value))
            }
        innerLexer = RegularLanguageLexer.fromRegexes(regexes)
    }

    override fun process(
        input: Input,
        diagnostics: Diagnostics,
    ): List<Token<TokenCategorySpecific>> {
        val initLocation = input.getLocation()
        println("FILE RIGHT BEFORE INNER LEXER")
        while (input.peek() !== null) {
            print(input.next())
        }
        input.setLocation(initLocation)

        val tokens = innerLexer.process(input, diagnostics)

        println("TOKENS AFTER INNER LEXER")
        println(tokens)

        return tokens.filter { !isWhitespaceToken(it) }
    }

    companion object {
        private fun isWhitespaceToken(token: Token<TokenCategorySpecific>): Boolean =
            token.category == TokenCategorySpecific.WHITESPACE || token.category == TokenCategorySpecific.COMMENT
    }
}
