@file:Suppress("ktlint:standard:no-wildcard-imports")

package cacophony.semantic

import cacophony.grammars.ParseTree
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.parser.CacophonyGrammarSymbol.*
import cacophony.semantic.syntaxtree.*
import cacophony.utils.Diagnostics
import cacophony.utils.Location
import cacophony.utils.TreePrinter

private fun pruneParseTree(
    parseTree: ParseTree<CacophonyGrammarSymbol>,
    diagnostics: Diagnostics,
): ParseTree<CacophonyGrammarSymbol>? {
    if (parseTree is ParseTree.Branch) {
        if (parseTree.children.size == 1) {
            return pruneParseTree(parseTree.children[0], diagnostics)
        } else {
            val newChildren = parseTree.children.map { pruneParseTree(it, diagnostics) }
            return ParseTree.Branch(parseTree.range, parseTree.production, newChildren.filterNotNull())
        }
    } else if (parseTree is ParseTree.Leaf) {
        val symbol: CacophonyGrammarSymbol = parseTree.token.category
        if (symbol.syntaxTreeClass == null) {
            return null
        }
        return parseTree
    }
    return parseTree
}

private fun generateASTInternal(
    parseTree: ParseTree<CacophonyGrammarSymbol>,
    diagnostics: Diagnostics,
): Expression {
    if (parseTree is ParseTree.Leaf) {
        val symbol: CacophonyGrammarSymbol = parseTree.token.category
        val context = parseTree.token.context
        return when (symbol) {
            VARIABLE_IDENTIFIER -> VariableUse(parseTree.range, context)
            INT_LITERAL -> Literal.IntLiteral(parseTree.range, context.toInt())
            BOOL_LITERAL -> Literal.BoolLiteral(parseTree.range, context.toBoolean())
            KEYWORD_BREAK -> Statement.BreakStatement(parseTree.range)
            else -> throw IllegalArgumentException("Unexpected leaf symbol: $symbol")
        }
    } else if (parseTree is ParseTree.Branch) {
        val symbol: CacophonyGrammarSymbol = parseTree.production.lhs
        val range = parseTree.range
        val childNum = parseTree.children.size
        return when (symbol) {
            START -> {
                val newChildren = parseTree.children.map { generateASTInternal(it, diagnostics) }
                Block(range, newChildren)
            }
            FUNCTION_CALL -> {
                TODO("This one is tricky")
            }
            WHILE_CLAUSE -> {
                assert(childNum == 2)
                val testExpression = generateASTInternal(parseTree.children[0], diagnostics)
                val doExpression = generateASTInternal(parseTree.children[1], diagnostics)
                Statement.WhileStatement(range, testExpression, doExpression)
            }
            IF_CLAUSE -> {
                assert(childNum == 2 || childNum == 3)
                val testExpression = generateASTInternal(parseTree.children[0], diagnostics)
                val doExpression = generateASTInternal(parseTree.children[1], diagnostics)
                val elseExpression = (if (childNum > 2) generateASTInternal(parseTree.children[2], diagnostics) else null)
                Statement.IfElseStatement(range, testExpression, doExpression, elseExpression)
            }
            DECLARATION_TYPED -> {
                TODO("")
            }
            DECLARATION_UNTYPED -> {
                TODO("")
            }
            VARIABLE_DECLARATION -> {
                TODO("")
            }
            FUNCTION_DECLARATION -> {
                TODO("")
            }
            FUNCTION_ARGUMENT -> {
                TODO("")
            }
            ASSIGNMENT -> {
                TODO("")
            }
            UNARY -> {
                TODO("")
            }
            BLOCK -> {
                val newChildren = parseTree.children.map { generateASTInternal(it, diagnostics) }
                Block(range, newChildren)
            }

            else -> throw IllegalArgumentException("Unexpected branch symbol: $symbol")
        }
    }
    return Empty(range = Pair(Location(0), Location(0)))
}

fun generateAST(
    parseTree: ParseTree<CacophonyGrammarSymbol>,
    diagnostics: Diagnostics,
): AST {
    val prunedTree = pruneParseTree(parseTree, diagnostics)
    println("PRUNED TREE")
    println(TreePrinter(StringBuilder()).printTree(prunedTree!!))
    val ast = generateASTInternal(prunedTree, diagnostics)
    println("AST")
    println(TreePrinter(StringBuilder()).printTree(ast))
    return Block(Pair(Location(0), Location(0)), listOf())
}
