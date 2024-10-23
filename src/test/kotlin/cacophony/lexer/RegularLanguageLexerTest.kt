package cacophony.lexer

import cacophony.DFA_FIRST_UPPER_CASE
import cacophony.DFA_NON_EMPTY
import cacophony.DFA_SQUARE
import cacophony.DFA_UPPER_CASE
import cacophony.MockCategory
import cacophony.automata.SimpleDFA
import cacophony.automata.SimpleNFA
import cacophony.automata.buildNFAFromRegex
import cacophony.automata.determinize
import cacophony.token.Token
import cacophony.utils.AlgebraicRegex
import cacophony.utils.Diagnostics
import cacophony.utils.Location
import cacophony.utils.StringInput
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

    @MockK
    lateinit var diagnostics: Diagnostics

    @BeforeEach
    fun setUpMocks() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { diagnostics.report(any(), any(), any()) } just runs
    }

    @Test
    fun `should construct automata if constructed from regexes`() {
        // given
        val regex = AlgebraicRegex.AtomicRegex('a')
        val mockNFA = SimpleNFA<Char>(0, mapOf(), mapOf(), 0)
        val mockDFA = SimpleDFA<Int, Char, Boolean>(0, mapOf(), mapOf(0 to true))

        mockkStatic("cacophony.automata.NFAConstructorKt")
        every { buildNFAFromRegex(regex) } returns mockNFA
        mockkStatic("cacophony.automata.DeterminizationKt")
        every { determinize<Int, Char>(any()) } returns mockDFA

        // when
        RegularLanguageLexer.fromRegexes(listOf(Pair(MockCategory.NON_EMPTY, regex)))

        // then
        verify { buildNFAFromRegex(regex) }
        verify { determinize(mockNFA) }
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
        verify(exactly = 0) { diagnostics.report(any(), any(), any()) }
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
        verify(exactly = 1) {
            diagnostics.report(
                eq("Lexer failure: no valid token found."),
                eq(invalidInput),
                eq(Location(2)),
            )
        }
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
        verify(exactly = 0) { diagnostics.report(any(), any(), any()) }
    }
}
