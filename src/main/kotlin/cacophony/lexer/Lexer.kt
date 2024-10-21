package cacophony.lexer

import cacophony.token.Token
import cacophony.utils.Diagnostics
import cacophony.utils.Input

interface Lexer<TC : Enum<TC>> {
    fun process(
        input: Input,
        diagnostics: Diagnostics,
    ): List<Token<TC>>
}
