package cacophony.lexer

import cacophony.automata.DFA
import cacophony.token.Token
import cacophony.util.AlgebraicRegex
import cacophony.util.Diagnostics
import cacophony.util.Input

class Lexer<TC : Enum<TC>> {
    companion object {
        // Factory accepting list of DFAs.
        // Param is list of pairs (DFA, token_category). Pairs are provided in descending order of category priority.
        fun <TC : Enum<TC>> fromAutomata(automata: List<Pair<DFA<out Object>, TC>>): Lexer<TC> {
            TODO("Not yet implemented")
        }

        // Special factory accepting regexes instead of already created NFAs.
        // Param is list of pairs (regex, token_category). Pairs are provided in descending order of category priority.
        fun <TC : Enum<TC>> fromRegexes(regexes: List<Pair<AlgebraicRegex, TC>>): Lexer<TC> {
            TODO("Not yet implemented")
        }
    }

    fun process(
        input: Input,
        diagnostics: Diagnostics,
    ): List<Token<TC>> {
        TODO("Not yet implemented")
    }
}
