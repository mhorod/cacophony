package cacophony.grammars

import cacophony.automata.DFA

typealias DFAStateReference<StateType, SymbolType, ResultType> =
    Pair<StateType, DFA<StateType, SymbolType, ResultType>>

typealias StateToSymbolsMap<StateType, SymbolType, ResultType> =
    Map<DFAStateReference<StateType, SymbolType, ResultType>, Collection<SymbolType>>

fun <StateType, SymbolType, ResultType> findNullable(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
): Collection<DFAStateReference<StateType, SymbolType, ResultType>> {
    TODO("Not implemented")
}

fun <StateType, SymbolType, ResultType> findFirst(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType, ResultType>>,
): StateToSymbolsMap<StateType, SymbolType, ResultType> {
    TODO("Not implemented")
}

fun <StateType, SymbolType, ResultType> findFollow(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType, ResultType>>,
    first: StateToSymbolsMap<StateType, SymbolType, ResultType>,
): StateToSymbolsMap<StateType, SymbolType, ResultType> {
    TODO("Not implemented")
}

data class AnalyzedGrammar<StateType, SymbolType>(
    val syncSymbolTypes: Collection<SymbolType>,
    val automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
    val nullable: Collection<DFAStateReference<StateType, SymbolType, Production<SymbolType>>>,
    val first: StateToSymbolsMap<StateType, SymbolType, Production<SymbolType>>,
    val follow: StateToSymbolsMap<StateType, SymbolType, Production<SymbolType>>,
) {
    companion object {
        fun <StateType, SymbolType, ResultType> fromAutomata(
            syncSymbolTypes: Collection<SymbolType>,
            automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
        ): AnalyzedGrammar<StateType, SymbolType> {
            val nullable = findNullable(automata)
            val first = findFirst(automata, nullable)
            val follow = findFollow(automata, nullable, first)
            return AnalyzedGrammar(syncSymbolTypes, automata, nullable, first, follow)
        }
    }
}
