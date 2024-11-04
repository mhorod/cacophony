package cacophony.grammars

import cacophony.token.Token
import cacophony.utils.Location
import cacophony.utils.Tree
import cacophony.utils.TreeLeaf

sealed class ParseTree<SymbolType : Enum<SymbolType>>(
    val range: Pair<Location, Location>,
) : Tree {
    class Leaf<SymbolType : Enum<SymbolType>>(
        val token: Token<SymbolType>,
    ) : ParseTree<SymbolType>(Pair(token.rangeFrom, token.rangeTo)),
        TreeLeaf {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Leaf<*>) return false
            return token == other.token
        }

        override fun hashCode() = token.hashCode()

        override fun toString() = "${token.category} $range"
    }

    class Branch<SymbolType : Enum<SymbolType>>(
        range: Pair<Location, Location>,
        val production: Production<SymbolType>,
        val children: List<ParseTree<SymbolType>>,
    ) : ParseTree<SymbolType>(range) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Branch<*>) return false
            if (other.children.size != children.size) return false
            return children.zip(other.children).all { (l, r) -> l == r }
        }

        override fun hashCode() = children.fold(0) { hash, child -> 19 * hash + child.hashCode() }

        override fun toString() = "${production.lhs} $range"

        override fun children() = children
    }
}
