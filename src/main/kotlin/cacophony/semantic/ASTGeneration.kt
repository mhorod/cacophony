package cacophony.semantic

import cacophony.grammars.ParseTree
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Expression
import cacophony.utils.Diagnostics
import cacophony.utils.Location

fun generateAST(
    parseTree: ParseTree<CacophonyGrammarSymbol>,
    diagnostics: Diagnostics,
): AST {
    return AST(Location(1) to Location(2), ArrayList<Expression>())
}
