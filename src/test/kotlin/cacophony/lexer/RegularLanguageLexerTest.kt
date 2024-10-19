package cacophony.lexer

import cacophony.DFA_FIRST_UPPER_CASE
import cacophony.DFA_NON_EMPTY
import cacophony.DFA_SQUARE
import cacophony.DFA_UPPER_CASE
import cacophony.MockCategory
import cacophony.createStringInput
import cacophony.token.Token
import cacophony.utils.Diagnostics
import cacophony.utils.Location
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
        val validInput = createStringInput("aabb[Abab]abAba[]baaBabb")

        // when
        val tokens = lexer.process(validInput, diagnostics)

        // then
        // TODO: What is context??
        assertEquals(
            listOf(
                Token(MockCategory.NON_EMPTY, "what is context?", Location(0), Location(3)),
                Token(MockCategory.SQUARE, "what is context?", Location(4), Location(9)),
                Token(MockCategory.NON_EMPTY, "what is context?", Location(10), Location(14)),
                Token(MockCategory.SQUARE, "what is context?", Location(15), Location(16)),
                Token(MockCategory.NON_EMPTY, "what is context?", Location(17), Location(23)),
            ),
            tokens,
        )
    }

    @Test
    fun `should not report any diagnostics for valid input`() {
        // given
        val validInput = createStringInput("AABB[Abab]abAba[]AaaBabb")

        // when
        lexer.process(validInput, diagnostics)

        // then
        verify(diagnostics, never()).report(any(), any(), any())
    }

    @Test
    fun `should respect token priorities`() {
        // given
        val input = createStringInput("AB[]Ab[]ab")

        // when
        val tokens = lexer.process(input, diagnostics)

        // then
        // TODO: What is context??
        assertEquals(
            listOf(
                Token(MockCategory.UPPER_CASE, "what is context?", Location(0), Location(1)),
                Token(MockCategory.SQUARE, "what is context?", Location(2), Location(3)),
                Token(MockCategory.FIRST_UPPER_CASE, "what is context?", Location(4), Location(5)),
                Token(MockCategory.SQUARE, "what is context?", Location(6), Location(7)),
                Token(MockCategory.NON_EMPTY, "what is context?", Location(8), Location(9)),
            ),
            tokens,
        )
    }

    @Test
    fun `should skip positions without match and report diagnostics`() {
        // given
        val invalidInput = createStringInput("AB@ab")

        // when
        val tokens = lexer.process(invalidInput, diagnostics)

        // then
        // TODO: What the hell is context?
        assertEquals(
            listOf(
                Token(MockCategory.UPPER_CASE, "what is context?", Location(0), Location(1)),
                Token(MockCategory.NON_EMPTY, "what is context?", Location(3), Location(4)),
            ),
            tokens,
        )
        // TODO: Message not any()
        verify(diagnostics, times(1)).report(any(), eq(invalidInput), eq(Location(2)))
    }

    @Test
    fun `should ignore comments`() {
        // given
        val inputWithComment = createStringInput("AABB#this_is_a_comment\nAABB")

        // when
        val tokens = lexer.process(inputWithComment, diagnostics)

        // then
        // TODO: What the hell is context?
        assertEquals(
            listOf(
                Token(MockCategory.UPPER_CASE, "what is context?", Location(0), Location(3)),
                Token(MockCategory.UPPER_CASE, "what is context?", Location(23), Location(26)),
            ),
            tokens,
        )
        verify(diagnostics, never()).report(any(), any(), any())
    }
}
