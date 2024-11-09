package cacophony.parser

import cacophony.diagnostics.Diagnostics
import cacophony.grammars.ParseTree
import cacophony.utils.CompileException

interface Parser<SymbolType : Enum<SymbolType>> {
    fun process(
        terminals: List<ParseTree.Leaf<SymbolType>>,
        diagnostics: Diagnostics,
    ): ParseTree<SymbolType>
}

class ParsingException(
    reason: String,
) : CompileException(reason)
