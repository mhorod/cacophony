package cacophony.grammars

import cacophony.automata.DFA
import cacophony.utils.getTransitiveClosure

data class GeneralizedSymbol<StateType, SymbolType, ResultType>(
    val state: DFAStateReference<StateType, SymbolType, ResultType>?,
    val symbol: SymbolType?
) {
    init {
        if (state == null && symbol == null)
            throw IllegalArgumentException("State and Symbol cannot both be nulls")
    }
}

fun <StateType, SymbolType, ResultType> findFirst(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
    nullable: Set<DFAStateReference<StateType, SymbolType, ResultType>>,
): StateToSymbolsMap<StateType, SymbolType, ResultType> {
    val stateReferenceToSymbol = automata.map { (symbol, dfa) -> Pair(Pair(dfa.getStartingState(), dfa), symbol) }.toMap()
    val symbolToStateReference = stateReferenceToSymbol.map { (u, v) -> Pair(v, u) }.toMap()

    // this is here for implicit access to function parameters
    fun fromSymbol(symbol: SymbolType): GeneralizedSymbol<StateType, SymbolType, ResultType> {
        return GeneralizedSymbol(symbolToStateReference[symbol], symbol)
    }
    fun fromState(state: DFAStateReference<StateType, SymbolType, ResultType>): GeneralizedSymbol<StateType, SymbolType, ResultType> {
        return GeneralizedSymbol(state, stateReferenceToSymbol[state])
    }

    val graph = automata
        .values
        .flatMap { dfa -> dfa.getAllStates().map { DFAStateReference(it, dfa) } }
        .map { fromState(it) }
        .associateWith { mutableSetOf<GeneralizedSymbol<StateType, SymbolType, ResultType>>() }

    for (dfa in automata.values) {
        for (production in dfa.getProductions()) {
            val from = fromState(Pair(production.key.first, dfa))
            val by = fromSymbol(production.key.second)
            val to = fromState(Pair(production.value, dfa))

            graph[from]!!.add(by)
            if (nullable.contains(by.state)) {
                graph[from]!!.add(to)
            }
        }
    }

    return getTransitiveClosure(graph).mapNotNull { (key, values) ->
        val newKey = key.state ?: return@mapNotNull null
        val newValues = values.mapNotNull { it.symbol }.filter { !automata.containsKey(it) }.toSet()
        return@mapNotNull Pair(newKey, newValues)
    }.toMap()
}
