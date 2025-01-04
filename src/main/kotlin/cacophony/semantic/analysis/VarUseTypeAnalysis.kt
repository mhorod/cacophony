package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.semantic.analysis.UseTypesForExpression.Companion.withHeapUse
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.*
import kotlin.error

// Type of variables usage for each expression
typealias UseTypeAnalysisResult = Map<Expression, Map<Variable, VariableUseType>>

fun analyzeVarUseTypes(
    ast: AST,
    resolvedVariables: ResolvedVariables,
    functionAnalysis: FunctionAnalysisResult,
    variablesMap: VariablesMap,
): UseTypeAnalysisResult {
    val visitor = VarUseVisitor(resolvedVariables, functionAnalysis, variablesMap)
    visitor.visit(ast)
    return visitor.getAnalysisResult()
}

private class UseTypesForExpression(
    private val map: MutableMap<Variable, VariableUseType>,
) {
    fun add(variable: Variable, type: VariableUseType) {
        val previousType =
            map.getOrElse(
                variable,
            ) { VariableUseType.UNUSED }
        map[variable] = previousType.union(type)
    }

    fun getMap(): Map<Variable, VariableUseType> = map

    fun mergeWith(vararg others: UseTypesForExpression?) {
        others.forEach { other ->
            other?.getMap()?.forEach {
                this.add(it.key, it.value)
            }
        }
    }

    fun filter(collection: Collection<Variable>) {
        map.keys.removeAll(collection.toSet())
    }

    companion object {
        fun empty() = UseTypesForExpression(mutableMapOf())

        fun merge(vararg subexpressionUseTypes: UseTypesForExpression?): UseTypesForExpression {
            val result = empty()
            subexpressionUseTypes.forEach { useTypesForExpression ->
                useTypesForExpression?.getMap()?.forEach {
                    result.add(it.key, it.value)
                }
            }
            return result
        }

        infix fun UseTypesForExpression.withHeapUse(heapUseType: VariableUseType): UseTypesForExpression {
            val heapUseTypes = UseTypesForExpression(mutableMapOf(Variable.Heap to heapUseType))
            return merge(this, heapUseTypes)
        }
    }
}

private class VarUseVisitor(
    val resolvedVariables: ResolvedVariables,
    val functionAnalysis: FunctionAnalysisResult,
    val variablesMap: VariablesMap,
) {
    private val useTypeAnalysis = mutableMapOf<Expression, UseTypesForExpression>()
    private val scopeStack = ArrayDeque<MutableSet<Variable>>()

    fun visit(ast: AST) = visitExpression(ast)

    fun getAnalysisResult(): UseTypeAnalysisResult = useTypeAnalysis.mapValues { it.value.getMap() }

    private fun gatherNestedVariables(variable: Variable): Set<Variable> {
        val result = HashSet<Variable>()
        result.add(variable)
        if (variable is Variable.StructVariable) {
            variable.fields.values.forEach {
                result.addAll(gatherNestedVariables(it))
            }
        }
        return result
    }

    private fun gatherAffectedVariables(expr: Assignable): Set<Variable> {
        val result = HashSet<Variable>()
        result.addAll(gatherNestedVariables(variablesMap.lvalues[expr]!!))

        var nestedExpression: Expression = expr
        while (nestedExpression is Assignable) {
            result.add(variablesMap.lvalues[nestedExpression]!!)
            if (nestedExpression !is FieldRef) {
                break
            }
            nestedExpression = nestedExpression.struct()
        }
        return result
    }

    private fun visitExpression(expr: Expression) {
        if (expr is Definition.VariableDeclaration) {
            scopeStack.lastOrNull()?.add(variablesMap.definitions[expr]!!)
        }
        when (expr) {
            is Block -> visitBlock(expr)
            is Definition.VariableDeclaration -> visitVariableDeclaration(expr)
            is Definition.FunctionDefinition -> visitFunctionDeclaration(expr)
            is FunctionCall -> visitFunctionCall(expr)
            is Statement.IfElseStatement -> visitIfElseStatement(expr)
            is Statement.WhileStatement -> visitWhileStatement(expr)
            is Statement.ReturnStatement -> visitReturnStatement(expr)
            is OperatorUnary -> visitUnaryOperator(expr)
            is OperatorBinary -> visitBinaryOperator(expr)
            is VariableUse -> visitVariableUse(expr)
            is Struct -> visitStruct(expr)
            is FieldRef.LValue -> visitFieldRefLValue(expr)
            is FieldRef.RValue -> visitFieldRefRValue(expr)
            is Allocation -> visitAllocation(expr)
            is Dereference -> visitDereference(expr)
            is LeafExpression -> {
                useTypeAnalysis[expr] = UseTypesForExpression.empty()
            }
        }
    }

    private fun visitAssignable(expr: Assignable, type: VariableUseType) {
        // TODO: Should `Dereference` be considered Assignable?
        if (expr is Dereference) {
            visitExpression(expr.value)
            useTypeAnalysis[expr] = useTypeAnalysis[expr.value]!! withHeapUse type
            return
        }

        val affectedVariables = gatherAffectedVariables(expr)

        var nestedExpression: Expression = expr
        while (nestedExpression is Assignable) {
            useTypeAnalysis[nestedExpression] = UseTypesForExpression.empty()
            affectedVariables.forEach {
                useTypeAnalysis[nestedExpression]!!.add(
                    it,
                    type,
                )
            }
            if (nestedExpression !is FieldRef) {
                break
            }
            nestedExpression = nestedExpression.struct()
        }
    }

    private fun visitFieldRefLValue(expr: FieldRef.LValue) {
        visitAssignable(expr, VariableUseType.READ)
    }

    private fun visitFieldRefRValue(expr: FieldRef.RValue) {
        visitExpression(expr.obj)
        useTypeAnalysis[expr] = useTypeAnalysis[expr.obj] ?: error("Variable use types missing for child ${expr.obj} of $expr")
    }

    private fun visitStruct(expr: Struct) {
        useTypeAnalysis[expr] =
            UseTypesForExpression.merge(
                *expr.fields.values
                    .map {
                        visitExpression(it)
                        useTypeAnalysis[it]
                    }.toTypedArray(),
            )
    }

    private fun visitVariableUse(expr: VariableUse) {
        val definition = resolvedVariables[expr]
        if (definition is Definition.FunctionDefinition || definition is Definition.ForeignFunctionDeclaration)
            return
        useTypeAnalysis[expr] = UseTypesForExpression.empty()
        gatherNestedVariables(variablesMap.lvalues[expr]!!).forEach {
            useTypeAnalysis[expr]!!.add(
                it,
                VariableUseType.READ,
            )
        }
    }

    private fun visitBinaryOperator(expr: OperatorBinary) {
        when (expr) {
            is OperatorBinary.Assignment -> visitAssignment(expr, compound = false)
            is OperatorBinary.LValueOperator -> visitAssignment(expr, compound = true)
            else -> {
                visitExpression(expr.lhs)
                visitExpression(expr.rhs)
                useTypeAnalysis[expr] =
                    UseTypesForExpression.merge(
                        useTypeAnalysis[expr.lhs],
                        useTypeAnalysis[expr.rhs],
                    )
            }
        }
    }

    private fun visitAssignment(expr: OperatorBinary.LValueOperator, compound: Boolean) {
        useTypeAnalysis[expr] = UseTypesForExpression.empty()
        visitExpression(expr.rhs)
        val lhsUseType =
            if (compound) {
                VariableUseType.READ_WRITE
            } else {
                VariableUseType.WRITE
            }
        check(expr.lhs is Assignable) { "Invalid lhs of assignment" }
        visitAssignable(expr.lhs, lhsUseType)
        useTypeAnalysis[expr]!!.mergeWith(useTypeAnalysis[expr.lhs], useTypeAnalysis[expr.rhs])
    }

    private fun visitUnaryOperator(expr: OperatorUnary) {
        visitExpression(expr.expression)
        useTypeAnalysis[expr] =
            UseTypesForExpression.merge(
                useTypeAnalysis[expr.expression],
            )
    }

    private fun visitReturnStatement(expr: Statement.ReturnStatement) {
        visitExpression(expr.value)
        useTypeAnalysis[expr] =
            UseTypesForExpression.merge(
                useTypeAnalysis[expr.value],
            )
    }

    private fun visitWhileStatement(expr: Statement.WhileStatement) {
        visitExpression(expr.testExpression)
        visitExpression(expr.doExpression)
        useTypeAnalysis[expr] =
            UseTypesForExpression.merge(
                useTypeAnalysis[expr.testExpression],
                useTypeAnalysis[expr.doExpression],
            )
    }

    private fun visitIfElseStatement(expr: Statement.IfElseStatement) {
        visitExpression(expr.testExpression)
        visitExpression(expr.doExpression)
        expr.elseExpression?.let { visitExpression(it) }
        useTypeAnalysis[expr] =
            UseTypesForExpression.merge(
                useTypeAnalysis[expr.testExpression],
                useTypeAnalysis[expr.doExpression],
                useTypeAnalysis[expr.elseExpression],
            )
    }

    private fun visitFunctionCall(expr: FunctionCall) {
        visitExpression(expr.function)
        useTypeAnalysis[expr] =
            UseTypesForExpression.merge(
                useTypeAnalysis[expr.function],
            )
        expr.arguments.forEach {
            // arguments should be marked as read-used
            visitExpression(it)
            useTypeAnalysis[expr]!!.mergeWith(useTypeAnalysis[it]!!)
        }

        // variables used in called function:
        when (val calledFunction = resolvedVariables[expr.function]) {
            is Definition.FunctionDefinition -> {
                val map =
                    functionAnalysis[calledFunction]!!.outerVariables().associate {
                        it.origin to it.useType
                    }
                useTypeAnalysis[expr]!!.mergeWith(UseTypesForExpression(map.toMutableMap()))
            }

            is Definition.ForeignFunctionDeclaration -> useTypeAnalysis[expr]!!.add(Variable.Heap, VariableUseType.READ_WRITE)

            else -> error("Left side of function call is not a function declaration")
        }
    }

    private fun visitFunctionDeclaration(expr: Definition.FunctionDefinition) {
        // we don't want to merge with declaration body, as it need to be called to use the variables
        visitExpression(expr.body)
        useTypeAnalysis[expr] = UseTypesForExpression.empty()
    }

    private fun visitVariableDeclaration(expr: Definition.VariableDeclaration) {
        visitExpression(expr.value)
        useTypeAnalysis[expr] =
            UseTypesForExpression.merge(
                useTypeAnalysis[expr.value],
            )
    }

    private fun visitBlock(expr: Block) {
        useTypeAnalysis[expr] = UseTypesForExpression.empty()
        scopeStack.addLast(mutableSetOf())
        expr.expressions.forEach {
            visitExpression(it)
            useTypeAnalysis[expr]!!.mergeWith(useTypeAnalysis[it]!!)
        }
        useTypeAnalysis[expr]!!.filter(scopeStack.last())
        scopeStack.removeLast()
    }

    private fun visitAllocation(expr: Allocation) {
        visitExpression(expr.value)
        useTypeAnalysis[expr] = useTypeAnalysis[expr.value]!! withHeapUse VariableUseType.WRITE
    }

    private fun visitDereference(expr: Dereference) {
        visitExpression(expr.value)
        useTypeAnalysis[expr] = useTypeAnalysis[expr.value]!! withHeapUse VariableUseType.READ
    }
}
