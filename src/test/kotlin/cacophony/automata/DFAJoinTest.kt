package cacophony.automata

import cacophony.automata.minimalization.buildDFAFromRegex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DFAJoinTest {
    @Test
    fun `joining empty list throws`() {
        assertThrows<IllegalArgumentException> {
            joinAutomata<Unit, Unit, Unit>(emptyList())
        }
    }

    @Test
    fun `joining same automata throws`() {
        val dfa1 = buildDFAFromRegex("abc")
        val dfa2 = buildDFAFromRegex("abc")
        assertThrows<IllegalArgumentException> {
            joinAutomata(listOf(dfa1 to 1, dfa2 to 2))
        }
    }

    @Test
    fun `joining automata with nontrivial intersection throws`() {
        val dfa1 = buildDFAFromRegex("aaa")
        val dfa2 = buildDFAFromRegex("a*")
        assertThrows<IllegalArgumentException> {
            joinAutomata(listOf(dfa1 to 1, dfa2 to 2))
        }
    }

    @Test
    fun `only empty word in intersection throws`() {
        val dfa1 = buildDFAFromRegex("a*")
        val dfa2 = buildDFAFromRegex("b*")
        assertThrows<IllegalArgumentException> {
            joinAutomata(listOf(dfa1 to 1, dfa2 to 2))
        }
    }

    @Test
    fun `joining single automaton produces equivalent one`() {
        val prev = buildDFAFromRegex("abc")
        // Have to do this way, because joinAutomata has different Result type
        // then a dfa from buildDFAFromRegex (Something? vs. Boolean)
        val dfa1 = joinAutomata(listOf(prev to Unit))
        val dfa2 = joinAutomata(listOf(dfa1 to Unit))
        // TODO: uncomment after merging Jan's PR
//        assert(areEquivalent(dfa1, dfa2))
    }

    @Test
    fun `correct results`() {
        val dfa1 = buildDFAFromRegex("aa|aaaa")
        println(dfa1.getProductions())
        val dfa2 = buildDFAFromRegex("a|aaa")
        println(dfa2.getProductions())
        val join = joinAutomata(listOf(dfa1 to 1, dfa2 to 2))
        var state = join.getStartingState()
        println(join.getProductions())
        println(join.getAllStates())
        println(state)
        assertNull(join.result(state))

        state = join.getProduction(state, 'a')!!
        println(state)
        assertEquals(2, join.result(state))

        state = join.getProduction(state, 'a')!!
        println(state)
        assertEquals(1, join.result(state))

        state = join.getProduction(state, 'a')!!
        println(state)
        assertEquals(2, join.result(state))

        state = join.getProduction(state, 'a')!!
        println(state)
        assertEquals(1, join.result(state))

        state = join.getProduction(state, 'a')!!
        println(state)
        assertNull(join.result(state))
    }

    @Test
    fun `many automata long path`() {
        val da = buildDFAFromRegex("\\w*a")
        val db = buildDFAFromRegex("\\w*b")
        val dc = buildDFAFromRegex("\\w*c")
        val dd = buildDFAFromRegex("\\w*d")
        val join = joinAutomata(listOf(da to 'a', db to 'b', dc to 'c', dd to 'd'))
        val path = "abcsbcahsbcassbcabhcsbcsbcabsahcdahcdabcdacabdbdacbacabcd"
        var state = join.getStartingState()
        for (c in path) {
            state = join.getProduction(state, c)!!
            when (c) {
                in "abcd" -> assertEquals(c, join.result(state))
                else -> assertNull(join.result(state))
            }
        }
    }

    @Test
    fun `starting state can be accepting`() {
        val dfa1 = buildDFAFromRegex("a*")
        val dfa2 = buildDFAFromRegex("ba*")
        val join = joinAutomata(listOf(dfa1 to 1, dfa2 to 2))
        assert(join.isAccepting(join.getStartingState()))
    }

    @Test
    fun `long word in the intersection throws`() {
        val dfa1 = buildDFAFromRegex("aasdbbasc*d(x|y)aaac(c|C)*")
        val dfa2 = buildDFAFromRegex("\\w*aaacCc*")
        assertThrows<IllegalArgumentException> {
            joinAutomata(listOf(dfa1 to 1, dfa2 to 2))
        }
    }
}
