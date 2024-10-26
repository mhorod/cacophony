package cacophony.parser

import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.DFAStateReference
import cacophony.grammars.ParseTree
import cacophony.grammars.Production
import cacophony.utils.Diagnostics

class LLOneParser<StateType, SymbolType : Enum<SymbolType>>(
    private val nextAction: Map<SymbolType, MutableMap<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, SymbolType?>>,
) : Parser<SymbolType> {
    companion object {
        // Constructs LLOneParser with computed nextAction map.
        // nextAction gives symbol on the production that we should use next
        // depending on the current input symbol and dfa state.
        // null denotes no suitable production, then we should accept.
        // Throws if grammar is not LL(1).
        inline fun <StateType, reified SymbolType : Enum<SymbolType>> fromAnalyzedGrammar(
            analyzedGrammar: AnalyzedGrammar<StateType, SymbolType>,
        ): LLOneParser<StateType, SymbolType> {
            val nextAction: Map<SymbolType, MutableMap<DFAStateReference<StateType, SymbolType, Production<SymbolType>>, SymbolType?>> =
                enumValues<SymbolType>().toList().associateWith { mutableMapOf() }

            analyzedGrammar.automata.forEach { (dfaLabel, dfa) -> // automaton for the grammar symbol
                dfa.getAllStates().forEach { curState -> // dfa state we are currently in
                    val curStateRef = DFAStateReference(curState, dfa)

                    enumValues<SymbolType>().forEach { inputSymbol -> // symbol from the input
                        val suitableSymbols = mutableListOf<SymbolType>()
                        enumValues<SymbolType>().forEach { prodSymbol -> // symbol in the production
                            val nextState = dfa.getProduction(curState, prodSymbol)
                            if (nextState != null) {
                                val nextStateRef = DFAStateReference(nextState, dfa)
                                if (analyzedGrammar.first[nextStateRef]?.contains(inputSymbol) == true) {
                                    suitableSymbols.add(prodSymbol)
                                } else if (analyzedGrammar.nullable.contains(nextStateRef) and
                                    (analyzedGrammar.follow[nextStateRef]?.contains(inputSymbol) == true)
                                ) {
                                    suitableSymbols.add(prodSymbol)
                                }
                            }
                        }

                        if (suitableSymbols.size > 1) { // too many productions are suitable
                            // TODO: more expressive error
                            throw ParserConstructorErrorException("Sorry, this is not an LL(1) grammar :c")
                        }
                        if (suitableSymbols.size == 1) {
                            nextAction[inputSymbol]!![curStateRef] = suitableSymbols[0]
                        } else { // acceptableSymbols.size == 0
                            if (dfa.isAccepting(curState)) {
                                nextAction[inputSymbol]!![curStateRef] = null
                            } else { // nowhere to go and cannot accept
                                // TODO: more expressive error
                                throw ParserConstructorErrorException("Sorry, this is not an LL(1) grammar :c")
                            }
                        }
                    }
                }
            }

            return LLOneParser(nextAction)
        }
    }

    override fun process(
        terminals: List<ParseTree.Leaf<SymbolType>>,
        diagnostics: Diagnostics,
    ): ParseTree<SymbolType> {
        TODO("Not yet implemented")
    }
}
