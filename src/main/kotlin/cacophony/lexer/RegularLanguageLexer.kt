package cacophony.lexer

import cacophony.COMMENT_START_CHAR
import cacophony.END_OF_LINE_CHAR
import cacophony.automata.DFA
import cacophony.token.Token
import cacophony.utils.AlgebraicRegex
import cacophony.utils.Diagnostics
import cacophony.utils.Input
import cacophony.utils.Location

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

    // Tries to match new token starting with the first position after current cursor in input.
    // It assumes the end of input has not been approached yet.
    // At the end, sets cursor position to the last element of found match.
    // If no match is found, returns null and sets the cursor to initial position.
    private fun matchNewToken(input: Input): Token<TC>? {
        val initialLocation = input.getLocation()
        var from: Location? = null
        val matches = mutableListOf<Pair<Location, TC>>()
        val states: MutableList<Pair<TC, State?>> =
            automata
                .map { (category, automaton) ->
                    Pair(category, automaton.getStartingState())
                }.toMutableList()

        while (input.peek() != null && states.isNotEmpty()) {
            val next = input.next()!!

            if (from == null) {
                from = input.getLocation()
            }

            states.replaceAll { (category, state) -> Pair(category, automata[category]!!.getProduction(state!!, next)) }
            states.removeAll { (_, state) -> state == null }
            states.forEach { (category, state) ->
                run {
                    if (automata[category]?.isAccepting(state!!) == true) {
                        matches.add(Pair(input.getLocation(), category))
                    }
                }
            }
        }

        if (matches.isEmpty()) {
            input.setLocation(initialLocation)
            return null
        } else {
            val match =
                matches
                    .maxWith(
                        compareBy { it: Pair<Location, TC> -> it.first }
                            .thenByDescending { it: Pair<Location, TC> -> priorities[it.second]!! },
                    ).let { (to, category) -> Token(category, "what is context?", from!!, to) }

            input.setLocation(match.rangeTo)
            return match
        }
    }

    override fun process(
        input: Input,
        diagnostics: Diagnostics,
    ): List<Token<TC>> {
        val tokens = mutableListOf<Token<TC>>()

        while (true) {
            when (input.peek()) {
                COMMENT_START_CHAR -> input.skip(END_OF_LINE_CHAR)
                null -> break
                else -> {
                    matchNewToken(input)?.let { token ->
                        run {
                            tokens.add(token)
                            input.setLocation(token.rangeTo)
                        }
                    } ?: run {
                        input.next()
                        diagnostics.report("No match found", input, input.getLocation()) // TODO: Better error message
                    }
                }
            }
        }

        return tokens
    }
}
