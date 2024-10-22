package cacophony.grammars

import cacophony.automata.DFA

// TODO: Uncomment this after #46
// typealias DFAStateReference<State, Atom, Result>
//     = Pair<State, DFA<State, Atom, Result>>
typealias DFAStateReference<State> = Pair<State, DFA<State>>

fun <State, Symbol> findNullable(automata: Map<Symbol, DFA<State>>): Collection<DFAStateReference<State>> {
    TODO("Not implemented")
}

fun <State, Symbol> findFirst(
    automata: Map<Symbol, DFA<State>>,
    nullable: Collection<DFAStateReference<State>>,
): Map<DFAStateReference<State>, Collection<Symbol>> {
    TODO("Not implemented")
}

fun <State, Symbol> findFollow(
    automata: Map<Symbol, DFA<State>>,
    nullable: Collection<DFAStateReference<State>>,
    first: Map<DFAStateReference<State>, Collection<Symbol>>,
): Map<DFAStateReference<State>, Collection<Symbol>> {
    TODO("Not implemented")
}

data class AnalyzedGrammar<State, Symbol>(
    val syncSymbols: Collection<Symbol>,
    val automata: Map<Symbol, DFA<State>>,
    // TODO: Uncomment after #46
    // val automata: Map<Symbol, DFA<State, Atom, Symbol>>
) {
    val nullable: Collection<DFAStateReference<State>> = findNullable(automata)
    val first: Map<DFAStateReference<State>, Collection<Symbol>> = findFirst(automata, nullable)
    val follow: Map<DFAStateReference<State>, Collection<Symbol>> = findFollow(automata, nullable, first)
}
