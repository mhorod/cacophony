package cacophony.grammars

import cacophony.automata.DFA
import cacophony.automata.buildDFAFromRegex
import cacophony.automata.joinAutomata

typealias DFAStateReference<StateType, SymbolType, ResultType> =
    Pair<StateType, DFA<StateType, SymbolType, ResultType>>

typealias StateToSymbolsMap<StateType, SymbolType, ResultType> =
    Map<DFAStateReference<StateType, SymbolType, ResultType>, Set<SymbolType>>

data class AnalyzedGrammar<StateType, SymbolType>(
    val startSymbol: SymbolType,
    val syncSymbols: Collection<SymbolType>,
    val automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
    val nullable: Collection<DFAStateReference<StateType, SymbolType, Production<SymbolType>>>,
    val first: StateToSymbolsMap<StateType, SymbolType, Production<SymbolType>>,
    val follow: StateToSymbolsMap<StateType, SymbolType, Production<SymbolType>>,
) {
    companion object {
        fun <SymbolType> fromGrammar(
            syncSymbols: Collection<SymbolType>,
            grammar: Grammar<SymbolType>,
        ) = fromAutomata(grammar.start, syncSymbols, buildAutomata(grammar))

        fun <SymbolType> buildAutomata(grammar: Grammar<SymbolType>) =
            grammar.productions.groupBy { it.lhs }.mapValues { buildAutomaton(it.value) }

        private fun <SymbolType> buildAutomaton(productions: List<Production<SymbolType>>) =
            joinAutomata(productions.map { buildDFAFromRegex(it.rhs) }.zip(productions))

        private fun <StateType, SymbolType> fromAutomata(
            startSymbol: SymbolType,
            syncSymbols: Collection<SymbolType>,
            automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
        ): AnalyzedGrammar<StateType, SymbolType> {
            val nullable = findNullable(automata)
            val first = findFirst(automata, nullable)
            val follow = findExtendedFollowForStateReferences(automata, nullable, first)
            return AnalyzedGrammar(startSymbol, syncSymbols, automata, nullable, first, follow)
        }
    }
}
