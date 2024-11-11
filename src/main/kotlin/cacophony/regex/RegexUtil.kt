package cacophony.regex

import cacophony.utils.AlgebraicRegex
import cacophony.utils.AlgebraicRegex.AtomicRegex
import cacophony.utils.AlgebraicRegex.ConcatenationRegex
import cacophony.utils.AlgebraicRegex.StarRegex
import cacophony.utils.AlgebraicRegex.UnionRegex

// This is not CompileException intentionally.
class RegexSyntaxError(
    reason: String,
) : Exception(reason)

internal sealed class RegexT : Cloneable {
    public abstract override fun clone(): RegexT

    abstract fun toAlgebraicRegex(): AlgebraicRegex<Char>
}

internal class Atom(
    val symbol: Char,
) : RegexT() {
    override fun toAlgebraicRegex() = AtomicRegex(symbol)

    override fun clone(): Atom = Atom(symbol)
}

internal class Union(
    val summands: MutableList<RegexT>,
) : RegexT() {
    override fun toAlgebraicRegex() = UnionRegex(*summands.map { it.toAlgebraicRegex() }.toTypedArray())

    override fun clone(): Union = Union(summands.map { it.clone() }.toMutableList())
}

internal class Concat(
    val factors: MutableList<RegexT>,
) : RegexT() {
    override fun toAlgebraicRegex() = ConcatenationRegex(*factors.map { it.toAlgebraicRegex() }.toTypedArray())

    override fun clone(): Concat = Concat(factors.map { it.clone() }.toMutableList())
}

internal class Star(
    private val internal: RegexT,
) : RegexT() {
    override fun toAlgebraicRegex() = StarRegex(internal.toAlgebraicRegex())

    override fun clone(): Star = Star(internal.clone())
}

private fun atoms(range: CharRange): Array<Atom> = range.map { Atom(it) }.toTypedArray()

private val SPECIAL_CHARACTER_MAP =
    mapOf(
        // Newline
        'n' to Atom('\n'),
        // Horizontal tab
        't' to Atom('\t'),
        // Carriage return
        'r' to Atom('\r'),
        // Every `normal` character except newline (comments match #\N*)
        'N' to
            Union(
                arrayListOf(
                    Atom('\t'),
                    Atom('\r'),
                    Atom(' '),
                    *(32..126).map { Atom(it.toChar()) }.toTypedArray(),
                ),
            ),
        // Whitespaces
        's' to
            Union(
                arrayListOf(
                    Atom(' '),
                    Atom('\t'),
                    Atom('\r'),
                    Atom('\n'),
                ),
            ),
        // lowercase ASCII letters
        'l' to Union(arrayListOf(*atoms('a'..'z'))),
        // uppercase ASCII letters
        'u' to Union(arrayListOf(*atoms('A'..'Z'))),
        // digits
        'd' to Union(arrayListOf(*atoms('0'..'9'))),
        // alphanumeric and underscore
        'w' to
            Union(
                arrayListOf(
                    *atoms('a'..'z'),
                    *atoms('A'..'Z'),
                    *atoms('0'..'9'),
                    Atom('_'),
                ),
            ),
        // regex special characters
        *"()|*\\".map { Pair(it, Atom(it)) }.toTypedArray(),
    )

internal fun getSpecialCharacterRegex(c: Char): RegexT? = SPECIAL_CHARACTER_MAP[c]?.clone()
