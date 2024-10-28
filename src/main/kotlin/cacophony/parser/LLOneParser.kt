package cacophony.parser

import cacophony.automata.DFA
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.DFAStateReference
import cacophony.grammars.ParseTree
import cacophony.grammars.Production
import cacophony.utils.Diagnostics
import kotlin.collections.mutableListOf

class ParserConstructorErrorException(
    reason: String,
) : Exception(reason)

class LLOneParser<StateType, SymbolType : Enum<SymbolType>>(
    private val nextAction: Map<SymbolType, Map<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, SymbolType?>>,
    private val startSymbol: SymbolType,
    private val automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
    private val syncSymbols: Collection<SymbolType>,
) : Parser<SymbolType> {
    companion object {
        // Constructs LLOneParser with computed nextAction map.
        // nextAction gives symbol on the production that we should use next
        // depending on the current input symbol and dfa state.
        // null denotes no suitable production, then we should try to accept.
        // Throws if grammar is not LL(1).
        inline fun <StateType, reified SymbolType : Enum<SymbolType>> fromAnalyzedGrammar(
            analyzedGrammar: AnalyzedGrammar<StateType, SymbolType>,
        ): LLOneParser<StateType, SymbolType> {
            // Currently the logic is as follows:
            // 1. Loop over all DFAStateReference and inputSymbol: SymbolType.
            // 2. For each production with some symbol P, using nullable, first and follow on the start state of the DFA for P
            //    find all suitable productions (if there is no DFA for P, meaning P is terminal, production is suitable iff P == inputSymbol).
            // 3. If there are at least 2 suitable productions then our grammar is not LL(1).
            // 4. If there is exactly one then ok.
            // 5. If there are none, in process() we should try to accept in current DFA, else it is a parsing error.
            val nextAction: Map<SymbolType, MutableMap<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, SymbolType?>> =
                enumValues<SymbolType>().toList().associateWith { mutableMapOf() }

            analyzedGrammar.automata.forEach { (dfaLabel, dfa) ->
                // dfa: automaton for the grammar symbol

                dfa.getAllStates().forEach { curState ->
                    // curState: dfa state we are currently in

                    val curStateRef = DFAStateReference(curState, dfa)

                    enumValues<SymbolType>().forEach { inputSymbol ->
                        // inputSymbol: symbol from the input

                        val suitableSymbols = mutableListOf<SymbolType>()
                        enumValues<SymbolType>().forEach prod@{ prodSymbol ->
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
                            else -> throw ParserConstructorErrorException(
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

    override fun process(
        terminals: List<ParseTree.Leaf<SymbolType>>,
        diagnostics: Diagnostics,
    ): ParseTree<SymbolType> {
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

        fun topDownParse(symbol: SymbolType): ParseTree<SymbolType> {
            if (symbol == terminal.token.category) {
                return terminal.also {
                    if (terminalIterator.hasNext()) {
                        terminal = terminalIterator.next()
                    } else {
                        eof = true
                    }
                }
            }

            val children = mutableListOf<ParseTree<SymbolType>>()

            val dfa = automata[symbol]!!
            var state = dfa.getStartingState()

            do {
                nextAction[terminal.token.category]?.get(Pair(state, dfa))?.let { nextSymbol ->
                    dfa.getProduction(state, nextSymbol)?.let { nextState ->
                        try {
                            state = nextState
                            children.add(topDownParse(nextSymbol))
                        } catch (e: ParsingErrorException) {
                            if (!eof && dfa.getProduction(state, terminal.token.category) == null) {
                                throw e
                            } else {}
                        }
                    } ?: run {
                        diagnostics.report("Unexpected token ${terminal.token.category} while parsing $symbol", terminal.range)
                        goToSyncSymbol()
                        throw ParsingErrorException("Unable to go to desired symbol $nextSymbol from $state in DFA for $symbol")
                    }
                } ?: break
            } while (!eof)
            if (!dfa.isAccepting(state)) {
                diagnostics.report("Unexpected token ${terminal.token.category} while parsing $symbol", terminal.range)
                goToSyncSymbol()
                throw ParsingErrorException("State $state in DFA for $symbol is not accepting")
            }

            val range = Pair(children.first().range.first, children.last().range.second)
            return ParseTree.Branch(range, dfa.result(state)!!, children)
        }

        return topDownParse(startSymbol)
    }
}
