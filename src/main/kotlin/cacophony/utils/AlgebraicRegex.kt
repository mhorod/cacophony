package cacophony.utils

import cacophony.regex.parseRegex

sealed class AlgebraicRegex<AtomT> {
    class AtomicRegex<AtomT>(
        val symbol: AtomT,
    ) : AlgebraicRegex<AtomT>()

    class UnionRegex<AtomT>(
        vararg val internalRegexes: AlgebraicRegex<AtomT>,
    ) : AlgebraicRegex<AtomT>()

    class ConcatenationRegex<AtomT>(
        vararg val internalRegexes: AlgebraicRegex<AtomT>,
    ) : AlgebraicRegex<AtomT>()

    class StarRegex<AtomT>(
        val internalRegex: AlgebraicRegex<AtomT>,
    ) : AlgebraicRegex<AtomT>()

    companion object {
        fun fromString(regex: String): AlgebraicRegex<Char> = parseRegex(regex)

        fun <AtomT> atomic(atom: AtomT) = AtomicRegex(atom)
    }

    infix fun or(rhs: AlgebraicRegex<AtomT>) = UnionRegex(this, rhs)

    infix fun concat(rhs: AlgebraicRegex<AtomT>) = ConcatenationRegex(this, rhs)

    fun star() = StarRegex(this)
}
