package cacophony.parser

import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.utils.Diagnostics

class LLOneParser<Symbol : Enum<Symbol>> : Parser<Symbol> {
    companion object {
        fun <State, Symbol : Enum<Symbol>> fromAnalyzedGrammar(analyzedGrammar: AnalyzedGrammar<State, Symbol>): LLOneParser<Symbol> {
            TODO("Not yet implemented")
        }
    }

    override fun process(
        terminals: List<ParseTree.Leaf<Symbol>>,
        diagnostics: Diagnostics,
    ): ParseTree<Symbol> {
        TODO("Not yet implemented")
    }
}
