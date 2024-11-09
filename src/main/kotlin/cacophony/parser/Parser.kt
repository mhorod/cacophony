package cacophony.parser

import cacophony.grammars.ParseTree
import cacophony.utils.CompileException
import cacophony.utils.Diagnostics

interface Parser<SymbolT : Enum<SymbolT>> {
    fun process(
        terminals: List<ParseTree.Leaf<SymbolT>>,
        diagnostics: Diagnostics,
    ): ParseTree<SymbolT>
}

class ParsingException(
    reason: String,
) : CompileException(reason)
