package cacophony.semantic.syntaxtree

import cacophony.controlflow.functions.Builtin
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
        REFERENCE_TYPE -> {
            require(parseTree is ParseTree.Branch) { "Unable to construct referential type from leaf node $symbol" }
            val innerType = constructType(parseTree.children.last(), diagnostics)
            BaseType.Referential(parseTree.range, innerType)
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

fun <T : Expression> createInstanceUnary(kClass: KClass<T>, range: Pair<Location, Location>, subExpression: Expression): Expression {
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
        if (operatorKind is ParseTree.Leaf ||
            // cheat code to distinguish logical and from double reference
            (operatorKind is ParseTree.Branch && operatorKind.production.lhs == OPERATOR_LOGICAL_AND)
        ) {
            val symbol = getGrammarSymbol(operatorKind)
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

private fun callAndFieldRegexToAst(children: List<ParseTree<CacophonyGrammarSymbol>>, diagnostics: Diagnostics): Expression {
    val childNum = children.size
    if (childNum == 1) {
        return generateASTInternal(children[0], diagnostics)
    }
    val newChildren = children.subList(0, childNum - 1)
    val range = Pair(first = children[0].range.first, second = children[childNum - 1].range.second)
    val lhs = callAndFieldRegexToAst(newChildren, diagnostics)
    val lastChild = children[childNum - 1]
    return if (lastChild is ParseTree.Leaf && lastChild.token.category == VARIABLE_IDENTIFIER) { // field reference
        if (lhs is Assignable) {
            FieldRef.LValue(range, lhs, lastChild.token.context)
        } else {
            FieldRef.RValue(range, lhs, lastChild.token.context)
        }
    } else if (lastChild is ParseTree.Branch && lastChild.production.lhs == FUNCTION_CALL) {
        assert(lastChild.production.lhs == FUNCTION_CALL)
        val arguments = lastChild.children.map { generateASTInternal(it, diagnostics) }
        FunctionCall(range, lhs, arguments)
    } else throw IllegalArgumentException("Expected either field access or function call symbol, got: $lastChild")
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
            // DECLARATION_LEVEL is in the pruned graph iff it corresponds to a declaration
            DECLARATION_LEVEL -> {
                val identifier = parseTree.children[0] as ParseTree.Leaf
                val declaration = parseTree.children[1] as ParseTree.Branch
                when (getGrammarSymbol(declaration)) {
                    DECLARATION_TYPED, DECLARATION_UNTYPED -> {
                        val declarationPosition: Int
                        var type: Type? = null
                        if (getGrammarSymbol(declaration) == DECLARATION_TYPED) {
                            type = constructType(declaration.children[0], diagnostics)
                            declarationPosition = 2
                        } else {
                            declarationPosition = 1
                        }
                        var declarationKind = declaration.children[declarationPosition]
                        if (getGrammarSymbol(declarationKind) == LAMBDA_EXPRESSION) {
                            declarationKind = declarationKind as ParseTree.Branch
                            val branchesNum = declarationKind.children.size
                            val returnType = declarationKind.children[branchesNum - 2]
                            val body = declarationKind.children[branchesNum - 1]
                            var arguments: List<Definition.FunctionArgument> = listOf()
                            if (branchesNum >= 3) {
                                val unparsedArguments = declarationKind.children.subList(0, branchesNum - 2)
                                arguments =
                                    unparsedArguments.map { constructFunctionArgument(it, diagnostics) } // non-empty function argument list
                            }
                            return Definition.FunctionDefinition(
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
                                type as BaseType?,
                                generateASTInternal(declaration.children.last(), diagnostics),
                            )
                        }
                    }

                    FOREIGN_DECLARATION -> {
                        val type = constructType(declaration.children[0], diagnostics)
                        if (type !is BaseType.Functional) {
                            diagnostics.report(ASTDiagnostics.NonFunctionalForeign, range)
                            throw diagnostics.fatal()
                        }
                        return Definition.ForeignFunctionDeclaration(
                            range,
                            identifier.token.context,
                            type,
                            type.returnType,
                        )
                    }

                    else -> throw IllegalArgumentException("Expected declaration with type")
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

            LAMBDA_EXPRESSION -> {
                val branchesNum = parseTree.children.size
                val returnType = parseTree.children[branchesNum - 2]
                val body = parseTree.children[branchesNum - 1]
                var arguments: List<Definition.FunctionArgument> = listOf()
                if (branchesNum >= 3) {
                    val unparsedArguments = parseTree.children.subList(0, branchesNum - 2)
                    arguments =
                        unparsedArguments.map { constructFunctionArgument(it, diagnostics) } // non-empty function argument list
                }
                LambdaExpression(
                    range,
                    arguments,
                    constructType(returnType, diagnostics),
                    generateASTInternal(body, diagnostics),
                )
            }

            RETURN_STATEMENT -> {
                assert(childNum == 1)
                val expression = generateASTInternal(parseTree.children[0], diagnostics)
                Statement.ReturnStatement(range, expression)
            }

            ASSIGNMENT_LEVEL -> {
                assert(childNum == 3)
                val operatorKind = parseTree.children[1]
                require(operatorKind is ParseTree.Leaf) { "Expected the operator symbol, got: $operatorKind" }

                val lhs = generateASTInternal(parseTree.children[0], diagnostics)
                if (lhs !is Assignable) {
                    diagnostics.report(ASTDiagnostics.ValueNotAssignable, range)
                    throw diagnostics.fatal()
                }

                val rhs = generateASTInternal(parseTree.children[2], diagnostics)
                createInstanceBinary(operatorKind.token.category.syntaxTreeClass!! as KClass<OperatorBinary>, range, lhs, rhs)
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
                        unarySymbol.syntaxTreeClass!! as KClass<out Expression>,
                        range,
                        generateASTInternal(parseTree.children[1], diagnostics),
                    )
                } else {
                    throw IllegalArgumentException("Expected the operator symbol, got: $operatorKind")
                }
            }
            DEREFERENCE_LEVEL, ALLOCATION_LEVEL -> { // very similar to unary operators
                assert(childNum == 2)
                val operatorKind = parseTree.children[0]
                if (operatorKind is ParseTree.Leaf) {
                    val unarySymbol = operatorKind.token.category
                    return createInstanceUnary(
                        unarySymbol.syntaxTreeClass!! as KClass<out Expression>,
                        range,
                        generateASTInternal(parseTree.children[1], diagnostics),
                    )
                } else {
                    throw IllegalArgumentException("Expected the operator symbol, got: $operatorKind")
                }
            }
            // call level is in the pruned tree iff there is an actual function call or field access underneath
            CALL_LEVEL -> {
                assert(childNum >= 2)
                callAndFieldRegexToAst(parseTree.children, diagnostics)
            }
            /* ATOM_LEVEL -> { we don't expect atom level for now

            } */

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
        Definition.FunctionDefinition(
            Pair(beforeStart, behindEnd),
            MAIN_FUNCTION_IDENTIFIER,
            BaseType.Functional(
                Pair(beforeStart, beforeStart),
                emptyList(),
                BaseType.Basic(Pair(beforeStart, beforeStart), "Int"),
            ),
            emptyList(),
            BaseType.Basic(Pair(beforeStart, beforeStart), "Int"),
            Block(
                Pair(Location(0), behindEnd),
                listOf(
                    originalAST,
                    Statement.ReturnStatement(Pair(behindEnd, behindEnd), Literal.IntLiteral(Pair(behindEnd, behindEnd), 0)),
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
        Builtin.all +
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
