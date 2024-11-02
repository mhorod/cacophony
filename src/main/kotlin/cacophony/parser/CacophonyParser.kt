package cacophony.parser

import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.utils.Diagnostics
import cacophony.utils.Location

class CacophonyParser : Parser<CacophonyGrammarSymbol> {
    private val innerParser: LLOneParser<Int, CacophonyGrammarSymbol>

    init {
        val analyzedGrammar = AnalyzedGrammar.fromGrammar(syncSymbols, grammar)
        innerParser = LLOneParser.fromAnalyzedGrammar(analyzedGrammar)
    }

    override fun process(
        terminals: List<ParseTree.Leaf<CacophonyGrammarSymbol>>,
        diagnostics: Diagnostics,
    ): ParseTree<CacophonyGrammarSymbol> {
        return terminals[0]
    }

    companion object {
        val syncSymbols = setOf<CacophonyGrammarSymbol>()
        val grammar = CacophonyGrammar.dummyGrammar1 // TODO: change this to the actual grammar
    }
}
