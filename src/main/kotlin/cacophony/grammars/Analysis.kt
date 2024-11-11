package cacophony.grammars

import cacophony.automata.DFA
import cacophony.automata.buildDFAFromRegex
import cacophony.automata.joinAutomata

typealias DFAStateReference<StateT, SymbolT, ResultT> =
    Pair<StateT, DFA<StateT, SymbolT, ResultT>>

typealias StateToSymbolsMap<StateT, SymbolT, ResultT> =
    Map<DFAStateReference<StateT, SymbolT, ResultT>, Set<SymbolT>>

data class AnalyzedGrammar<StateT, SymbolT>(
    val startSymbol: SymbolT,
    val syncSymbols: Collection<SymbolT>,
    val automata: Map<SymbolT, DFA<StateT, SymbolT, Production<SymbolT>>>,
    val nullable: Collection<DFAStateReference<StateT, SymbolT, Production<SymbolT>>>,
    val first: StateToSymbolsMap<StateT, SymbolT, Production<SymbolT>>,
    val follow: StateToSymbolsMap<StateT, SymbolT, Production<SymbolT>>,
) {
    companion object {
        fun <SymbolT> fromGrammar(
            syncSymbols: Collection<SymbolT>,
            grammar: Grammar<SymbolT>,
        ) = fromAutomata(grammar.start, syncSymbols, buildAutomata(grammar))

        fun <SymbolT> buildAutomata(grammar: Grammar<SymbolT>) =
            grammar.productions.groupBy { it.lhs }.mapValues { buildAutomaton(it.value) }

        private fun <SymbolT> buildAutomaton(productions: List<Production<SymbolT>>) =
            joinAutomata(productions.map { buildDFAFromRegex(it.rhs) }.zip(productions))

        private fun <StateT, SymbolT> fromAutomata(
            startSymbol: SymbolT,
            syncSymbols: Collection<SymbolT>,
            automata: Map<SymbolT, DFA<StateT, SymbolT, Production<SymbolT>>>,
        ): AnalyzedGrammar<StateT, SymbolT> {
            val nullable = findNullable(automata)
            val first = findFirst(automata, nullable)
            val follow = findExtendedFollowForStateReferences(automata, nullable, first)
            return AnalyzedGrammar(startSymbol, syncSymbols, automata, nullable, first, follow)
        }
    }
}
