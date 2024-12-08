package cacophony.semantic.syntaxtree

import cacophony.diagnostics.ASTDiagnostics
import cacophony.diagnostics.Diagnostics
import cacophony.grammars.ParseTree
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.parser.CacophonyGrammarSymbol.*
import cacophony.utils.Location
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

const val MAIN_FUNCTION_IDENTIFIER = "<program>"

private fun getGrammarSymbol(parseTree: ParseTree<CacophonyGrammarSymbol>): CacophonyGrammarSymbol =
    when (parseTree) {
        is ParseTree.Leaf -> parseTree.token.category
        is ParseTree.Branch -> parseTree.production.lhs
    }

private fun pruneParseTree(parseTree: ParseTree<CacophonyGrammarSymbol>, diagnostics: Diagnostics): ParseTree<CacophonyGrammarSymbol>? {
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

private fun constructType(parseTree: ParseTree<CacophonyGrammarSymbol>, diagnostics: Diagnostics): Type =
    when (val symbol = getGrammarSymbol(parseTree)) {
        TYPE_IDENTIFIER -> {
            require(parseTree is ParseTree.Leaf) { "Unable to construct atomic type from non-leaf node $symbol" }
            BaseType.Basic(parseTree.range, parseTree.token.context)
        }
        FUNCTION_TYPE -> {
            require(parseTree is ParseTree.Branch) { "Unable to construct functional type from leaf node $symbol" }
            val returnType = constructType(parseTree.children.last(), diagnostics)
            val argumentsTypes = parseTree.children.slice(0..<parseTree.children.size - 1).map { constructType(it, diagnostics) }
            BaseType.Functional(parseTree.range, argumentsTypes, returnType)
        }
        STRUCT_TYPE -> {
            require(parseTree is ParseTree.Branch) { "Unable to construct structure type from leaf node $symbol" }
            BaseType.Structural(
                parseTree.range,
                parseTree.children
                    .windowed(2, 2) { (ident, type) ->
                        require(ident is ParseTree.Leaf) { "Field identifier ${getGrammarSymbol(ident)} is not a leaf" }
                        ident.token to constructType(type, diagnostics)
                    }.also {
                        it.fold(mutableSetOf<String>()) { acc, (ident) ->
                            if (!acc.add(ident.context)) {
                                diagnostics.report(ASTDiagnostics.DuplicateField(ident.context), Pair(ident.rangeFrom, ident.rangeTo))
                                throw diagnostics.fatal()
                            } else acc
                        }
                    }.map { (ident, type) -> ident.context to type }
                    .toMap(),
            )
        }
        else -> throw IllegalStateException("Can't construct type from node $symbol")
    }

private fun constructFunctionArgument(
    parseTree: ParseTree<CacophonyGrammarSymbol>,
    diagnostics: Diagnostics,
): Definition.FunctionArgument {
    val tree = parseTree as ParseTree.Branch
    assert(tree.children.size == 2)
    val range = tree.range
    val argName = tree.children[0]
    if (argName is ParseTree.Leaf<*>) {
        return Definition.FunctionArgument(
            range,
            argName.token.context,
            constructType(tree.children[1], diagnostics),
        )
    } else {
        throw IllegalArgumentException("Expected argument identifier, got: $argName")
    }
}

fun <T : OperatorBinary> createInstanceBinary(
    kClass: KClass<T>,
    range: Pair<Location, Location>,
    lhs: Expression,
    rhs: Expression,
): Expression {
    val constructor = kClass.primaryConstructor
    return constructor!!.call(range, lhs, rhs)
}

fun <T : OperatorUnary> createInstanceUnary(kClass: KClass<T>, range: Pair<Location, Location>, subExpression: Expression): Expression {
    val constructor = kClass.primaryConstructor
    return constructor!!.call(range, subExpression)
}

private fun createStructField(node: ParseTree<CacophonyGrammarSymbol>, diagnostics: Diagnostics): Pair<StructField, Expression> {
    require(node is ParseTree.Branch) { "Struct field should not be a leaf node" }
    require(node.children.size == 2) { "Struct field should have exactly 2 children" }
    val (identifier, def) = node.children
    require(identifier is ParseTree.Leaf) { "Field name should be a variable identifier, got ${getGrammarSymbol(node)}" }
    require(def is ParseTree.Branch) { "Struct field body should not be a leaf node" }
    return when (val cnt = def.children.size) {
        2 -> Pair(StructField(node.range, identifier.token.context, null), generateASTInternal(def.children[1], diagnostics))
        3 ->
            Pair(
                StructField(node.range, identifier.token.context, constructType(def.children[0], diagnostics)),
                generateASTInternal(def.children[2], diagnostics),
            )
        else -> throw IllegalStateException("Struct field ${getGrammarSymbol(node)} has $cnt children, expected 2-3")
    }
}

private fun operatorRegexToAST(children: List<ParseTree<CacophonyGrammarSymbol>>, diagnostics: Diagnostics): Expression {
    val childNum = children.size
    if (childNum == 1) {
        return generateASTInternal(children[0], diagnostics)
    } else {
        val operatorKind = children[childNum - 2]
        val newChildren = children.subList(0, childNum - 2)
        val range = Pair(first = children[0].range.first, second = children[childNum - 1].range.second)
        if (operatorKind is ParseTree.Leaf) {
            val symbol = operatorKind.token.category
            return createInstanceBinary(
                symbol.syntaxTreeClass!! as KClass<OperatorBinary>,
                range,
                operatorRegexToAST(newChildren, diagnostics),
                generateASTInternal(children[childNum - 1], diagnostics),
            )
        } else {
            throw IllegalArgumentException("Expected the operator symbol, got: $operatorKind")
        }
    }
}

private fun generateASTInternal(parseTree: ParseTree<CacophonyGrammarSymbol>, diagnostics: Diagnostics): Expression {
    if (parseTree is ParseTree.Leaf) {
        val symbol: CacophonyGrammarSymbol = parseTree.token.category
        val context = parseTree.token.context
        return when (symbol) {
            VARIABLE_IDENTIFIER -> VariableUse(parseTree.range, context)
            INT_LITERAL ->
                Literal.IntLiteral(
                    parseTree.range,
                    try {
                        context.toInt()
                    } catch (e: NumberFormatException) {
                        diagnostics.report(ASTDiagnostics.ValueOutOfRange(context), parseTree.range)
                        throw diagnostics.fatal()
                    },
                )

            BOOL_LITERAL -> Literal.BoolLiteral(parseTree.range, context.toBoolean())
            KEYWORD_BREAK -> Statement.BreakStatement(parseTree.range)
            else -> throw IllegalArgumentException("Unexpected leaf symbol: $symbol")
        }
    } else if (parseTree is ParseTree.Branch) {
        val range = parseTree.range
        val childNum = parseTree.children.size
        return when (val symbol = parseTree.production.lhs) {
            START, BLOCK -> {
                val newChildren: MutableList<Expression> = mutableListOf()
                var seekingExpression = true
                parseTree.children.forEach {
                    seekingExpression =
                        if ((it is ParseTree.Leaf) && (it.token.category == SEMICOLON)) {
                            if (seekingExpression) {
                                newChildren.add(Empty(Pair(first = it.range.first, second = it.range.first)))
                            }
                            true
                        } else {
                            newChildren.add(generateASTInternal(it, diagnostics))
                            false
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
                        type as BaseType.Functional?,
                        arguments,
                        constructType(returnType, diagnostics),
                        generateASTInternal(body, diagnostics),
                    )
                } else { // VARIABLE_DECLARATION, which was pruned
                    return Definition.VariableDeclaration(
                        range,
                        identifier.token.context,
                        type as BaseType.Basic?,
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

            RETURN_STATEMENT -> {
                assert(childNum == 1)
                val expression = generateASTInternal(parseTree.children[0], diagnostics)
                Statement.ReturnStatement(range, expression)
            }

            ASSIGNMENT_LEVEL -> {
                assert(childNum == 3)
                val operatorKind = parseTree.children[1]
                if (operatorKind is ParseTree.Leaf) {
                    val assignmentSymbol = operatorKind.token.category
                    return createInstanceBinary(
                        assignmentSymbol.syntaxTreeClass!! as KClass<OperatorBinary>,
                        range,
                        generateASTInternal(parseTree.children[0], diagnostics),
                        generateASTInternal(parseTree.children[2], diagnostics),
                    )
                } else {
                    throw IllegalArgumentException("Expected the operator symbol, got: $operatorKind")
                }
            }

            ADDITION_LEVEL, MULTIPLICATION_LEVEL, EQUALITY_LEVEL, COMPARATOR_LEVEL, LOGICAL_OPERATOR_LEVEL -> {
                assert(childNum >= 3)
                operatorRegexToAST(parseTree.children, diagnostics)
            }

            UNARY_LEVEL -> {
                assert(childNum == 2)
                val operatorKind = parseTree.children[0]
                if (operatorKind is ParseTree.Leaf) {
                    val unarySymbol = operatorKind.token.category
                    // we have to consider it individually because of the collision with binary minus
                    if (unarySymbol == OPERATOR_SUBTRACTION) {
                        return OperatorUnary.Minus(range, generateASTInternal(parseTree.children[1], diagnostics))
                    }
                    return createInstanceUnary(
                        unarySymbol.syntaxTreeClass!! as KClass<OperatorUnary>,
                        range,
                        generateASTInternal(parseTree.children[1], diagnostics),
                    )
                } else {
                    throw IllegalArgumentException("Expected the operator symbol, got: $operatorKind")
                }
            }

            FIELD_ACCESS -> {
                require(childNum >= 2) { "Field access missing rhs: $parseTree" }
                val lhs = parseTree.children[0]
                parseTree.children.slice(1..<parseTree.children.size).fold(generateASTInternal(lhs, diagnostics)) { ast, field ->
                    require(field is ParseTree.Leaf) { "Field access rhs should be a leaf: $field in $parseTree" }
                    require(
                        getGrammarSymbol(field) == VARIABLE_IDENTIFIER,
                    ) { "Field access rhs should be an identifier: $field in $parseTree" }

                    if (ast is Assignable) {
                        FieldRef.LValue(field.range, ast, field.token.context)
                    } else {
                        FieldRef.RValue(field.range, ast, field.token.context)
                    }
                }
            }

            STRUCT ->
                Struct(
                    parseTree.range,
                    parseTree.children
                        .map { createStructField(it, diagnostics) }
                        .also {
                            it.fold(mutableSetOf<String>()) { acc, (field) ->
                                if (!acc.add(field.name)) {
                                    diagnostics.report(ASTDiagnostics.DuplicateField(field.name), field.range)
                                    throw diagnostics.fatal()
                                } else acc
                            }
                        }.toMap(),
                )

            else -> throw IllegalArgumentException("Unexpected branch symbol: $symbol")
        }
    }
    return Empty(range = Pair(Location(0), Location(0)))
}

private fun wrapInFunction(originalAST: AST): AST {
    val beforeStart = Location(-1)
    val behindEnd = Location(originalAST.range.second.value + 1)
    val program =
        Definition.FunctionDeclaration(
            Pair(beforeStart, behindEnd),
            MAIN_FUNCTION_IDENTIFIER,
            BaseType.Functional(
                Pair(beforeStart, beforeStart),
                emptyList(),
                BaseType.Basic(Pair(beforeStart, beforeStart), "Unit"),
            ),
            emptyList(),
            BaseType.Basic(Pair(beforeStart, beforeStart), "Unit"),
            Block(
                Pair(Location(0), behindEnd),
                listOf(
                    originalAST,
                    Empty(Pair(behindEnd, behindEnd)),
                ),
            ),
        )
    val programCall =
        FunctionCall(
            Pair(behindEnd, behindEnd),
            VariableUse(Pair(behindEnd, behindEnd), MAIN_FUNCTION_IDENTIFIER),
            emptyList(),
        )
    return Block(
        Pair(beforeStart, behindEnd),
        listOf(
            program,
            programCall,
        ),
    )
}

fun generateAST(parseTree: ParseTree<CacophonyGrammarSymbol>, diagnostics: Diagnostics): AST {
    val prunedTree = pruneParseTree(parseTree, diagnostics)!!
    val ast = generateASTInternal(prunedTree, diagnostics)
    return wrapInFunction(ast)
}
