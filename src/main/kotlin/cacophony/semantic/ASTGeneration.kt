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
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

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
        if (symbol.syntaxTreeClass == null && symbol != SEMICOLON) {
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

private fun constructFunctionArgument(
    parseTree: ParseTree<CacophonyGrammarSymbol>,
    diagnostics: Diagnostics,
): Definition.FunctionArgument {
    val tree = parseTree as ParseTree.Branch
    assert(parseTree.children.size == 2)
    val range = parseTree.range
    val argName = parseTree.children[0]
    if (argName is ParseTree.Leaf<*>) {
        return Definition.FunctionArgument(
            range,
            argName.token.context,
            constructType(parseTree.children[1], diagnostics),
        )
    } else {
        throw IllegalArgumentException("Expected argument identifier, got: $argName")
    }
}

fun <T : OperatorBinary> createInstance(
    kClass: KClass<T>,
    range: Pair<Location, Location>,
    lhs: Expression,
    rhs: Expression,
): Expression {
    val constructor = kClass.primaryConstructor
    return constructor!!.call(range, lhs, rhs)
}

private fun operatorRegexToAST(
    children: List<ParseTree<CacophonyGrammarSymbol>>,
    diagnostics: Diagnostics,
): Expression {
    val childNum = children.size
    if (childNum == 1) {
        return generateASTInternal(children[0], diagnostics)
    } else {
        val operator = children[childNum - 2]
        val newChildren = children.subList(0, childNum - 2)
        val range = Pair(first = children[0].range.first, second = children[childNum - 1].range.second)
        if (operator is ParseTree.Leaf) {
            val symbol = operator.token.category
            return createInstance(
                symbol.syntaxTreeClass!! as KClass<OperatorBinary>,
                range,
                operatorRegexToAST(newChildren, diagnostics),
                generateASTInternal(children[childNum - 1], diagnostics),
            )
        } else {
            throw IllegalArgumentException("Expected the operator symbol, got: $operator")
        }
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
            START, BLOCK -> {
                val newChildren: MutableList<Expression> = mutableListOf<Expression>()
                var seekingExpression = true
                parseTree.children.forEach {
                    if (it is ParseTree.Leaf && it.token.category == SEMICOLON) {
                        if (seekingExpression) {
                            newChildren.add(Empty(Pair(first = it.range.first, second = it.range.first)))
                        }
                        seekingExpression = true
                    } else {
                        newChildren.add(generateASTInternal(it, diagnostics))
                        seekingExpression = false
                    }
                }
                if (seekingExpression) {
                    newChildren.add(
                        Empty(
                            Pair(
                                Location(range.second.value - 1),
                                Location(range.second.value - 1),
                            ),
                        ),
                    )
                }
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
                val declarationPosition: Int
                if (getGrammarSymbol(isDeclarationTyped) == DECLARATION_TYPED) {
                    type = constructType(isDeclarationTyped.children[0], diagnostics)
                    declarationPosition = 2
                } else {
                    declarationPosition = 1
                }
                var declarationKind = isDeclarationTyped.children[declarationPosition]
                if (getGrammarSymbol(declarationKind) == FUNCTION_DECLARATION) {
                    declarationKind = declarationKind as ParseTree.Branch
                    val branchesNum = declarationKind.children.size
                    val returnType = declarationKind.children[branchesNum - 2]
                    val body = declarationKind.children[branchesNum - 1]
                    var arguments: List<Definition.FunctionArgument> = listOf()
                    if (branchesNum >= 3) {
                        val unparsedArguments = declarationKind.children.subList(0, branchesNum - 2)
                        arguments = unparsedArguments.map { constructFunctionArgument(it, diagnostics) } // non-empty function argument list
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
            ASSIGNMENT_LEVEL -> {
                assert(childNum == 3)
                val operator = parseTree.children[1]
                if (operator is ParseTree.Leaf) {
                    val assignmentSymbol = operator.token.category
                    return when (assignmentSymbol) {
                        OPERATOR_ASSIGNMENT ->
                            OperatorBinary.Assignment(
                                range,
                                generateASTInternal(parseTree.children[0], diagnostics),
                                generateASTInternal(parseTree.children[2], diagnostics),
                            )
                        OPERATOR_SUBTRACTION_ASSIGNMENT ->
                            OperatorBinary.SubtractionAssignment(
                                range,
                                generateASTInternal(parseTree.children[0], diagnostics),
                                generateASTInternal(parseTree.children[2], diagnostics),
                            )
                        OPERATOR_ADDITION_ASSIGNMENT ->
                            OperatorBinary.AdditionAssignment(
                                range,
                                generateASTInternal(parseTree.children[0], diagnostics),
                                generateASTInternal(parseTree.children[2], diagnostics),
                            )
                        OPERATOR_MULTIPLICATION_ASSIGNMENT ->
                            OperatorBinary.MultiplicationAssignment(
                                range,
                                generateASTInternal(parseTree.children[0], diagnostics),
                                generateASTInternal(parseTree.children[2], diagnostics),
                            )
                        OPERATOR_DIVISION_ASSIGNMENT ->
                            OperatorBinary.DivisionAssignment(
                                range,
                                generateASTInternal(parseTree.children[0], diagnostics),
                                generateASTInternal(parseTree.children[2], diagnostics),
                            )
                        OPERATOR_MODULO_ASSIGNMENT ->
                            OperatorBinary.ModuloAssignment(
                                range,
                                generateASTInternal(parseTree.children[0], diagnostics),
                                generateASTInternal(parseTree.children[2], diagnostics),
                            )

                        else -> throw IllegalArgumentException("Expected the assignment operator, got: $assignmentSymbol")
                    }
                } else {
                    throw IllegalArgumentException("Expected the operator symbol, got: $operator")
                }
            }
            ADDITION_LEVEL, MULTIPLICATION_LEVEL, EQUALITY_LEVEL, COMPARATOR_LEVEL, LOGICAL_OPERATOR_LEVEL -> {
                assert(childNum >= 3)
                operatorRegexToAST(parseTree.children, diagnostics)
            }
            UNARY_LEVEL -> {
                assert(childNum == 2)
                val unaryOperator = parseTree.children[0]
                if (unaryOperator is ParseTree.Leaf) {
                    val unarySymbol = unaryOperator.token.category
                    return when (unarySymbol) {
                        OPERATOR_SUBTRACTION ->
                            OperatorUnary.Minus(
                                range,
                                generateASTInternal(parseTree.children[1], diagnostics),
                            )
                        OPERATOR_LOGICAL_NOT ->
                            OperatorUnary.Negation(
                                range,
                                generateASTInternal(parseTree.children[1], diagnostics),
                            )
                        else -> throw IllegalArgumentException("Expected the unary operator: $unarySymbol")
                    }
                } else {
                    throw IllegalArgumentException("Expected the operator symbol, got: $unaryOperator")
                }
            }

            else -> throw IllegalArgumentException("Unexpected branch symbol: $symbol")
        }
    }
    return Empty(range = Pair(Location(0), Location(0)))
}

fun generateAST(
    parseTree: ParseTree<CacophonyGrammarSymbol>,
    diagnostics: Diagnostics,
): Expression {
    val prunedTree = pruneParseTree(parseTree, diagnostics)
    println("PRUNED TREE")
    println(TreePrinter(StringBuilder()).printTree(prunedTree!!))
    val ast = generateASTInternal(prunedTree, diagnostics)
    println("AST")
    println(TreePrinter(StringBuilder()).printTree(ast))
    return ast
}
