package cacophony.parser

import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.utils.Diagnostics

class LLOneParser<SymbolType : Enum<SymbolType>> : Parser<SymbolType> {
    companion object {
        fun <StateType, SymbolType : Enum<SymbolType>> fromAnalyzedGrammar(
            analyzedGrammar: AnalyzedGrammar<StateType, SymbolType>,
        ): LLOneParser<SymbolType> {
            TODO("Not yet implemented")
        }
    }

    override fun process(
        terminals: List<ParseTree.Leaf<SymbolType>>,
        diagnostics: Diagnostics,
    ): ParseTree<SymbolType> {
        TODO("Not yet implemented")
    }
}
