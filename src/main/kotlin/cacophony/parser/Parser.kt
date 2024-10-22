package cacophony.parser

import cacophony.grammars.ParseTree
import cacophony.utils.Diagnostics

interface Parser<SymbolType : Enum<SymbolType>> {
    fun process(
        terminals: List<ParseTree.Leaf<SymbolType>>,
        diagnostics: Diagnostics,
    ): ParseTree<SymbolType>
}
