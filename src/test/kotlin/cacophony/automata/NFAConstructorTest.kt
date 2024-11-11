package cacophony.automata

import cacophony.utils.AlgebraicRegex
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class NFAConstructorTest {
    private class NFAWalker<StateT>(
        val nfa: NFA<StateT, Char>,
    ) {
        private fun epsClosure(states: Set<StateT>): Set<StateT> {
            var states = states
            while (true) {
                var newStates: Set<StateT> = setOf()
                for (state in states) {
                    val epsNeighbors = nfa.getEpsilonProductions()[state] ?: emptyList()
                    newStates = newStates union epsNeighbors
                }
                if ((newStates union states) == states) {
                    break
                } else {
                    states = (newStates union states).toMutableSet()
                }
            }
            return states
        }

        private fun step(
            states: Set<StateT>,
            symbol: Char,
        ): Set<StateT> {
            val states = epsClosure(states)
            var newStates: Set<StateT> = setOf()
            for (state in states) {
                newStates = newStates union nfa.getProductions(state, symbol)
            }
            return epsClosure(newStates)
        }

        fun accepts(str: String): Boolean {
            var states = epsClosure(setOf(nfa.getStartingState()))
            for (symbol in str) {
                states = step(states, symbol)
            }
            return (nfa.getAcceptingState() in states)
        }
    }

    @Test
    fun `simple atomic regex test`() {
        val re = AlgebraicRegex.AtomicRegex('A')
        val walker = NFAWalker(buildNFAFromRegex(re))
        assert(walker.accepts("A"))
        assertFalse(walker.accepts("AA"))
        assertFalse(walker.accepts(""))
        assertFalse(walker.accepts("B"))
    }

    @Test
    fun `simple concat regex test`() {
        val reA = AlgebraicRegex.AtomicRegex('A')
        val reB = AlgebraicRegex.AtomicRegex('B')
        val re = AlgebraicRegex.ConcatenationRegex(reB, reA, reA)
        val walker = NFAWalker(buildNFAFromRegex(re))
        assert(walker.accepts("BAA"))
        assertFalse(walker.accepts(""))
        assertFalse(walker.accepts("A"))
        assertFalse(walker.accepts("BA"))
        assertFalse(walker.accepts("BAAA"))
    }

    @Test
    fun `simple star regex test`() {
        val reA = AlgebraicRegex.AtomicRegex('A')
        val reAA = AlgebraicRegex.ConcatenationRegex(reA, reA)
        val re = AlgebraicRegex.StarRegex(reAA)
        val walker = NFAWalker(buildNFAFromRegex(re))
        assert(walker.accepts(""))
        assert(walker.accepts("AA"))
        assert(walker.accepts("AAAA"))
        assertFalse(walker.accepts("A"))
        assertFalse(walker.accepts("B"))
    }

    @Test
    fun `union regex test`() {
        val reA = AlgebraicRegex.AtomicRegex('A')
        val reB = AlgebraicRegex.AtomicRegex('B')
        val reAB = AlgebraicRegex.ConcatenationRegex(reA, reB)
        val reBAA = AlgebraicRegex.ConcatenationRegex(reB, reA, reA)
        val re = AlgebraicRegex.UnionRegex(reAB, reBAA, reB)
        val walker = NFAWalker(buildNFAFromRegex(re))
        assert(walker.accepts("B"))
        assert(walker.accepts("AB"))
        assert(walker.accepts("BAA"))
        assertFalse(walker.accepts(""))
        assertFalse(walker.accepts("A"))
        assertFalse(walker.accepts("BA"))
        assertFalse(walker.accepts("BAAA"))
    }

    @Test
    fun `complicated regex test`() {
        // #(((AB)*|(BA)*)#)*
        val reA = AlgebraicRegex.AtomicRegex('A')
        val reB = AlgebraicRegex.AtomicRegex('B')
        val reHash = AlgebraicRegex.AtomicRegex('#')
        val reABxStar = AlgebraicRegex.StarRegex(AlgebraicRegex.ConcatenationRegex(reA, reB))
        val reBAxStar = AlgebraicRegex.StarRegex(AlgebraicRegex.ConcatenationRegex(reB, reA))
        val reUnion = AlgebraicRegex.UnionRegex(reABxStar, reBAxStar)
        val reUnionHash = AlgebraicRegex.ConcatenationRegex(reUnion, reHash)
        val re = AlgebraicRegex.ConcatenationRegex(reHash, AlgebraicRegex.StarRegex(reUnionHash))
        val walker = NFAWalker(buildNFAFromRegex(re))
        assert(walker.accepts("##"))
        assert(walker.accepts("#AB#"))
        assert(walker.accepts("#ABAB##"))
        assert(walker.accepts("#ABAB#BA#BABA#"))
        assertFalse(walker.accepts(""))
        assertFalse(walker.accepts("#ABBA#"))
        assertFalse(walker.accepts("#ABAB"))
        assertFalse(walker.accepts("ABAB#"))
    }
}
