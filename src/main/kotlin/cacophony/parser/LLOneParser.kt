package cacophony.parser

import cacophony.automata.DFA
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.DFAStateReference
import cacophony.grammars.ParseTree
import cacophony.grammars.Production
import cacophony.utils.Diagnostics
import cacophony.utils.Input
import kotlin.collections.mutableListOf

class LLOneParser<StateType, SymbolType : Enum<SymbolType>>(
    private val nextAction: Map<SymbolType, Map<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, SymbolType>>,
    private val startSymbol: SymbolType,
    private val automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
    private val syncSymbols: Collection<SymbolType>,
    private val input: Input,
) : Parser<SymbolType> {
    companion object {
        fun <StateType, SymbolType : Enum<SymbolType>> fromAnalyzedGrammar(
            analyzedGrammar: AnalyzedGrammar<StateType, SymbolType>,
        ): LLOneParser<StateType, SymbolType> {
            TODO("Not yet implemented")
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

            var children = mutableListOf<ParseTree<SymbolType>>()

            val dfa = automata[symbol]!!
            var state = dfa.getStartingState()

            do {
                nextAction[terminal.token.category]?.get(Pair(state, dfa))?.let { nextSymbol ->
                    dfa.getProduction(state, nextSymbol)?.let { nextState ->
                        try {
                            state = nextState
                            children.add(topDownParse(nextSymbol))
                        } catch (e: ParserError) {
                            if (dfa.getProduction(state, terminal.token.category) == null) {
                                throw e
                            }
                        }
                    } ?: run {
                        diagnostics.report("Unexpected token $terminal for $symbol", input, terminal.token)
                        goToSyncSymbol()
                        throw ParserError("no edge")
                    }
                } ?: break
            } while (!eof)
            if (!dfa.isAccepting(state)) {
                diagnostics.report("Unexpected token $terminal for $symbol", input, terminal.token)
                goToSyncSymbol()
                throw ParserError("State $state in DFA for $symbol is not accepting")
            }

            val range = Pair(children.first().range.first, children.last().range.second)
            return ParseTree.Branch(range, dfa.result(state)!!, children)
        }

        return topDownParse(startSymbol)
    }
}
