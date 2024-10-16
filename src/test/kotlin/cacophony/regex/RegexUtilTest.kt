package cacophony.regex

import cacophony.utils.AlgebraicRegex
import cacophony.utils.AlgebraicRegex.AtomicRegex
import cacophony.utils.AlgebraicRegex.ConcatenationRegex
import cacophony.utils.AlgebraicRegex.StarRegex
import cacophony.utils.AlgebraicRegex.UnionRegex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class RegexUtilTest {
    @Test
    fun `pretty printing`() {
        val regex: AlgebraicRegex =
            UnionRegex(
                StarRegex(ConcatenationRegex(AtomicRegex('\n'), AtomicRegex('a'), AtomicRegex('\n'))),
                ConcatenationRegex(
                    UnionRegex(
                        AtomicRegex('a'),
                        UnionRegex(AtomicRegex('a'), AtomicRegex('b')),
                        ConcatenationRegex(AtomicRegex('a'), StarRegex(AtomicRegex('a'))),
                    ),
                    AtomicRegex('x'),
                    AtomicRegex('|'),
                    StarRegex(AtomicRegex('*')),
                ),
            )
        val result = algebraicRegexToString(regex)
        assertEquals(result, "(((\na\n))*|((a|(a|b)|(a(a)*))x|(*)*))")
    }

    @Test
    fun `regex equality`() {
        val regex1: AlgebraicRegex =
            UnionRegex(
                StarRegex(ConcatenationRegex(AtomicRegex('\n'), AtomicRegex('a'), AtomicRegex('\n'))),
                ConcatenationRegex(
                    UnionRegex(
                        AtomicRegex('a'),
                        UnionRegex(AtomicRegex('a'), AtomicRegex('b')),
                        ConcatenationRegex(AtomicRegex('a'), StarRegex(AtomicRegex('a'))),
                    ),
                    AtomicRegex('x'),
                    AtomicRegex('|'),
                    StarRegex(AtomicRegex('*')),
                ),
            )
        val regex2: AlgebraicRegex =
            UnionRegex(
                StarRegex(ConcatenationRegex(AtomicRegex('\n'), AtomicRegex('a'), AtomicRegex('\n'))),
                ConcatenationRegex(
                    UnionRegex(
                        AtomicRegex('a'),
                        UnionRegex(AtomicRegex('a'), AtomicRegex('b')),
                        ConcatenationRegex(AtomicRegex('a'), StarRegex(AtomicRegex('a'))),
                    ),
                    AtomicRegex('x'),
                    AtomicRegex('|'),
                    StarRegex(AtomicRegex('*')),
                ),
            )
        assert(algebraicRegexEquals(regex1, regex2))
    }

    @Test
    fun `not equal same type`() {
        val regex1: AlgebraicRegex =
            UnionRegex(
                StarRegex(ConcatenationRegex(AtomicRegex('\n'), AtomicRegex('a'), AtomicRegex('\n'))),
                ConcatenationRegex(
                    UnionRegex(
                        AtomicRegex('a'),
                        UnionRegex(AtomicRegex('a'), AtomicRegex('b')),
                        ConcatenationRegex(AtomicRegex('a'), StarRegex(AtomicRegex('a'))),
                    ),
                    AtomicRegex('x'),
                    AtomicRegex('|'),
                    StarRegex(AtomicRegex('*')),
                ),
            )
        val regex2: AlgebraicRegex =
            UnionRegex(
                StarRegex(ConcatenationRegex(AtomicRegex('\n'), AtomicRegex('a'), AtomicRegex('\n'))),
                ConcatenationRegex(
                    UnionRegex(
                        UnionRegex(AtomicRegex('a'), AtomicRegex('b')),
                        ConcatenationRegex(AtomicRegex('a'), StarRegex(AtomicRegex('a'))),
                    ),
                    AtomicRegex('x'),
                    AtomicRegex('|'),
                    StarRegex(AtomicRegex('*')),
                ),
            )
        assertFalse(algebraicRegexEquals(regex1, regex2))
    }

    @Test
    fun `not equal different type`() {
        val regex1: AlgebraicRegex =
            UnionRegex(
                StarRegex(ConcatenationRegex(AtomicRegex('\n'), AtomicRegex('a'), AtomicRegex('\n'))),
                ConcatenationRegex(
                    UnionRegex(
                        AtomicRegex('a'),
                        UnionRegex(AtomicRegex('a'), AtomicRegex('b')),
                        ConcatenationRegex(AtomicRegex('a'), StarRegex(AtomicRegex('a'))),
                    ),
                    AtomicRegex('x'),
                    StarRegex(AtomicRegex('|')),
                    StarRegex(AtomicRegex('*')),
                ),
            )
        val regex2: AlgebraicRegex =
            UnionRegex(
                StarRegex(ConcatenationRegex(AtomicRegex('\n'), AtomicRegex('a'), AtomicRegex('\n'))),
                ConcatenationRegex(
                    UnionRegex(
                        AtomicRegex('a'),
                        UnionRegex(AtomicRegex('a'), AtomicRegex('b')),
                        ConcatenationRegex(AtomicRegex('a'), StarRegex(AtomicRegex('a'))),
                    ),
                    AtomicRegex('x'),
                    AtomicRegex('|'),
                    StarRegex(AtomicRegex('*')),
                ),
            )
        assertFalse(algebraicRegexEquals(regex1, regex2))
    }

    @Test
    fun `equivalent can be not equal`() {
        val regex1: AlgebraicRegex =
            UnionRegex(
                StarRegex(ConcatenationRegex(AtomicRegex('\n'), AtomicRegex('a'), AtomicRegex('\n'))),
                ConcatenationRegex(
                    UnionRegex(
                        AtomicRegex('a'),
                        UnionRegex(AtomicRegex('a'), AtomicRegex('b')),
                        ConcatenationRegex(AtomicRegex('a'), StarRegex(AtomicRegex('a'))),
                    ),
                    AtomicRegex('x'),
                    AtomicRegex('|'),
                    StarRegex(AtomicRegex('*')),
                ),
            )
        val regex2: AlgebraicRegex =
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
        assertFalse(algebraicRegexEquals(regex1, regex2))
    }
}
