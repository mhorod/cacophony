package cacophony.utils

import cacophony.regex.parseRegex

sealed class AlgebraicRegex {
    class AtomicRegex(val symbol: Char) : AlgebraicRegex()

    class UnionRegex(vararg val internalRegexes: AlgebraicRegex) : AlgebraicRegex()

    class ConcatenationRegex(vararg val internalRegexes: AlgebraicRegex) : AlgebraicRegex()

    class StarRegex(val internalRegex: AlgebraicRegex) : AlgebraicRegex()

    companion object {
        fun fromString(regex: String): AlgebraicRegex = parseRegex(regex)
    }
}
