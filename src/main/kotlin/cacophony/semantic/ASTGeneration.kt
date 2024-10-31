package cacophony.semantic

import cacophony.grammars.ParseTree
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.semantic.syntaxtree.AST
import cacophony.utils.Diagnostics

fun generateAST(
    parseTree: ParseTree<CacophonyGrammarSymbol>,
    diagnostics: Diagnostics,
): AST {
    TODO("Implement AST generation")
}
