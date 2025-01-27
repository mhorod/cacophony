package cacophony.semantic.names

import cacophony.controlflow.functions.Builtin
import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.NRDiagnostics
import cacophony.semantic.names.Shape.Companion.isSubshapeOf
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.syntaxtree.Definition.ForeignFunctionDeclaration
import cacophony.semantic.syntaxtree.Definition.FunctionArgument
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition
import cacophony.semantic.syntaxtree.Definition.VariableDeclaration

typealias EntityResolutionResult = Map<VariableUse, ResolvedEntity>
typealias ArityResolutionResult = Map<Definition, Int>

class NameResolutionResult(val entityResolution: EntityResolutionResult, val shapeResolution: ArityResolutionResult)

fun resolveNames(root: AST, diagnostics: Diagnostics): NameResolutionResult {
    val resolver = NameResolver(diagnostics)
    resolver.traverseAst(root, true)
    val arities =
        resolver
            .defsToShapes
            .mapNotNull { (definition, shape) -> if (shape is Shape.Functional) definition to shape.arity else null }
            .toMap()
    return NameResolutionResult(resolver.resolvedEntities, arities)
}

private class NameResolver(val diagnostics: Diagnostics) {
    val resolvedEntities = mutableMapOf<VariableUse, ResolvedEntity>()
    val defsToShapes = mutableMapOf<Definition, Shape>()

    private val symbolsTable = SymbolsTable()

    fun traverseAst(node: Expression, openNewBlock: Boolean): Set<Shape> {
        if (openNewBlock) symbolsTable.open()

        val shapes =
            when (node) {
                is Block -> traverseBlock(node)
                is VariableUse -> traverseVariableUse(node)
                is FunctionDefinition -> traverseFunctionDefinition(node)
                is Definition.VariableDefinition -> traverseVariableDefinition(node)
                is LambdaExpression -> traverseLambdaExpression(node)
                is ForeignFunctionDeclaration -> traverseForeignFunctionDeclaration(node)
                is FunctionArgument -> traverseFunctionArgument(node)
                is FunctionCall -> traverseFunctionCall(node)
                is Statement.IfElseStatement -> traverseIfElseStatement(node)
                is Statement.WhileStatement -> traverseWhileStatement(node)
                is Statement.ReturnStatement -> traverseReturnStatement(node)
                is OperatorUnary -> traverseOperatorUnary(node)
                is OperatorBinary -> traverseOperatorBinary(node)
                is Struct -> traverseStruct(node)
                is FieldRef -> traverseFieldRef(node)
                is Dereference -> traverseDereference(node)
                is Allocation -> traverseAllocation(node)
                is LeafExpression -> setOf(Shape.Atomic)
            }

        if (openNewBlock) symbolsTable.close()

        return shapes
    }

    private fun traverseBlock(block: Block): Set<Shape> =
        block.expressions
            .map { traverseAst(it, it is Block) }
            .lastOrNull()
            ?: setOf(Shape.Atomic)

    private fun traverseVariableUse(use: VariableUse): Set<Shape> {
        val entity = symbolsTable.find(use.identifier)
        if (entity == null) {
            diagnostics.report(NRDiagnostics.UnidentifiedIdentifier(use.identifier), use.range)
            return setOf(Shape.Top)
        }
        resolvedEntities[use] = entity
        return when (entity) {
            is ResolvedEntity.Unambiguous -> setOf(Shape.Top)
            is ResolvedEntity.WithOverloads ->
                entity.overloads
                    .toMap()
                    .map { (_, definition) -> defsToShapes[definition]!! }
                    .toSet()
        }
    }

    private fun traverseVariableDefinition(definition: Definition.VariableDefinition): Set<Shape> {
        val decidedShape = decideSingleShape(definition.type, definition.value)
        symbolsTable.define(definition.identifier, definition)
        defsToShapes[definition] = decidedShape
        return setOf(Shape.Atomic)
    }

    private fun traverseFunctionDefinition(definition: FunctionDefinition): Set<Shape> {
        symbolsTable.define(definition.identifier, definition)
        defsToShapes[definition] = Shape.Functional(definition.value.arguments.size, Shape.from(definition.value.returnType))
        visitLambdaExpression(definition.value.arguments, definition.value.returnType, definition.value.body)
        return setOf(Shape.Atomic)
    }

    private fun traverseLambdaExpression(lambda: LambdaExpression): Set<Shape> =
        setOf(visitLambdaExpression(lambda.arguments, lambda.returnType, lambda.body))

    private fun traverseForeignFunctionDeclaration(declaration: ForeignFunctionDeclaration): Set<Shape> {
        symbolsTable.define(declaration.identifier, declaration)
        val type = declaration.type
        defsToShapes[declaration] = Shape.Functional(type.argumentsType.size, Shape.from(type.returnType))
        // TODO: Unfortunately, we do not use alloc_func as VariableUse anywhere, but we need it in the preamble
        if (declaration === Builtin.allocStruct) {
            resolvedEntities[VariableUse(declaration.range, declaration.identifier)] = symbolsTable.find(declaration.identifier)!!
        }
        return setOf(Shape.Atomic)
    }

    private fun traverseFunctionArgument(argument: FunctionArgument): Set<Shape> {
        symbolsTable.define(argument.identifier, argument)
        return emptySet()
    }

    private fun traverseFunctionCall(call: FunctionCall): Set<Shape> {
        val functionShapes = traverseAst(call.function, false)
        call.arguments.forEach { traverseAst(it, true) }
        val chosen = functionShapes.filterIsInstance<Shape.Functional>().filter { it.arity == call.arguments.size }
        check(chosen.size <= 1) { "At most one function with a given arity should be visible at any given point" }
        if (chosen.size == 1)
            return setOf(chosen.last().result)
        return setOf(Shape.Top)
    }

    private fun traverseIfElseStatement(statement: Statement.IfElseStatement): Set<Shape> {
        traverseAst(statement.testExpression, true)
        val doBranchShapes = traverseAst(statement.doExpression, true)
        val elseBranchShapes = statement.elseExpression?.let { traverseAst(it, true) } ?: emptySet()
        return doBranchShapes intersect elseBranchShapes
    }

    private fun traverseWhileStatement(statement: Statement.WhileStatement): Set<Shape> {
        traverseAst(statement.testExpression, true)
        traverseAst(statement.doExpression, true)
        return setOf(Shape.Atomic)
    }

    private fun traverseReturnStatement(statement: Statement.ReturnStatement): Set<Shape> {
        traverseAst(statement.value, true)
        return setOf(Shape.Atomic)
    }

    private fun traverseOperatorUnary(operator: OperatorUnary): Set<Shape> {
        traverseAst(operator.expression, true)
        return setOf(Shape.Atomic)
    }

    private fun traverseOperatorBinary(operator: OperatorBinary): Set<Shape> {
        traverseAst(operator.lhs, true)
        traverseAst(operator.rhs, true)
        return setOf(Shape.Atomic)
    }

    private fun traverseStruct(struct: Struct): Set<Shape> {
        val fieldMap =
            struct
                .fields
                .map { (field, value) -> field.name to decideSingleShape(field.type, value) }
                .toMap()
        return setOf(Shape.Structural(fieldMap))
    }

    private fun traverseFieldRef(ref: FieldRef): Set<Shape> =
        traverseAst(ref.struct(), false)
            .filterIsInstance<Shape.Structural>()
            .mapNotNull { it.fields[ref.field] }
            .toSet()

    private fun traverseAllocation(allocation: Allocation): Set<Shape> =
        traverseAst(allocation.value, true)
            .map { Shape.Referential(it) }
            .toSet()

    private fun traverseDereference(dereference: Dereference): Set<Shape> =
        traverseAst(dereference.value, true)
            .filterIsInstance<Shape.Referential>()
            .map { it.internal }
            .toSet()

    private fun visitLambdaExpression(arguments: List<FunctionArgument>, returnType: Type, body: Expression): Shape {
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
        return Shape.Functional(arguments.size, Shape.from(returnType))
    }

    private fun decideSingleShape(annotation: Type?, value: Expression): Shape {
        val expectedShape = annotation?.let { Shape.from(it) } ?: Shape.Top
        val validShapes = traverseAst(value, true).filter { it isSubshapeOf expectedShape }
        val decidedShape =
            if (validShapes.size == 1) {
                val shape = validShapes.last()
                if (annotation == null) shape else expectedShape
            } else {
                expectedShape
            }
        return decidedShape
    }
}

typealias MutableOverloadSet = MutableMap<Int, Definition>

private class MutableWithOverloads : ResolvedEntity.WithOverloads {
    val mutableOverloads: MutableOverloadSet = mutableMapOf()

    override val overloads: OverloadSet
        get() = mutableOverloads
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

    fun define(id: String, definition: Definition) {
        if (blocks.isEmpty())
            return
        if (!idToBlocks.containsKey(id)) {
            idToBlocks[id] = mutableListOf()
        }
        if (idToBlocks[id]!!.lastOrNull() !== blocks.last()) {
            idToBlocks[id]!!.add(blocks.last())
        }

        when (definition) {
            is ForeignFunctionDeclaration -> {
                val declaredArity = definition.type.argumentsType.size
                defineOverload(id, declaredArity, definition)
            }
            is FunctionDefinition -> {
                val declaredArity = definition.value.arguments.size
                defineOverload(id, declaredArity, definition)
            }
            is VariableDeclaration -> {
                blocks.last()[id] = ResolvedEntity.Unambiguous(definition)
            }
            is FunctionArgument -> {
                if (definition.type is BaseType.Functional) {
                    defineOverload(id, definition.type.argumentsType.size, definition)
                } else {
                    blocks.last()[id] = ResolvedEntity.Unambiguous(definition)
                }
            }
        }
    }

    fun find(id: String): ResolvedEntity? =
        idToBlocks[id]?.let { blocks ->
            if (blocks.isEmpty()) return null
            val closestDefs = blocks.last()[id]!!
            if (closestDefs is ResolvedEntity.WithOverloads) {
                val entity = MutableWithOverloads()
                for (block in blocks.reversed()) {
                    when (val defs = block[id]) {
                        is ResolvedEntity.WithOverloads ->
                            defs.overloads.forEach { (arity, definition) ->
                                entity.mutableOverloads.putIfAbsent(arity, definition)
                            }
                        else -> break
                    }
                }
                return entity
            } else {
                return blocks.last()[id]!!
            }
        }

    private fun defineOverload(id: String, arity: Int, definition: Definition) {
        val entity =
            when (val presentEntity = blocks.last()[id]) {
                is MutableWithOverloads -> presentEntity
                else -> {
                    val withOverloads = MutableWithOverloads()
                    blocks.last()[id] = withOverloads
                    withOverloads
                }
            }
        entity.mutableOverloads[arity] = definition
    }
}
