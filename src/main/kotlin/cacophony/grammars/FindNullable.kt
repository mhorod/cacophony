package cacophony.grammars

import cacophony.automata.DFA
import cacophony.automata.minimalization.reverse

fun <StateType, SymbolType, ResultType> findNullable(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
): Set<DFAStateReference<StateType, SymbolType, ResultType>> {
    val reversed = automata.mapValues { (_, dfa) -> reverse(dfa) }
    val toProcess =
        automata
            .flatMap { (symbol, automaton) ->
                automaton.getAllStates().map { it to symbol }
            }.filter { (state, symbol) -> automata[symbol]!!.isAccepting(state) }
            .toMutableList()
    val nullableSymbols = mutableSetOf<SymbolType>()
    val conditionalNullable = mutableMapOf<SymbolType, MutableSet<Pair<StateType, SymbolType>>>().withDefault { mutableSetOf() }
    val nullable = mutableSetOf<DFAStateReference<StateType, SymbolType, ResultType>>()
    while (toProcess.isNotEmpty()) {
        val (state, symbol) = toProcess.removeLast()
        val automaton = automata[symbol]!!
        if (state == automaton.getStartingState()) {
            nullableSymbols.add(symbol)
            nullable.add(state to automaton)
            nullable.addAll(conditionalNullable[symbol]!!.map { (state, symbol) -> state to automata[symbol]!! })
        } else {
            for ((transitionSymbol, states) in reversed[symbol]!![state]!!) {
                if (transitionSymbol in nullableSymbols) {
                    toProcess.addAll(states.map { it to symbol })
                } else {
                    conditionalNullable[transitionSymbol]!!.addAll(states.map { it to symbol })
                }
            }
        }
    }
    return nullable
}
