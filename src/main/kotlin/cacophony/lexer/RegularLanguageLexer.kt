package cacophony.lexer

import cacophony.COMMENT_START_CHAR
import cacophony.END_OF_LINE_CHAR
import cacophony.automata.DFA
import cacophony.token.Token
import cacophony.utils.AlgebraicRegex
import cacophony.utils.Diagnostics
import cacophony.utils.Input

class RegularLanguageLexer<TC : Enum<TC>, State>(
    private val automata: Map<TC, DFA<State>>,
    private val priorities: Map<TC, Int>,
) : Lexer<TC> {
    companion object {
        // Factory accepting list of DFAs.
        // Param is list of pairs (token_category, DFA). Pairs are provided in descending order of category priority.
        fun <TC : Enum<TC>, State> fromAutomata(automata: List<Pair<TC, DFA<State>>>): RegularLanguageLexer<TC, State> =
            RegularLanguageLexer(
                automata.toMap(),
                automata.mapIndexed { index, (category, _) -> Pair(category, index) }.toMap(),
            )

        // Special factory accepting regexes instead of already created NFAs.
        // Param is list of pairs (token_category, regex). Pairs are provided in descending order of category priority.
        fun <TC : Enum<TC>, State> fromRegexes(regexes: List<Pair<TC, AlgebraicRegex>>): RegularLanguageLexer<TC, State> {
            // TODO: Waiting for AlgebraicRegex -> DFA implementation.
            //   It will look something like this:
            //     return fromAutomata(regexes.map { (category, regex) ->
            //     Pair(category, DFA.fromRegex(regex) }
            TODO("Not yet implemented")
        }
    }

    // Tries to match new token starting with the current cursor in input.
    // It assumes the end of input has not been approached yet.
    // At the end, sets cursor position to the last element of found match.
    // If no match is found, returns null and sets the cursor to initial position.
    private fun matchNewToken(input: Input): Token<TC>? {
        val from = input.getLocation()
        var readString = ""
        var lastMatch: Token<TC>? = null

        // List of current states in simulation sorted by category priority in increasing order.
        // This assures that the last match found has the highest priority.
        val states: MutableList<Pair<TC, State?>> =
            automata
                .map { (category, automaton) ->
                    Pair(category, automaton.getStartingState())
                }.sortedByDescending { priorities[it.first]!! }
                .toMutableList()

        while (input.peek() != null && states.isNotEmpty()) {
            val next = input.peek()!!
            readString += next

            states.replaceAll { (category, state) -> Pair(category, automata[category]!!.getProduction(state!!, next)) }
            states.removeAll { (_, state) -> state == null }
            states.forEach { (category, state) ->
                run {
                    if (automata[category]?.isAccepting(state!!) == true) {
                        lastMatch = Token(category, readString, from, input.getLocation())
                    }
                }
            }

            input.next()
        }

        input.setLocation(lastMatch?.rangeTo ?: from)
        return lastMatch
    }

    override fun process(
        input: Input,
        diagnostics: Diagnostics,
    ): List<Token<TC>> {
        val tokens = mutableListOf<Token<TC>>()

        while (true) {
            when (input.peek()) {
                COMMENT_START_CHAR -> {
                    input.skip(END_OF_LINE_CHAR)
                    input.next()
                }
                null -> break
                else -> {
                    matchNewToken(input)?.let { token ->
                        tokens.add(token)
                    } ?: run {
                        diagnostics.report("Lexer failure: no valid token found.", input, input.getLocation())
                    }
                    input.next()
                }
            }
        }

        return tokens
    }
}
