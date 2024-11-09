package cacophony.grammars

import cacophony.automata.DFA
import cacophony.automata.reverse

fun <StateT, SymbolT, ResultT> findNullable(
    automata: Map<SymbolT, DFA<StateT, SymbolT, ResultT>>,
): Set<DFAStateReference<StateT, SymbolT, ResultT>> {
    val reversed = automata.mapValues { (_, dfa) -> reverse(dfa) }
    val toProcess =
        automata
            .flatMap { (symbol, automaton) ->
                automaton.getAllStates().map { it to symbol }
            }.filter { (state, symbol) -> automata[symbol]!!.isAccepting(state) }
            .toMutableList()
    val nullableSymbols = mutableSetOf<SymbolT>()
    val conditionalNullable =
        with(mutableMapOf<SymbolT, MutableSet<Pair<StateT, SymbolT>>>()) {
            withDefault { key -> getOrPut(key) { mutableSetOf() } }
        }
    val nullable = mutableSetOf<Pair<StateT, SymbolT>>()
    while (toProcess.isNotEmpty()) {
        val (state, symbol) = toProcess.removeLast()
        if ((state to symbol) in nullable) continue
        nullable.add(state to symbol)
        val automaton = automata[symbol]!!
        if (state == automaton.getStartingState()) {
            nullableSymbols.add(symbol)
            toProcess.addAll(conditionalNullable.getValue(symbol))
        } else {
            for ((transitionSymbol, states) in reversed[symbol]!![state]!!) {
                if (transitionSymbol in nullableSymbols) {
                    toProcess.addAll(states.map { it to symbol })
                } else {
                    conditionalNullable.getValue(transitionSymbol).addAll(states.map { it to symbol })
                }
            }
        }
    }
    return nullable.map { (state, symbol) -> state to automata[symbol]!! }.toSet()
}
