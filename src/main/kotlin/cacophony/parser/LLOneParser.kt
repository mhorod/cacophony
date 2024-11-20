package cacophony.parser

import cacophony.automata.DFA
import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.ParserDiagnostics
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.DFAStateReference
import cacophony.grammars.ParseTree
import cacophony.grammars.Production
import kotlin.collections.mutableListOf

// This is not CompileException intentionally.
class ParserConstructorError(
    reason: String,
) : Exception(reason)

class LLOneParser<StateT, SymbolT : Enum<SymbolT>>(
    private val nextAction: Map<SymbolT, Map<DFAStateReference<StateT, SymbolT, Production<SymbolT>>, SymbolT?>>,
    private val startSymbol: SymbolT,
    private val automata: Map<SymbolT, DFA<StateT, SymbolT, Production<SymbolT>>>,
    private val syncSymbols: Collection<SymbolT>,
) : Parser<SymbolT> {
    companion object {
        // Constructs LLOneParser with computed nextAction map.
        // nextAction gives symbol on the production that we should use next
        // depending on the current input symbol and dfa state.
        // null denotes no suitable production, then we should try to accept.
        // Throws if grammar is not LL(1).
        inline fun <StateT, reified SymbolT : Enum<SymbolT>> fromAnalyzedGrammar(
            analyzedGrammar: AnalyzedGrammar<StateT, SymbolT>,
        ): LLOneParser<StateT, SymbolT> {
            // Currently the logic is as follows:
            // 1. Loop over all DFAStateReference and inputSymbol: SymbolT.
            // 2. For each production with some symbol P, using nullable, first and follow on the start state of the DFA for P
            //    find all suitable productions (if there is no DFA for P, meaning P is terminal, production is suitable iff P == inputSymbol).
            // 3. If there are at least 2 suitable productions then our grammar is not LL(1).
            // 4. If there is exactly one then ok.
            // 5. If there are none, in process() we should try to accept in current DFA, else it is a parsing error.
            val nextAction: Map<SymbolT, MutableMap<DFAStateReference<StateT, SymbolT, Production<SymbolT>>, SymbolT?>> =
                enumValues<SymbolT>().toList().associateWith { mutableMapOf() }

            analyzedGrammar.automata.forEach { (dfaLabel, dfa) ->
                // dfa: automaton for the grammar symbol

                dfa.getAllStates().forEach { curState ->
                    // curState: dfa state we are currently in

                    val curStateRef = DFAStateReference(curState, dfa)

                    enumValues<SymbolT>().forEach { inputSymbol ->
                        // inputSymbol: symbol from the input

                        val suitableSymbols = mutableListOf<SymbolT>()
                        enumValues<SymbolT>().forEach prod@{ prodSymbol ->
                            // prodSymbol: symbol in the production

                            dfa.getProduction(curState, prodSymbol) ?: return@prod

                            val dfaForProdSymbol = analyzedGrammar.automata[prodSymbol]
                            if (dfaForProdSymbol != null) { // nonterminal
                                val nextState = dfaForProdSymbol.getStartingState()
                                val nextStateRef = DFAStateReference(nextState, dfaForProdSymbol)
                                if (analyzedGrammar.first[nextStateRef]!!.contains(inputSymbol) or (prodSymbol == inputSymbol)) {
                                    suitableSymbols.add(prodSymbol)
                                }
                                if (analyzedGrammar.nullable.contains(nextStateRef) and
                                    analyzedGrammar.follow[nextStateRef]!!.contains(inputSymbol)
                                ) {
                                    suitableSymbols.add(prodSymbol)
                                }
                            } else { // terminal
                                if (prodSymbol == inputSymbol) {
                                    suitableSymbols.add(prodSymbol)
                                }
                            }
                        }

                        // Leaving this for the convenience in (very) possible future debugging.
                        // println("state $curState in $dfaLabel, possibilities for $inputSymbol are $suitableSymbols")

                        when (suitableSymbols.size) {
                            0 -> nextAction[inputSymbol]!![curStateRef] = null
                            1 -> nextAction[inputSymbol]!![curStateRef] = suitableSymbols[0]
                            else -> throw ParserConstructorError(
                                "Not an LL(1) grammar: " +
                                    "state $curState in dfa $dfaLabel, possible productions for $inputSymbol are $suitableSymbols",
                            )
                        }
                    }
                }
            }

            return LLOneParser(nextAction, analyzedGrammar.startSymbol, analyzedGrammar.automata, analyzedGrammar.syncSymbols)
        }
    }

    override fun process(terminals: List<ParseTree.Leaf<SymbolT>>, diagnostics: Diagnostics): ParseTree<SymbolT> {
        if (terminals.isEmpty()) {
            throw ParsingException("Unable to parse empty input.")
        }

        val terminalIterator = terminals.iterator()
        var terminal = terminalIterator.next()
        var eof = false

        fun goToSyncSymbol() {
            while (!syncSymbols.contains(terminal.token.category) && terminalIterator.hasNext()) {
                terminal = terminalIterator.next()
            }
            if (!syncSymbols.contains(terminal.token.category) && !terminalIterator.hasNext()) {
                eof = true
            }
        }

        fun topDownParse(symbol: SymbolT): ParseTree<SymbolT> {
            if (symbol == terminal.token.category) {
                return terminal.also {
                    if (terminalIterator.hasNext()) {
                        terminal = terminalIterator.next()
                    } else {
                        eof = true
                    }
                }
            }

            val children = mutableListOf<ParseTree<SymbolT>>()

            val dfa = automata[symbol]!!
            var state = dfa.getStartingState()

            do {
                nextAction[terminal.token.category]?.get(Pair(state, dfa))?.let { nextSymbol ->
                    dfa.getProduction(state, nextSymbol)?.let { nextState ->
                        try {
                            state = nextState
                            children.add(topDownParse(nextSymbol))
                        } catch (e: ParsingException) {
                            if (!eof && dfa.getProduction(state, terminal.token.category) == null) {
                                throw diagnostics.fatal()
                            } else {
                            }
                        }
                    } ?: run {
                        diagnostics.report(ParserDiagnostics.UnexpectedToken(terminal.token.category.name, symbol.name), terminal.range)
                        goToSyncSymbol()
                        throw ParsingException("Unable to go to desired symbol $nextSymbol from $state in DFA for $symbol")
                    }
                } ?: break
            } while (!eof)
            if (!dfa.isAccepting(state)) {
                diagnostics.report(ParserDiagnostics.UnexpectedToken(terminal.token.category.name, symbol.name), terminal.range)
                goToSyncSymbol()
                throw ParsingException("State $state in DFA for $symbol is not accepting")
            }

            if (children.isEmpty()) {
                diagnostics.report(ParserDiagnostics.ChildrenEmpty(symbol.name), terminal.range)
                throw diagnostics.fatal()
            }

            val range = Pair(children.first().range.first, children.last().range.second)
            return ParseTree.Branch(range, dfa.result(state)!!, children)
        }

        return topDownParse(startSymbol).also {
            if (!eof) {
                diagnostics.report(ParserDiagnostics.UnableToContinueParsing(terminal.token.category.name), terminal.range)
            }
        }
    }
}
