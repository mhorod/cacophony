package cacophony.semantic

import cacophony.semantic.syntaxtree.*

typealias UseTypeAnalysisResult = Map<Expression, Map<Definition, VariableUseType>>

fun analyzeVarUseTypes(
    ast: AST,
    resolvedVariables: ResolvedVariables,
    callGraph: CallGraph,
): UseTypeAnalysisResult {
    val visitor = VarUseVisitor(resolvedVariables)
    visitor.visit(ast)
    return visitor.getAnalysisResult()
}

private class UseTypesForExpression(
    private val map: MutableMap<Definition, VariableUseType>,
) {
    fun add(
        definition: Definition,
        type: VariableUseType,
    ) {
        val previousType =
            map.getOrElse(
                definition,
                { VariableUseType.UNUSED },
            )
        map.put(definition, previousType.union(type))
    }

    fun getMap(): Map<Definition, VariableUseType> = map

    fun mergeWith(other: UseTypesForExpression) {
        other.getMap().forEach {
            this.add(it.key, it.value)
        }
    }

    companion object {
        fun empty() = UseTypesForExpression(mutableMapOf())

        fun merge(vararg subexpressionUseTypes: UseTypesForExpression?): UseTypesForExpression {
            val result = empty()
            subexpressionUseTypes.forEach {
                it?.getMap()?.forEach {
                    result.add(it.key, it.value)
                }
            }
            return result
        }
    }
}

private class VarUseVisitor(
    val resolvedVariables: ResolvedVariables,
) {
    private val useTypeAnalysis = mutableMapOf<Expression, UseTypesForExpression>()

    fun visit(ast: AST) = visitExpression(ast)

    fun getAnalysisResult(): UseTypeAnalysisResult = useTypeAnalysis.mapValues { it.value.getMap() }

    private fun visitExpression(expr: Expression) {
        when (expr) {
            is Block -> visitBlock(expr)
            is Definition.VariableDeclaration -> visitVariableDeclaration(expr)
            is Definition.FunctionDeclaration -> visitFunctionDeclaration(expr)
            is FunctionCall -> visitFunctionCall(expr)
            is Statement.IfElseStatement -> visitIfElseStatement(expr)
            is Statement.WhileStatement -> visitWhileStatement(expr)
            is Statement.ReturnStatement -> visitReturnStatement(expr)
            is OperatorUnary -> visitUnaryOperator(expr)
            is OperatorBinary -> visitBinaryOperator(expr)
            is VariableUse -> visitVariableUse(expr)
            else -> {
                // do nothing for expressions without nested expressions
            }
        }
    }

    private fun visitVariableUse(expr: VariableUse) {
        useTypeAnalysis.put(expr, UseTypesForExpression.empty())
        useTypeAnalysis.get(expr)!!.add(
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
                useTypeAnalysis.put(
                    expr,
                    UseTypesForExpression.merge(
                        useTypeAnalysis.get(expr.lhs),
                        useTypeAnalysis.get(expr.rhs),
                    ),
                )
            }
        }
    }

    private fun visitCompoundAssignment(expr: OperatorBinary) {
        visitExpression(expr.rhs)
        when (expr.lhs) {
            is VariableUse -> {
                useTypeAnalysis.put(expr, UseTypesForExpression.empty())
                useTypeAnalysis.get(expr)!!.add(
                    resolvedVariables[expr.lhs]!!,
                    VariableUseType.READ_WRITE,
                )
                useTypeAnalysis.get(expr)!!.mergeWith(useTypeAnalysis.get(expr.rhs)!!)
            }

            else -> visitExpression(expr.lhs)
        }
    }

    private fun visitAssignment(expr: OperatorBinary.Assignment) {
        visitExpression(expr.rhs)
        when (expr.lhs) {
            is VariableUse -> {
                useTypeAnalysis.put(expr, UseTypesForExpression.empty())
                useTypeAnalysis.get(expr)!!.add(
                    resolvedVariables[expr.lhs]!!,
                    VariableUseType.WRITE,
                )
                useTypeAnalysis.get(expr)!!.mergeWith(useTypeAnalysis.get(expr.rhs)!!)
            }

            else -> visitExpression(expr.lhs)
        }
    }

    private fun visitUnaryOperator(expr: OperatorUnary) {
        visitExpression(expr.expression)
        useTypeAnalysis.put(
            expr,
            UseTypesForExpression.merge(
                useTypeAnalysis.get(expr.expression),
            ),
        )
    }

    private fun visitReturnStatement(expr: Statement.ReturnStatement) {
        visitExpression(expr.value)
        useTypeAnalysis.put(
            expr,
            UseTypesForExpression.merge(
                useTypeAnalysis.get(expr.value),
            ),
        )
    }

    private fun visitWhileStatement(expr: Statement.WhileStatement) {
        visitExpression(expr.testExpression)
        visitExpression(expr.doExpression)
        useTypeAnalysis.put(
            expr,
            UseTypesForExpression.merge(
                useTypeAnalysis.get(expr.testExpression),
                useTypeAnalysis.get(expr.doExpression),
            ),
        )
    }

    private fun visitIfElseStatement(expr: Statement.IfElseStatement) {
        visitExpression(expr.testExpression)
        visitExpression(expr.doExpression)
        expr.elseExpression?.let { visitExpression(it) }
        useTypeAnalysis.put(
            expr,
            UseTypesForExpression.merge(
                useTypeAnalysis.get(expr.testExpression),
                useTypeAnalysis.get(expr.doExpression),
                useTypeAnalysis.get(expr.elseExpression),
            ),
        )
    }

    private fun visitFunctionCall(expr: FunctionCall) {
        visitExpression(expr.function)
        useTypeAnalysis.put(
            expr,
            UseTypesForExpression.merge(
                useTypeAnalysis.get(expr.function),
            ),
        )
        expr.arguments.forEach {
            visitExpression(it)
            useTypeAnalysis.get(expr)!!.mergeWith(useTypeAnalysis.get(it)!!)
        }
    }

    private fun visitFunctionDeclaration(expr: Definition.FunctionDeclaration) {
        visitExpression(expr.body)
        useTypeAnalysis.put(
            expr,
            UseTypesForExpression.merge(
                useTypeAnalysis.get(expr.body),
            ),
        )
    }

    private fun visitVariableDeclaration(expr: Definition.VariableDeclaration) {
        visitExpression(expr.value)
        useTypeAnalysis.put(
            expr,
            UseTypesForExpression.merge(
                useTypeAnalysis.get(expr.value),
            ),
        )
    }

    private fun visitBlock(expr: Block) {
        useTypeAnalysis.put(
            expr,
            UseTypesForExpression.empty(),
        )
        expr.expressions.forEach {
            visitExpression(it)
            useTypeAnalysis.get(expr)!!.mergeWith(useTypeAnalysis.get(it)!!)
        }
    }
}
