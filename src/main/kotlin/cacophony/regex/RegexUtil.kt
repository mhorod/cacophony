package cacophony.regex

import cacophony.utils.AlgebraicRegex
import cacophony.utils.AlgebraicRegex.AtomicRegex
import cacophony.utils.AlgebraicRegex.ConcatenationRegex
import cacophony.utils.AlgebraicRegex.StarRegex
import cacophony.utils.AlgebraicRegex.UnionRegex

class RegexSyntaxErrorException : Exception()

internal sealed class RegexType {
    abstract fun toAlgebraicRegex(): AlgebraicRegex
}

internal class Atom(val symbol: Char) : RegexType() {
    override fun toAlgebraicRegex() = AtomicRegex(symbol)
}

internal class Union(val summands: ArrayList<RegexType>) : RegexType() {
    override fun toAlgebraicRegex() = UnionRegex(*summands.map { it.toAlgebraicRegex() }.toTypedArray())
}

internal class Concat(val factors: ArrayList<RegexType>) : RegexType() {
    override fun toAlgebraicRegex() = ConcatenationRegex(*factors.map { it.toAlgebraicRegex() }.toTypedArray())
}

internal class Star(val internal: RegexType) : RegexType() {
    override fun toAlgebraicRegex() = StarRegex(internal.toAlgebraicRegex())
}

internal val SPECIAL_CHARACTER_MAP =
    mapOf(
        // Newline
        Pair('n', Atom('\n')),
        // Horizontal tab
        Pair('t', Atom('\t')),
        // Carriage return
        Pair('r', Atom('\r')),
        // Every `normal` character except newline (comments match #\N*)
        Pair('N', Union(arrayListOf(Atom('\t'), Atom('\r'), Atom(' '), *(32..126).map { Atom(it.toChar()) }.toTypedArray()))),
        // Whitespaces
        Pair('s', Union(arrayListOf(Atom(' '), Atom('\t'), Atom('\r'), Atom('\n')))),
        // lowercase ASCII letters
        Pair('l', Union(arrayListOf(*('a'..'z').map { Atom(it) }.toTypedArray()))),
        // uppercase ASCII letters
        Pair('u', Union(arrayListOf(*('A'..'Z').map { Atom(it) }.toTypedArray()))),
        // regex special characters
        *"()|*\\".map { Pair(it, Atom(it)) }.toTypedArray(),
    )
