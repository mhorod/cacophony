package cacophony.grammars

import cacophony.automata.DFA

typealias DFAStateReference<StateType, SymbolType> = Pair<StateType, DFA<StateType, SymbolType, Production<SymbolType>>>

fun <StateType, SymbolType> findNullable(
    automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
): Collection<DFAStateReference<StateType, SymbolType>> {
    TODO("Not implemented")
}

fun <StateType, SymbolType> findFirst(
    automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType>>,
): Map<DFAStateReference<StateType, SymbolType>, Collection<SymbolType>> {
    TODO("Not implemented")
}

fun <StateType, SymbolType> findFollow(
    automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType>>,
    first: Map<DFAStateReference<StateType, SymbolType>, Collection<SymbolType>>,
): Map<DFAStateReference<StateType, SymbolType>, Collection<SymbolType>> {
    TODO("Not implemented")
}

data class AnalyzedGrammar<StateType, SymbolType>(
    val syncSymbolTypes: Collection<SymbolType>,
    val automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
    val nullable: Collection<DFAStateReference<StateType, SymbolType>>,
    val first: Map<DFAStateReference<StateType, SymbolType>, Collection<SymbolType>>,
    val follow: Map<DFAStateReference<StateType, SymbolType>, Collection<SymbolType>>,
) {
    companion object {
        fun <StateType, SymbolType> fromAutomata(
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
