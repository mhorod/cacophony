package cacophony.regex

import cacophony.utils.AlgebraicRegex
import cacophony.utils.AlgebraicRegex.AtomicRegex
import cacophony.utils.AlgebraicRegex.ConcatenationRegex
import cacophony.utils.AlgebraicRegex.StarRegex
import cacophony.utils.AlgebraicRegex.UnionRegex

class RegexSyntaxErrorException : Exception()

fun algebraicRegexEquals(
    x: AlgebraicRegex,
    y: AlgebraicRegex,
): Boolean {
    when (x) {
        is AtomicRegex ->
            if (y is AtomicRegex) {
                return x.symbol == y.symbol
            }
        is ConcatenationRegex ->
            if (y is ConcatenationRegex) {
                return x.internalRegexes.size == y.internalRegexes.size &&
                    x.internalRegexes.zip(y.internalRegexes).all {
                            (a, b) ->
                        algebraicRegexEquals(a, b)
                    }
            }
        is StarRegex ->
            if (y is StarRegex) {
                return algebraicRegexEquals(x.internalRegex, y.internalRegex)
            }
        is UnionRegex ->
            if (y is UnionRegex) {
                return x.internalRegexes.size == y.internalRegexes.size &&
                    x.internalRegexes.zip(y.internalRegexes).all {
                            (a, b) ->
                        algebraicRegexEquals(a, b)
                    }
            }
    }
    return false
}

fun algebraicRegexToString(ar: AlgebraicRegex): String {
    return when (ar) {
        is AtomicRegex -> ar.symbol.toString()
        is ConcatenationRegex ->
            ar.internalRegexes.joinToString(
                "",
                "(",
                ")",
            ) { algebraicRegexToString(it) }
        is StarRegex -> "(${algebraicRegexToString(ar.internalRegex)})*"
        is UnionRegex ->
            ar.internalRegexes.joinToString(
                "|",
                "(",
                ")",
            ) { algebraicRegexToString(it) }
    }
}

internal sealed class RegexType {
    abstract fun toAlgebraicRegex(): AlgebraicRegex

    class Atom(val symbol: Char) : RegexType() {
        override fun toAlgebraicRegex() = AtomicRegex(symbol)
    }

    class Union(val summands: ArrayList<RegexType>) : RegexType() {
        override fun toAlgebraicRegex() = UnionRegex(*summands.map { it.toAlgebraicRegex() }.toTypedArray())
    }

    class Concat(val factors: ArrayList<RegexType>) : RegexType() {
        override fun toAlgebraicRegex() = ConcatenationRegex(*factors.map { it.toAlgebraicRegex() }.toTypedArray())
    }

    class Star(val internal: RegexType) : RegexType() {
        override fun toAlgebraicRegex() = StarRegex(internal.toAlgebraicRegex())
    }
}
