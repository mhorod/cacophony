package cacophony.regex

import cacophony.utils.AlgebraicRegex
import cacophony.utils.AlgebraicRegex.AtomicRegex
import cacophony.utils.AlgebraicRegex.ConcatenationRegex
import cacophony.utils.AlgebraicRegex.StarRegex
import cacophony.utils.AlgebraicRegex.UnionRegex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegexParserTest {
    // Checks _exact_ structure, i.e. returns false for (a|b) and (b|a).
    private fun <AtomT> algebraicRegexEquals(x: AlgebraicRegex<AtomT>, y: AlgebraicRegex<AtomT>): Boolean {
        when (x) {
            is AtomicRegex ->
                if (y is AtomicRegex) {
                    return x.symbol == y.symbol
                }

            is ConcatenationRegex ->
                if (y is ConcatenationRegex) {
                    return x.internalRegexes.size == y.internalRegexes.size &&
                        x.internalRegexes.zip(y.internalRegexes).all { (a, b) ->
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
                        x.internalRegexes.zip(y.internalRegexes).all { (a, b) ->
                            algebraicRegexEquals(a, b)
                        }
                }
        }
        return false
    }

    private fun <AtomT> algebraicRegexToString(ar: AlgebraicRegex<AtomT>): String =
        when (ar) {
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

    private fun <AtomT> assertEqualAlgebraicRegex(result: AlgebraicRegex<AtomT>, expected: AlgebraicRegex<AtomT>) {
        assert(algebraicRegexEquals(result, expected)) {
            println("result: ${algebraicRegexToString(result)}\nexpect: ${algebraicRegexToString(expected)}")
        }
    }

    @Test
    fun `empty regex throws`() {
        assertThrows<RegexSyntaxError> { parseRegex("""""") }
    }

    @Test
    fun `simple concatenation`() {
        val result = parseRegex("""abc""")
        val expected = ConcatenationRegex(*"""abc""".map { AtomicRegex(it) }.toTypedArray())
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `simple union`() {
        val result = parseRegex("""a|b|c""")
        val expected = UnionRegex(*"""abc""".map { AtomicRegex(it) }.toTypedArray())
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `simple star`() {
        val result = parseRegex("""a***""")
        var expected: AlgebraicRegex<Char> = AtomicRegex('a')
        repeat(3) { expected = StarRegex(expected) }
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `nested parenthesis`() {
        val result = parseRegex("""(((a)))""")
        val expected: AlgebraicRegex<Char> = AtomicRegex('a')
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `mixed example`() {
        val result = parseRegex("""abc|x(a|b)|x*(ab*)""")
        val s0 = ConcatenationRegex(*"""abc""".map { AtomicRegex(it) }.toTypedArray())
        val s1 = ConcatenationRegex(AtomicRegex('x'), UnionRegex(AtomicRegex('a'), AtomicRegex('b')))
        val s2 = ConcatenationRegex(StarRegex(AtomicRegex('x')), AtomicRegex('a'), StarRegex(AtomicRegex('b')))
        val expected: AlgebraicRegex<Char> = UnionRegex(s0, s1, s2)
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `useless parenthesis`() {
        val result = parseRegex("""(((a))((b))(c)|(x)(((((a))))|(((b))))|(((x)))*((a)(((b)))*))""")
        val s0 = ConcatenationRegex(*"""abc""".map { AtomicRegex(it) }.toTypedArray())
        val s1 = ConcatenationRegex(AtomicRegex('x'), UnionRegex(AtomicRegex('a'), AtomicRegex('b')))
        val s2 = ConcatenationRegex(StarRegex(AtomicRegex('x')), AtomicRegex('a'), StarRegex(AtomicRegex('b')))
        val expected: AlgebraicRegex<Char> = UnionRegex(s0, s1, s2)
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `actual keywords`() {
        val keywords = arrayOf("""let""", """while""", """do""", """if""", """then""", """else""", """true""", """false""")
        val result = parseRegex(keywords.joinToString("""|"""))
        val expected: AlgebraicRegex<Char> =
            UnionRegex(
                *keywords
                    .map {
                        ConcatenationRegex(
                            *it
                                .map { c ->
                                    AtomicRegex(c)
                                }.toTypedArray(),
                        )
                    }.toTypedArray(),
            )
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `non special operators`() {
        val operators = """[]&=<>+-!#%"""
        val result = parseRegex(operators.toCharArray().joinToString("""|"""))
        val expected: AlgebraicRegex<Char> = UnionRegex(*operators.map { AtomicRegex(it) }.toTypedArray())
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `special operators`() {
        val operators = """|*\()"""
        val result = parseRegex(operators.toCharArray().joinToString("""|\""", """\"""))
        val expected: AlgebraicRegex<Char> = UnionRegex(*operators.map { AtomicRegex(it) }.toTypedArray())
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `escaped characters`() {
        val characters = """nrt"""
        val result = parseRegex(characters.toCharArray().joinToString("""|\""", """\"""))
        val expected: AlgebraicRegex<Char> = UnionRegex(AtomicRegex('\n'), AtomicRegex('\r'), AtomicRegex('\t'))
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `do not recognize empty word`() {
        assertThrows<RegexSyntaxError> { parseRegex("abc|()|def") }
    }

    @Test
    fun `do not recognize empty word here either`() {
        assertThrows<RegexSyntaxError> { parseRegex("abc|x()z|def") }
    }

    @Test
    fun `mismatched right parenthesis`() {
        assertThrows<RegexSyntaxError> { parseRegex("abc|x(z|d))ef") }
    }

    @Test
    fun `mismatched left parenthesis`() {
        assertThrows<RegexSyntaxError> { parseRegex("abc|x(((z|d))ef") }
    }

    @Test
    fun `wrong escaped character`() {
        val exception = assertThrows<RegexSyntaxError> { parseRegex("""abc|\yx(z|d)ef""") }
        assertEquals("Invalid escaped character 'y' at position 5", exception.message)
    }

    @Test
    fun `too many operators`() {
        assertThrows<RegexSyntaxError> { parseRegex("abc|x|") }
    }

    @Test
    fun `empty space`() {
        assertThrows<RegexSyntaxError> { parseRegex("abc||xyz|a") }
    }

    @Test
    fun `whitespace group special character`() {
        val regex = """\s"""
        val result = parseRegex(regex)
        val expected = getSpecialCharacterRegex('s')!!.toAlgebraicRegex()
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `lowercase group special character`() {
        val result = parseRegex("""\l""")
        val expected = parseRegex(('a'..'z').map { it }.joinToString("|"))
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `uppercase group special character`() {
        val result = parseRegex("""\u""")
        val expected = parseRegex(('A'..'Z').map { it }.joinToString("|"))
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `not-newline special character`() {
        val result = parseRegex("""\N""")
        val expected = getSpecialCharacterRegex('N')!!.toAlgebraicRegex()
        assertEqualAlgebraicRegex(result, expected)
    }

    @Test
    fun `all features`() {
        val regex = """(\na\n)*|(a|(a|b)|(aa*))x\|\**"""
        val result = parseRegex(regex)
        val expected: AlgebraicRegex<Char> =
            UnionRegex(
                StarRegex(ConcatenationRegex(AtomicRegex('\n'), AtomicRegex('a'), AtomicRegex('\n'))),
                ConcatenationRegex(
                    UnionRegex(
                        AtomicRegex('a'),
                        AtomicRegex('a'),
                        AtomicRegex('b'),
                        ConcatenationRegex(AtomicRegex('a'), StarRegex(AtomicRegex('a'))),
                    ),
                    AtomicRegex('x'),
                    AtomicRegex('|'),
                    StarRegex(AtomicRegex('*')),
                ),
            )
        assertEqualAlgebraicRegex(result, expected)
    }
}
