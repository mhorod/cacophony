package cacophony.semantic.names

import cacophony.controlflow.functions.Builtin
import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.NRDiagnostics
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.syntaxtree.Definition.ForeignFunctionDeclaration
import cacophony.semantic.syntaxtree.Definition.FunctionArgument
import cacophony.semantic.syntaxtree.Definition.FunctionDeclaration
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition
import cacophony.semantic.syntaxtree.Definition.VariableDeclaration

sealed interface ResolvedEntity {
    class Unambiguous(val definition: Definition) : ResolvedEntity

    class WithOverloads(val overloads: OverloadSet) : ResolvedEntity
}

private fun toArities(entity: ResolvedEntity): Set<Int> = when (entity) {
    is ResolvedEntity.Unambiguous -> emptySet()
    is ResolvedEntity.WithOverloads -> entity.overloads.toMap().keys
}

private class OverloadSetImpl : OverloadSet {
    private val overloads: MutableMap<Int, Definition> = mutableMapOf()

    override fun get(arity: Int): Definition? = overloads[arity]

    override fun toMap(): Map<Int, Definition> = overloads

    override fun addDeclaration(arity: Int, declaration: Definition): OverloadSet {
        overloads[arity] = declaration
        return this
    }

    fun addDeclarationsFrom(overloadSet: OverloadSet): OverloadSet {
        overloadSet.toMap().forEach { (arity, declaration) -> overloads[arity] = declaration }
        return this
    }
}

private class SymbolsTable {
    val blocks: MutableList<MutableMap<String, ResolvedEntity>> = mutableListOf()
    val idToBlocks: MutableMap<String, MutableList<MutableMap<String, ResolvedEntity>>> = mutableMapOf()

    fun open() {
        blocks.add(mutableMapOf())
    }

    fun close() {
        blocks.lastOrNull()?.let { block ->
            block.keys.forEach { id -> idToBlocks[id]!!.removeLast() }
            blocks.removeLast()
        }
    }

    fun define(id: String, definition: Definition, arity: Int? = null): Set<Int> {
        if (blocks.isNotEmpty()) {
            if (!idToBlocks.containsKey(id)) {
                idToBlocks[id] = mutableListOf()
            }
            if (idToBlocks[id]!!.lastOrNull() !== blocks.last()) {
                idToBlocks[id]!!.add(blocks.last())
            }

            when (definition) {
                is VariableDeclaration -> {
                    blocks.last()[id] = arity?.let {
                        ResolvedEntity.WithOverloads(OverloadSetImpl().addDeclaration(it, definition))
                    } ?: ResolvedEntity.Unambiguous(definition)
                }

                is FunctionArgument -> {
                    blocks.last()[id] = if (definition.type is BaseType.Functional) {
                        ResolvedEntity.WithOverloads(
                            OverloadSetImpl().addDeclaration(
                                definition.type.argumentsType.size,
                                definition
                            )
                        )
                    } else {
                        ResolvedEntity.Unambiguous(definition)
                    }
                }

                is FunctionDeclaration -> {
                    val declaredArity =
                        when (definition) {
                            is FunctionDefinition -> definition.arguments.size
                            is ForeignFunctionDeclaration ->
                                (definition.type ?: error("foreign function without a type")).argumentsType.size
                        }
                    when (val closestDefs = blocks.last()[id]) {
                        is ResolvedEntity.WithOverloads -> {
                            closestDefs
                                .overloads
                                .addDeclaration(declaredArity, definition)
                        }

                        else -> {
                            blocks.last()[id] =
                                ResolvedEntity.WithOverloads(
                                    OverloadSetImpl().addDeclaration(declaredArity, definition)
                                )
                        }
                    }
                }
            }

            return toArities(blocks.last()[id]!!)
        }
        return emptySet()
    }

    fun find(id: String): ResolvedEntity? =
        idToBlocks[id]?.let { blocks ->
            if (blocks.isEmpty()) return null
            val closestDefs = blocks.last()[id]!!
            if (closestDefs is ResolvedEntity.WithOverloads) {
                val overloadSet = OverloadSetImpl()
                for (block in blocks.reversed()) {
                    when (val defs = block[id]) {
                        is ResolvedEntity.WithOverloads -> overloadSet.addDeclarationsFrom(defs.overloads)
                        else -> break
                    }
                }
                return ResolvedEntity.WithOverloads(overloadSet)
            } else {
                return blocks.last()[id]!!
            }
        }
}

typealias NameResolutionResult = Map<VariableUse, ResolvedEntity>

fun resolveNames(root: AST, diagnostics: Diagnostics): NameResolutionResult {
    val resolution = mutableMapOf<VariableUse, ResolvedEntity>()
    val symbolsTable = SymbolsTable()

    fun traverseAst(node: Expression, openNewBlock: Boolean): Set<Int> {
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

        val arities: Set<Int> = when (node) {
            is Block -> {
                node.expressions.map { traverseAst(it, it is Block) }.lastOrNull() ?: emptySet()
            }

            is VariableUse -> {
                symbolsTable
                    .find(node.identifier)
                    ?.let { entity ->
                        resolution[node] = entity
                        toArities(entity)
                    }
                    ?: run {
                        diagnostics.report(NRDiagnostics.UnidentifiedIdentifier(node.identifier), node.range)
                        emptySet()
                    }
            }

            is VariableDeclaration -> {
                val arities = traverseAst(node.value, true)

                symbolsTable.define(node.identifier, node)
            }

            is FunctionDefinition -> {
                symbolsTable.define(node.identifier, node)
                visitLambdaExpression(node.arguments, node.body)
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
