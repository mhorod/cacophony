package cacophony.grammars

import cacophony.automata.DFA

fun <StateType, SymbolType, ResultType> findFollow(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType, ResultType>>,
    first: StateToSymbolsMap<StateType, SymbolType, ResultType>,
): StateToSymbolsMap<StateType, SymbolType, ResultType> {
    TODO("Not implemented")
}
