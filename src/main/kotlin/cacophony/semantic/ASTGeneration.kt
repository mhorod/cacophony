@file:Suppress("ktlint:standard:no-wildcard-imports")

package cacophony.semantic

import cacophony.grammars.ParseTree
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.parser.CacophonyGrammarSymbol.*
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.syntaxtree.Type
import cacophony.utils.Diagnostics
import cacophony.utils.Location
import cacophony.utils.TreePrinter

private fun getGrammarSymbol(parseTree: ParseTree<CacophonyGrammarSymbol>): CacophonyGrammarSymbol {
    if (parseTree is ParseTree.Leaf) {
        return parseTree.token.category
    } else if (parseTree is ParseTree.Branch) {
        return parseTree.production.lhs
    }
    throw Exception("Parse tree must be either Leaf or Branch")
}

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

private fun constructType(
    parseTree: ParseTree<CacophonyGrammarSymbol>,
    diagnostics: Diagnostics,
): Type {
    if (getGrammarSymbol(parseTree) == TYPE_IDENTIFIER) {
        val tree = parseTree as ParseTree.Leaf
        return Type
            .Basic(tree.range, tree.token.context)
    } else {
        val tree = parseTree as ParseTree.Branch
        val childNum = tree.children.size
        val returnType = constructType(tree.children.last(), diagnostics)
        val argumentsTypes = mutableListOf<Type>()
        for (i in 0 until (childNum - 1)) {
            argumentsTypes.add(constructType(tree.children[i], diagnostics))
        }
        return Type
            .Functional(tree.range, argumentsTypes, returnType)
    }
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
            // call level is in the pruned tree iff there is an actual function call underneath
            CALL_LEVEL -> {
                assert(childNum == 2)
                val function = generateASTInternal(parseTree.children[0], diagnostics)
                val argumentsParent = parseTree.children[1]
                val arguments = mutableListOf<Expression>()
                if (argumentsParent is ParseTree.Branch<CacophonyGrammarSymbol>) {
                    assert(argumentsParent.production.lhs == FUNCTION_CALL)
                    for (child in argumentsParent.children) {
                        arguments.add(generateASTInternal(child, diagnostics))
                    }
                    return FunctionCall(range, function, arguments)
                }
                throw Exception("Fatal: set of function call arguments should be a parse tree Branch!")
            }
            // DECLARATION_LEVEL is in the pruned graph iff it corresponds to a declaration
            DECLARATION_LEVEL -> {
                val identifier = parseTree.children[0] as ParseTree.Leaf
                val isDeclarationTyped = parseTree.children[1] as ParseTree.Branch
                var type: Type? = null
                if (getGrammarSymbol(isDeclarationTyped) == DECLARATION_TYPED) {
                    type = constructType(isDeclarationTyped.children[0], diagnostics)
                }
                var declarationKind = isDeclarationTyped.children[1]
                if (getGrammarSymbol(declarationKind) == FUNCTION_DECLARATION) {
                    declarationKind = declarationKind as ParseTree.Branch
                    val branchesNum = declarationKind.children.size
                    val returnType = declarationKind.children[branchesNum - 2]
                    val body = declarationKind.children[branchesNum - 1]
                    val arguments: MutableList<Definition.FunctionArgument> = mutableListOf()
                    if (branchesNum == 3) { // non-empty function argument list
                        val args = declarationKind.children[0] as ParseTree.Branch
                        // function arguments
                        val argsCount = args.children.size / 2
                        for (i in 0 until argsCount) {
                            val argIdentifier = args.children[i * 2] as ParseTree.Leaf
                            val argType = constructType(args.children[i * 2 + 1], diagnostics)
                            arguments.add(
                                Definition.FunctionArgument(
                                    Pair(argIdentifier.range.first, argType.range.second),
                                    argIdentifier.token.context,
                                    argType,
                                ),
                            )
                        }
                    }
                    return Definition.FunctionDeclaration(
                        range,
                        identifier.token.context,
                        type,
                        arguments,
                        constructType(returnType, diagnostics),
                        generateASTInternal(body, diagnostics),
                    )
                } else { // VARIABLE_DECLARATION, which was pruned
                    return Definition.VariableDeclaration(
                        range,
                        identifier.token.context,
                        type,
                        generateASTInternal(isDeclarationTyped.children.last(), diagnostics),
                    )
                }
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
