package cacophony.grammars

import cacophony.token.Token
import cacophony.utils.Location
import cacophony.utils.Tree
import cacophony.utils.TreeLeaf

sealed class ParseTree<SymbolT : Enum<SymbolT>>(
    val range: Pair<Location, Location>,
) : Tree {
    class Leaf<SymbolT : Enum<SymbolT>>(
        val token: Token<SymbolT>,
    ) : ParseTree<SymbolT>(Pair(token.rangeFrom, token.rangeTo)),
        TreeLeaf {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Leaf<*>) return false
            return token == other.token
        }

        override fun hashCode() = token.hashCode()

        override fun toString() = "${token.category} (${range.first.value}-${range.second.value})"
    }

    class Branch<SymbolT : Enum<SymbolT>>(
        range: Pair<Location, Location>,
        val production: Production<SymbolT>,
        val children: List<ParseTree<SymbolT>>,
    ) : ParseTree<SymbolT>(range) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Branch<*>) return false
            if (other.children.size != children.size) return false
            return children.zip(other.children).all { (l, r) -> l == r }
        }

        override fun hashCode() = children.fold(0) { hash, child -> 19 * hash + child.hashCode() }

        override fun toString() = "${production.lhs} (${range.first.value}-${range.second.value})"

        override fun children() = children
    }
}
