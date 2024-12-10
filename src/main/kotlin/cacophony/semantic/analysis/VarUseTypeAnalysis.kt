package cacophony.semantic.analysis

import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.*
import kotlin.error

// Type of variables usage for each expression
typealias UseTypeAnalysisResult = Map<Expression, Map<Definition, VariableUseType>>

fun analyzeVarUseTypes(ast: AST, resolvedVariables: ResolvedVariables, functionAnalysis: FunctionAnalysisResult): UseTypeAnalysisResult {
    val visitor = VarUseVisitor(resolvedVariables, functionAnalysis)
    visitor.visit(ast)
    return visitor.getAnalysisResult()
}

private class UseTypesForExpression(
    private val map: MutableMap<Definition, VariableUseType>,
) {
    fun add(definition: Definition, type: VariableUseType) {
        val previousType =
            map.getOrElse(
                definition,
            ) { VariableUseType.UNUSED }
        map[definition] = previousType.union(type)
    }

    fun getMap(): Map<Definition, VariableUseType> = map

    fun mergeWith(other: UseTypesForExpression) {
        other.getMap().forEach {
            this.add(it.key, it.value)
        }
    }

    fun filter(collection: Collection<Definition>) {
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
    }
}

private class VarUseVisitor(
    val resolvedVariables: ResolvedVariables,
    val functionAnalysis: FunctionAnalysisResult,
) {
    private val useTypeAnalysis = mutableMapOf<Expression, UseTypesForExpression>()
    private val scopeStack = ArrayDeque<MutableSet<Definition>>()

    fun visit(ast: AST) = visitExpression(ast)

    fun getAnalysisResult(): UseTypeAnalysisResult = useTypeAnalysis.mapValues { it.value.getMap() }

    private fun visitExpression(expr: Expression) {
        if (expr is Definition) {
            scopeStack.lastOrNull()?.add(expr)
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
            is FieldRef -> visitFieldRef(expr)
            is LeafExpression -> {
                useTypeAnalysis[expr] = UseTypesForExpression.empty()
            }
        }
    }

    private fun visitFieldRef(expr: FieldRef) {
        expr.struct().let {
            visitExpression(it)
            useTypeAnalysis[expr] = useTypeAnalysis[it] ?: error("Variable use types missing for child $it of $expr")
        }
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
        useTypeAnalysis[expr] = UseTypesForExpression.empty()
        useTypeAnalysis[expr]!!.add(
            resolvedVariables[expr]!!,
            VariableUseType.READ,
        )
    }

    private fun visitBinaryOperator(expr: OperatorBinary) {
        when (expr) {
            is OperatorBinary.Assignment -> visitAssignment(expr)
            is OperatorBinary.AdditionAssignment,
            is OperatorBinary.SubtractionAssignment,
            is OperatorBinary.MultiplicationAssignment,
            is OperatorBinary.DivisionAssignment,
            is OperatorBinary.ModuloAssignment,
            -> visitCompoundAssignment(expr)

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

    private fun visitCompoundAssignment(expr: OperatorBinary) {
        visitExpression(expr.rhs)
        when (expr.lhs) {
            is VariableUse -> {
                useTypeAnalysis[expr] = UseTypesForExpression.empty()
                useTypeAnalysis[expr]!!.add(
                    resolvedVariables[expr.lhs]!!,
                    VariableUseType.READ_WRITE,
                )
                useTypeAnalysis[expr]!!.mergeWith(useTypeAnalysis[expr.rhs]!!)
            }

            else -> visitExpression(expr.lhs)
        }
    }

    private fun visitAssignment(expr: OperatorBinary.Assignment) {
        visitExpression(expr.rhs)
        when (expr.lhs) {
            is VariableUse -> {
                useTypeAnalysis[expr] = UseTypesForExpression.empty()
                useTypeAnalysis[expr]!!.add(
                    resolvedVariables[expr.lhs]!!,
                    VariableUseType.WRITE,
                )
                useTypeAnalysis[expr]!!.mergeWith(useTypeAnalysis[expr.rhs]!!)
            }

            else -> visitExpression(expr.lhs)
        }
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
        val calledFunction = resolvedVariables[expr.function]
        if (calledFunction is Definition.FunctionDefinition) {
            val map =
                functionAnalysis[calledFunction]!!.outerVariables().associate {
                    it.declaration to it.useType
                }
            useTypeAnalysis[expr]!!.mergeWith(UseTypesForExpression(map.toMutableMap()))
        } else if (calledFunction !is Definition.ForeignFunctionDeclaration) {
            error("Left side of function call is not a function declaration")
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
}
