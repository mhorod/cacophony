package cacophony.lexer

import cacophony.DFA_FIRST_UPPER_CASE
import cacophony.DFA_NON_EMPTY
import cacophony.DFA_SQUARE
import cacophony.DFA_UPPER_CASE
import cacophony.MockCategory
import cacophony.token.Token
import cacophony.utils.Diagnostics
import cacophony.utils.Location
import cacophony.utils.StringInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class RegularLanguageLexerTest {
    private val lexer =
        RegularLanguageLexer.fromAutomata(
            listOf(
                Pair(MockCategory.UPPER_CASE, DFA_UPPER_CASE),
                Pair(MockCategory.FIRST_UPPER_CASE, DFA_FIRST_UPPER_CASE),
                Pair(MockCategory.NON_EMPTY, DFA_NON_EMPTY),
                Pair(MockCategory.SQUARE, DFA_SQUARE),
            ),
        )

    private val diagnostics: Diagnostics = mock()

    @BeforeEach
    fun setUp() {
        Mockito.reset(diagnostics)
    }

    @Test
    fun `should properly construct lexer from regexes`() {
        TODO("Waiting for implementation of Regex->DFA")
    }

    @Test
    fun `should properly process valid input`() {
        // given
        val validInput = StringInput("aabb[Abab]abAba[]baaBabb")

        // when
        val tokens = lexer.process(validInput, diagnostics)

        // then
        assertEquals(
            listOf(
                Token(MockCategory.NON_EMPTY, "aabb", Location(0), Location(3)),
                Token(MockCategory.SQUARE, "[Abab]", Location(4), Location(9)),
                Token(MockCategory.NON_EMPTY, "abAba", Location(10), Location(14)),
                Token(MockCategory.SQUARE, "[]", Location(15), Location(16)),
                Token(MockCategory.NON_EMPTY, "baaBabb", Location(17), Location(23)),
            ),
            tokens,
        )
    }

    @Test
    fun `should not report any diagnostics for valid input`() {
        // given
        val validInput = StringInput("AABB[Abab]abAba[]AaaBabb")

        // when
        lexer.process(validInput, diagnostics)

        // then
        verify(diagnostics, never()).report(any(), any(), any())
    }

    @Test
    fun `should respect token priorities`() {
        // given
        val input = StringInput("AB[]Ab[]ab")

        // when
        val tokens = lexer.process(input, diagnostics)

        // then
        assertEquals(
            listOf(
                Token(MockCategory.UPPER_CASE, "AB", Location(0), Location(1)),
                Token(MockCategory.SQUARE, "[]", Location(2), Location(3)),
                Token(MockCategory.FIRST_UPPER_CASE, "Ab", Location(4), Location(5)),
                Token(MockCategory.SQUARE, "[]", Location(6), Location(7)),
                Token(MockCategory.NON_EMPTY, "ab", Location(8), Location(9)),
            ),
            tokens,
        )
    }

    @Test
    fun `should skip positions without match and report diagnostics`() {
        // given
        val invalidInput = StringInput("AB@ab")

        // when
        val tokens = lexer.process(invalidInput, diagnostics)

        // then
        assertEquals(
            listOf(
                Token(MockCategory.UPPER_CASE, "AB", Location(0), Location(1)),
                Token(MockCategory.NON_EMPTY, "ab", Location(3), Location(4)),
            ),
            tokens,
        )
        verify(diagnostics, times(1))
            .report(eq("Lexer failure: no valid token found."), eq(invalidInput), eq(Location(2)))
    }

    @Test
    fun `should ignore comments`() {
        // given
        val inputWithComment = StringInput("AABB#this_is_a_comment\nAABB")

        // when
        val tokens = lexer.process(inputWithComment, diagnostics)

        // then
        assertEquals(
            listOf(
                Token(MockCategory.UPPER_CASE, "AABB", Location(0), Location(3)),
                Token(MockCategory.UPPER_CASE, "AABB", Location(23), Location(26)),
            ),
            tokens,
        )
        verify(diagnostics, never()).report(any(), any(), any())
    }
}
