package cacophony.parser

import cacophony.grammars.ParseTree
import cacophony.utils.Diagnostics

interface Parser<Symbol : Enum<Symbol>> {
    fun process(
        terminals: List<ParseTree.Leaf<Symbol>>,
        diagnostics: Diagnostics,
    ): ParseTree<Symbol>
}
