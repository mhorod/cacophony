package cacophony.grammars

import cacophony.token.Token
import cacophony.utils.Location

sealed class ParseTree<Symbol : Enum<Symbol>> {
    class Leaf<Symbol : Enum<Symbol>>(
        val token: Token<Symbol>,
    ) : ParseTree<Symbol>()

    class Branch<Symbol : Enum<Symbol>>(
        val range: Pair<Location, Location>,
        val production: Production<Symbol>,
        val children: List<ParseTree<Symbol>>,
    ) : ParseTree<Symbol>()
}
