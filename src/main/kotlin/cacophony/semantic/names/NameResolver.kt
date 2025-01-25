package cacophony.semantic.names

import cacophony.controlflow.functions.Builtin
import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.NRDiagnostics
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.syntaxtree.Definition.ForeignFunctionDeclaration
import cacophony.semantic.syntaxtree.Definition.FunctionArgument
import cacophony.semantic.syntaxtree.Definition.VariableDeclaration
import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.utils.CompileException

class NameResolutionException(
    reason: String,
) : CompileException(reason)

sealed interface ResolvedName {
    class Variable(
        val def: VariableDeclaration,
    ) : ResolvedName

    class Argument(
        val def: FunctionArgument,
    ) : ResolvedName

    // To musi zostać +- jak było
    class Function(
        val def: OverloadSet,
    ) : ResolvedName
}

private class OverloadSetImpl : OverloadSet {
    private val overloads: MutableMap<Int, Definition> = mutableMapOf()

    override fun get(arity: Int): Definition? = overloads.get(arity)

    override fun toMap(): Map<Int, Definition> = overloads

    override fun withDeclaration(arity: Int, declaration: Definition): OverloadSet {
        overloads[arity] = declaration
        return this
    }

    fun withDeclarationsFrom(overloadSet: OverloadSet): OverloadSet {
        overloadSet.toMap().forEach { (arity, declaration) -> overloads[arity] = declaration }
        return this
    }
}

private interface SymbolsTable {
    fun open()

    fun close()

    fun define(id: String, definition: Definition)

    fun find(id: String): ResolvedName?
}

private fun emptySymbolsTable(): SymbolsTable {
    val blocks: MutableList<MutableMap<String, ResolvedName>> = mutableListOf()
    val idToBlocks: MutableMap<String, MutableList<MutableMap<String, ResolvedName>>> = mutableMapOf()

    return object : SymbolsTable {
        override fun open() {
            blocks.add(mutableMapOf())
        }

        override fun close() {
            blocks.lastOrNull()?.let { block ->
                block.keys.forEach { id -> idToBlocks[id]!!.removeLast() }
                blocks.removeLast()
            }
        }

        fun defineFunction(id: String, definition: Definition, arity: Int) {
            when (blocks.last()[id]) {
                is ResolvedName.Function -> {
                    (blocks.last()[id] as ResolvedName.Function)
                        .def
                        .withDeclaration(arity, definition)
                }

                else -> {
                    blocks.last()[id] =
                        ResolvedName.Function(
                            OverloadSetImpl().withDeclaration(arity, definition),
                        )
                }
            }
        }

        override fun define(id: String, definition: Definition) {
            if (blocks.isNotEmpty()) {
                if (!idToBlocks.containsKey(id)) {
                    idToBlocks[id] = mutableListOf()
                }
                if (idToBlocks[id]!!.lastOrNull() !== blocks.last()) {
                    idToBlocks[id]!!.add(blocks.last())
                }

                when (definition) {
                    is FunctionArgument -> {
                        blocks.last()[id] = ResolvedName.Argument(definition)
                    }

                    is Definition.VariableDefinition -> {
                        blocks.last()[id] = ResolvedName.Variable(definition)
                    }

                    is Definition.FunctionDefinition -> {
                        val arity = definition.value.arguments.size
                        defineFunction(id, definition, arity)
                    }

                    is ForeignFunctionDeclaration -> {
                        val arity = definition.type.argumentsType.size
                        defineFunction(id, definition, arity)
                    }
                }
            }
        }

        override fun find(id: String): ResolvedName? =
            idToBlocks[id]?.let { blocks ->
                if (blocks.isEmpty()) return null
                if (blocks.last()[id]!! is ResolvedName.Function) {
                    // A bit convoluted logic of merging overload sets.
                    val lastNonFunction = blocks.indexOfLast { it[id] !is ResolvedName.Function }
                    val overloadSet = OverloadSetImpl()
                    for (i in lastNonFunction + 1 until blocks.size) {
                        overloadSet.withDeclarationsFrom((blocks[i][id]!! as ResolvedName.Function).def)
                    }
                    return ResolvedName.Function(overloadSet)
                } else {
                    return blocks.last()[id]!!
                }
            }
    }
}

typealias NameResolutionResult = Map<VariableUse, ResolvedName>

fun resolveNames(root: AST, diagnostics: Diagnostics): NameResolutionResult {
    val resolution = mutableMapOf<VariableUse, ResolvedName>()
    val symbolsTable = emptySymbolsTable()

    fun traverseAst(node: Expression, openNewBlock: Boolean) {
        fun visitLambdaExpression(arguments: List<FunctionArgument>, body: Expression) {
            arguments
                .groupBy { it.identifier }
                .filter { it.value.size > 1 }
                .values
                .flatten()
                .onEach { argNode ->
                    diagnostics.report(NRDiagnostics.DuplicatedFunctionArgument(argNode.identifier), argNode.range)
                }.let { if (it.isNotEmpty()) throw diagnostics.fatal() }

            // Open new block to make arguments visible in function body, but not after
            // the whole function declaration.
            symbolsTable.open()
            arguments.forEach { traverseAst(it, false) }
            traverseAst(body, true)
            symbolsTable.close()
        }

        if (openNewBlock) symbolsTable.open()

        when (node) {
            is Block -> {
                node.expressions.forEach { traverseAst(it, it is Block) }
            }

            is VariableUse -> {
                symbolsTable
                    .find(node.identifier)
                    ?.let { resolvedName -> resolution[node] = resolvedName }
                    ?: diagnostics.report(NRDiagnostics.UnidentifiedIdentifier(node.identifier), node.range)
            }

            is VariableDeclaration -> {
                traverseAst(node.value, true)
                symbolsTable.define(node.identifier, node)
            }

            is LambdaExpression -> {
                visitLambdaExpression(node.arguments, node.body)
            }

            is ForeignFunctionDeclaration -> {
                symbolsTable.define(node.identifier, node)
                // TODO: Unfortunately, we do not use alloc_func as VariableUse anywhere, but we need it in the preamble
                if (node === Builtin.allocStruct) {
                    resolution[VariableUse(node.range, node.identifier)] = symbolsTable.find(node.identifier)!!
                }
            }

            is FunctionArgument -> {
                if (node.type is BaseType.Functional) {
                    diagnostics.report(NRDiagnostics.IllegalFunctionalArgument(node.identifier), node.range)
                }
                symbolsTable.define(node.identifier, node)
            }

            is FunctionCall -> {
                traverseAst(node.function, false)
                node.arguments.forEach { traverseAst(it, true) }
            }

            is Statement.IfElseStatement -> {
                traverseAst(node.testExpression, true)
                traverseAst(node.doExpression, true)
                node.elseExpression?.let { traverseAst(it, true) }
            }

            is Statement.WhileStatement -> {
                traverseAst(node.testExpression, true)
                traverseAst(node.doExpression, true)
            }

            is Statement.ReturnStatement -> {
                traverseAst(node.value, true)
            }

            is OperatorUnary -> {
                traverseAst(node.expression, true)
            }

            is OperatorBinary -> {
                traverseAst(node.lhs, true)
                traverseAst(node.rhs, true)
            }

            is Struct -> {
                node.fields.values.forEach { traverseAst(it, true) }
            }

            is FieldRef -> {
                traverseAst(node.struct(), false)
            }

            is Allocation -> traverseAst(node.value, true)

            is Dereference -> traverseAst(node.value, true)

            is LeafExpression -> {}
        }

        if (openNewBlock) symbolsTable.close()
    }

    traverseAst(root, true)
    return resolution
}
