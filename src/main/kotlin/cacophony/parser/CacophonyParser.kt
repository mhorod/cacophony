package cacophony.parser

import cacophony.diagnostics.Diagnostics
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree

class CacophonyParser : Parser<CacophonyGrammarSymbol> {
    private val innerParser: LLOneParser<Int, CacophonyGrammarSymbol>

    init {
        val analyzedGrammar = AnalyzedGrammar.fromGrammar(syncSymbols, grammar)
        innerParser = LLOneParser.fromAnalyzedGrammar(analyzedGrammar)
    }

    override fun process(
        terminals: List<ParseTree.Leaf<CacophonyGrammarSymbol>>,
        diagnostics: Diagnostics,
    ): ParseTree<CacophonyGrammarSymbol> = innerParser.process(terminals, diagnostics)

    companion object {
        val syncSymbols = setOf(CacophonyGrammarSymbol.SEMICOLON)
        val grammar = CacophonyGrammar.grammar
    }
}
