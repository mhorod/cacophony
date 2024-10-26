package cacophony.parser

import cacophony.automata.SimpleDFA
import cacophony.automata.minimalization.via
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.Production
import cacophony.utils.AlgebraicRegex
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LLOneParserTest {
    private fun <T> results(vararg results: T) = results.associateWith { }

    private fun <StateType, AtomType> productions() = mapOf<Pair<StateType, AtomType>, StateType>()

    enum class Symbol {
        A,
        B,
        C,
    }

    @Test
    fun `constructor throws given ambiguous grammar`() {
//        A -> B | C
//        B -> C
        val dfaA =
            SimpleDFA(
                0,
                mapOf(0 via Symbol.B to 1, 0 via Symbol.C to 2),
                mapOf(
                    1 to Production(Symbol.A, AlgebraicRegex.atomic(Symbol.B)),
                    2 to Production(Symbol.A, AlgebraicRegex.atomic(Symbol.C)),
                ),
            )
        val dfaB = SimpleDFA(0, mapOf(0 via Symbol.C to 1), mapOf(1 to Production(Symbol.B, AlgebraicRegex.atomic(Symbol.C))))
        val dfaC = SimpleDFA(0, productions<Int, Symbol>(), mapOf(0 to Production(Symbol.C, AlgebraicRegex.atomic(Symbol.C))))
        // ^ can we do an empty AlgebraicRegex?
        val automata = mapOf(Symbol.A to dfaA, Symbol.B to dfaB, Symbol.C to dfaC)
        val nullable =
            setOf(
                Pair(1, dfaA),
                Pair(2, dfaA),
                Pair(0, dfaC),
            )
        val first =
            mapOf(
                Pair(0, dfaA) to setOf(Symbol.B, Symbol.C),
                Pair(1, dfaA) to emptySet(),
                Pair(2, dfaA) to emptySet(),
                Pair(0, dfaB) to setOf(Symbol.C),
                Pair(1, dfaB) to emptySet(),
                Pair(0, dfaC) to emptySet(),
            )
        val follow =
            mapOf<Pair<Int, SimpleDFA<Int, Symbol, Production<Symbol>>>, Set<Symbol>>(
                Pair(0, dfaA) to emptySet(),
                Pair(1, dfaA) to emptySet(),
                Pair(2, dfaA) to emptySet(),
                Pair(0, dfaB) to emptySet(),
                Pair(1, dfaB) to emptySet(),
                Pair(0, dfaC) to emptySet(),
            )

        // TODO: pls help with type mismatch :<
        val analyzedGrammar = AnalyzedGrammar(listOf(), automata, nullable, first, follow)

        assertThrows(ParserConstructorErrorException::class.java) {
            LLOneParser.fromAnalyzedGrammar(analyzedGrammar)
        }
    }
}
