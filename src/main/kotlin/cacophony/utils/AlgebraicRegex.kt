package cacophony.utils

sealed class AlgebraicRegex {
    class AtomicRegex(
        val symbol: Char,
    ) : AlgebraicRegex()

    class UnionRegex(
        vararg val internalRegexes: AlgebraicRegex,
    ) : AlgebraicRegex()

    class ConcatenationRegex(
        vararg val internalRegexes: AlgebraicRegex,
    ) : AlgebraicRegex()

    class StarRegex(
        val internalRegex: AlgebraicRegex,
    ) : AlgebraicRegex()
}
