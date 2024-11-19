package cacophony.lexer

import cacophony.diagnostics.Diagnostics
import cacophony.token.Token
import cacophony.utils.Input

interface Lexer<TC : Enum<TC>> {
    fun process(input: Input, diagnostics: Diagnostics): List<Token<TC>>
}
