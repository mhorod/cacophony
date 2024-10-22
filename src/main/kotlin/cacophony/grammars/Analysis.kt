package cacophony.grammars

import cacophony.automata.DFA

// TODO: Uncomment this after #46
// typealias DFAStateReference<StateType, Atom, Result>
//     = Pair<StateType, DFA<StateType, Atom, Result>>
typealias DFAStateReference<StateType> = Pair<StateType, DFA<StateType>>

fun <StateType, SymbolType> findNullable(automata: Map<SymbolType, DFA<StateType>>): Collection<DFAStateReference<StateType>> {
    TODO("Not implemented")
}

fun <StateType, SymbolType> findFirst(
    automata: Map<SymbolType, DFA<StateType>>,
    nullable: Collection<DFAStateReference<StateType>>,
): Map<DFAStateReference<StateType>, Collection<SymbolType>> {
    TODO("Not implemented")
}

fun <StateType, SymbolType> findFollow(
    automata: Map<SymbolType, DFA<StateType>>,
    nullable: Collection<DFAStateReference<StateType>>,
    first: Map<DFAStateReference<StateType>, Collection<SymbolType>>,
): Map<DFAStateReference<StateType>, Collection<SymbolType>> {
    TODO("Not implemented")
}

data class AnalyzedGrammar<StateType, SymbolType>(
    val syncSymbolTypes: Collection<SymbolType>,
    val automata: Map<SymbolType, DFA<StateType>>,
    // TODO: Uncomment after #46
    // val automata: Map<SymbolType, DFA<StateType, Atom, SymbolType>>
) {
    val nullable: Collection<DFAStateReference<StateType>> = findNullable(automata)
    val first: Map<DFAStateReference<StateType>, Collection<SymbolType>> = findFirst(automata, nullable)
    val follow: Map<DFAStateReference<StateType>, Collection<SymbolType>> = findFollow(automata, nullable, first)
}
