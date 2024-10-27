package cacophony.parser

import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.DFAStateReference
import cacophony.grammars.ParseTree
import cacophony.grammars.Production
import cacophony.utils.Diagnostics
import cacophony.utils.Input
import kotlin.collections.mutableListOf

class LLOneParser<StateType, SymbolType : Enum<SymbolType>>(
    private val nextAction: Map<SymbolType, Map<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, SymbolType?>>,
    private val startSymbol: SymbolType,
    private val automata: Map<SymbolType, DFAStateReference<StateType, SymbolType, Production<SymbolType>>>,
    private val input: Input,
    private val syncSymbols: Collection<SymbolType>,
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
                    }
                }
            }

            var children = mutableListOf<ParseTree<SymbolType>>()

            val stateRef = automata[symbol]!!
            val dfa = stateRef.second
            var state = stateRef.first

            while (terminalIterator.hasNext()) {
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
                        diagnostics.report("Unexpected token $terminal", input, terminal.token)
                        goToSyncSymbol()
                        throw ParserError("no edge")
                    }
                } ?: break
            }
            if (!dfa.isAccepting(state)) {
                diagnostics.report("Unexpected token $terminal", input, terminal.token)
                goToSyncSymbol()
                throw ParserError("not accepting")
            }

            val range = Pair(children.first().range.first, children.last().range.second)
            return ParseTree.Branch(range, dfa.result(state)!!, children)
        }

        return topDownParse(startSymbol)
    }
}
