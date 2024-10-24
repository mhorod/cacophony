package cacophony.grammars

import cacophony.automata.DFA

typealias DFAStateReference<StateType, AtomType, ResultType> = Pair<StateType, DFA<StateType, AtomType, ResultType>>

fun <StateType, SymbolType> findNullable(
    automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
): Collection<DFAStateReference<StateType, SymbolType, Production<SymbolType>>> {
    TODO("Not implemented")
}

fun <StateType, SymbolType> findFirst(
    automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType, Production<SymbolType>>>,
): Map<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, Collection<SymbolType>> {
    TODO("Not implemented")
}

fun <StateType, SymbolType> findFollow(
    automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType, Production<SymbolType>>>,
    first: Map<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, Collection<SymbolType>>,
): Map<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, Collection<SymbolType>> {
    TODO("Not implemented")
}

data class AnalyzedGrammar<StateType, SymbolType>(
    val syncSymbolTypes: Collection<SymbolType>,
    val automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
) {
    val nullable: Collection<DFAStateReference<StateType, SymbolType, Production<SymbolType>>> = findNullable(automata)
    val first: Map<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, Collection<SymbolType>> =
        findFirst(
            automata,
            nullable,
        )
    val follow: Map<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, Collection<SymbolType>> =
        findFollow(
            automata,
            nullable,
            first,
        )
}
