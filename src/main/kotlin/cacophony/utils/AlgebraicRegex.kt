package cacophony.utils

import cacophony.regex.parseRegex

sealed class AlgebraicRegex<AtomType> {
    class AtomicRegex<AtomType>(
        val symbol: AtomType,
    ) : AlgebraicRegex<AtomType>()

    class UnionRegex<AtomType>(
        vararg val internalRegexes: AlgebraicRegex<AtomType>,
    ) : AlgebraicRegex<AtomType>()

    class ConcatenationRegex<AtomType>(
        vararg val internalRegexes: AlgebraicRegex<AtomType>,
    ) : AlgebraicRegex<AtomType>()

    class StarRegex<AtomType>(
        val internalRegex: AlgebraicRegex<AtomType>,
    ) : AlgebraicRegex<AtomType>()

    companion object {
        fun fromString(regex: String): AlgebraicRegex<Char> = parseRegex(regex)

        fun <AtomType> atomic(atom: AtomType) = AtomicRegex(atom)
    }

    infix fun or(rhs: AlgebraicRegex<AtomType>) = UnionRegex(this, rhs)

    infix fun concat(rhs: AlgebraicRegex<AtomType>) = ConcatenationRegex(this, rhs)

    fun star() = StarRegex(this)
}
