package cacophony.grammars

import cacophony.token.Token
import cacophony.utils.Location

sealed class ParseTree<SymbolType : Enum<SymbolType>>(
    val range: Pair<Location, Location>,
) {
    class Leaf<SymbolType : Enum<SymbolType>>(
        val token: Token<SymbolType>,
    ) : ParseTree<SymbolType>(Pair(token.rangeFrom, token.rangeTo))

    class Branch<SymbolType : Enum<SymbolType>>(
        range: Pair<Location, Location>,
        val production: Production<SymbolType>,
        val children: List<ParseTree<SymbolType>>,
    ) : ParseTree<SymbolType>(range)
}
