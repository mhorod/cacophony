package cacophony.grammars

import cacophony.automata.DFA

fun <StateType, SymbolType, ResultType> findNullable(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
): Set<DFAStateReference<StateType, SymbolType, ResultType>> {
    TODO("Not implemented")
}
