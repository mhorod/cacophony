package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.semantic.types.FunctionType
import cacophony.semantic.types.StructType
import cacophony.semantic.types.TypeCheckingResult
import cacophony.semantic.types.TypeExpr
import kotlin.math.min

data class EscapeAnalysisResult(
    val escapedVariables: Set<Variable>,
    val closureCallables: Set<LambdaExpression>,
    val staticLinkCallables: Set<LambdaExpression>,
)

class EscapeAnalysisException(reason: String) : Exception(reason)

fun escapeAnalysis(
    ast: AST,
    resolvedVariables: ResolvedVariables,
    functionAnalysis: FunctionAnalysisResult,
    variablesMap: VariablesMap,
    types: TypeCheckingResult,
    namedFunctionInfo: NamedFunctionInfo,
): EscapeAnalysisResult {
    val baseVisitor = BaseEscapeAnalysisVisitor(resolvedVariables, variablesMap, types)
    baseVisitor.visit(ast)
    val baseResult = baseVisitor.getResult()

    val usageDepth = mutableMapOf<Variable, Int>()
    val definitionDepth = mutableMapOf<Variable, Int>()

    // Find all escaping closures, these are exactly variables of function type with usageDepth smaller than definitionDepth.
    val variableToDefinition = variablesMap.definitions.map { (variable, def) -> def to variable }.toMap()

    // Initialize usageDepth and definitionDepth for each non-global variable to static depth of function declaring it.
    functionAnalysis.forEach { (_, analysis) ->
        analysis.declaredVariables().forEach {
            definitionDepth[it.origin] = analysis.staticDepth
            usageDepth[it.origin] = analysis.staticDepth
        }
    }

    // Initialize usageDepth and definitionDepth for global variables.
    variableToDefinition.keys
        .filter { !usageDepth.containsKey(it) }
        .forEach {
            usageDepth[it] = -1
            definitionDepth[it] = -1
        }

    // Update usageDepth for variables in return statements.
    baseResult.returnVariables.forEach { (returnDepth, returnedVariables) ->
        returnedVariables.forEach {
            usageDepth[it] = min(usageDepth[it]!!, returnDepth - 1)
        }
    }

    // Iterate updating usageDepth using assignments and relation of variables used by functions until fixed point is obtained.
    var fixedPointObtained = false

    while (!fixedPointObtained) {
        fixedPointObtained = true

        baseResult.assignmentVariables.forEach { (lhs, rhs) ->
            val minOfLhsDepths = lhs.minOf { usageDepth[it]!! }
            rhs.forEach {
                if (usageDepth[it]!! > minOfLhsDepths) {
                    fixedPointObtained = false
                    usageDepth[it] = minOfLhsDepths
                }
            }
        }

        functionAnalysis
            .filter { (function, _) -> namedFunctionInfo.containsKey(function) }
            .mapKeys { (function, _) -> namedFunctionInfo[function]!! }
            .filter { (function, _) -> variablesMap.definitions.containsKey(function) }
            .forEach { (function, analysis) ->
                analysis.variables
                    .filter { !analysis.declaredVariables().contains(it) }
                    .forEach {
                        val functionUsageDepth = usageDepth[variablesMap.definitions[function]!!]!!
                        if (usageDepth[it.origin]!! > functionUsageDepth) {
                            fixedPointObtained = false
                            usageDepth[it.origin] = functionUsageDepth
                        }
                    }
            }
    }

    // The result are all variables with usageDepth smaller than definitionDepth
    return EscapeAnalysisResult(
        usageDepth.keys.filter { usageDepth[it]!! < definitionDepth[it]!! }.toSet(),
        emptySet(), // TODO
        emptySet(),
    )
}

/**
 * @property assignmentVariables list pairs of sets (L, R) consisting of variables used in assignments
 * @property returnVariables map of functions to sets of variables used in return statements
 */
private data class BaseEscapeAnalysisResult(
    val assignmentVariables: List<Pair<Set<Variable>, Set<Variable>>>,
    val returnVariables: Map<Int, Set<Variable>>,
)

private class BaseEscapeAnalysisVisitor(
    val resolvedVariables: ResolvedVariables,
    val variablesMap: VariablesMap,
    val types: TypeCheckingResult,
) {
    private var currentStaticDepth = -1
    private var functionTypeStack = ArrayDeque<FunctionType>()
    private val assignmentLhsStack = ArrayDeque<MutableSet<Variable>>()
    private val assignmentRhsStack = ArrayDeque<MutableSet<Variable>>()
    private val returnStack = ArrayDeque<MutableSet<Variable>>()

    private val assignmentVariables = mutableListOf<Pair<Set<Variable>, Set<Variable>>>()
    private val returnVariables = mutableMapOf<Int, MutableSet<Variable>>()

    fun visit(ast: AST) {
        currentStaticDepth = -1
        visitExpression(ast)
    }

    fun getResult(): BaseEscapeAnalysisResult = BaseEscapeAnalysisResult(assignmentVariables, returnVariables)

    private fun visitExpression(expr: Expression?) {
        if (expr == null) return

        when (expr) {
            is Block -> expr.expressions.forEach { visitExpression(it) }
            is FieldRef.LValue -> visitExpression(expr.obj)
            is FieldRef.RValue -> visitExpression(expr.obj)
            is Definition.VariableDeclaration -> visitVariableDeclaration(expr)
            is LambdaExpression -> visitLambdaExpression(expr)
            is FunctionCall -> {
                visitExpression(expr.function)
                expr.arguments.forEach { visitExpression(it) }
            }
            is Statement.IfElseStatement -> {
                visitExpression(expr.testExpression)
                visitExpression(expr.doExpression)
                visitExpression(expr.elseExpression)
            }
            is Statement.WhileStatement -> {
                visitExpression(expr.testExpression)
                visitExpression(expr.doExpression)
            }
            is Statement.ReturnStatement -> visitReturnedExpression(expr.value)
            is OperatorBinary.Assignment -> visitAssignment(expr)
            is OperatorUnary -> visitExpression(expr.expression)
            is OperatorBinary -> {
                visitExpression(expr.rhs)
                visitExpression(expr.lhs)
            }
            is VariableUse -> visitVariableUse(expr)
            is Struct -> expr.fields.values.forEach { visit(it) }
            is Allocation -> visitExpression(expr.value)
            is Dereference -> visitExpression(expr.value)
            is LeafExpression -> {
                // do nothing
            }
        }
    }

    private fun visitVariableUse(expr: VariableUse) {
        val definition = resolvedVariables[expr]!!

        val variable = variablesMap.definitions[definition]

        if (variable != null) {
            // Add Variable to all currently open LHS/RSH of assignments and return statements.
            assignmentLhsStack.forEach { it.add(variable) }
            assignmentRhsStack.forEach { it.add(variable) }
            returnStack.forEach { it.add(variable) }
        }
    }

    private fun visitFunctionBody(expr: Expression, functionType: FunctionType) {
        currentStaticDepth += 1
        functionTypeStack.add(functionType)

        val lastBodyExpression = if (expr is Block) expr.expressions.lastOrNull() else expr

        if (expr is Block) {
            expr.expressions.forEach { if (it !== lastBodyExpression) visitExpression(it) }
        }

        if (lastBodyExpression != null) visitReturnedExpression(lastBodyExpression)

        functionTypeStack.removeLast()
        currentStaticDepth -= 1
    }

    private fun visitLambdaExpression(expr: LambdaExpression) {
        val lambdaType = types.expressionTypes[expr]
        if (lambdaType == null || lambdaType !is FunctionType) {
            throw EscapeAnalysisException("Unexpected type $lambdaType of lambda expression $expr")
        }
        visitFunctionBody(expr.body, types.expressionTypes[expr] as FunctionType)
    }

    private fun canEscapeViaExpressionOfType(type: TypeExpr?): Boolean =
        when (type) {
            is FunctionType -> true
            is StructType -> type.fields.values.any { canEscapeViaExpressionOfType(it) }
            else -> false
        }

    private fun visitReturnedExpression(expr: Expression) {
        returnStack.add(mutableSetOf())
        visitExpression(expr)

        if (returnStack.last().isNotEmpty() && canEscapeViaExpressionOfType(functionTypeStack.last().result)) {
            returnVariables.getOrPut(currentStaticDepth) { mutableSetOf() }.addAll(returnStack.last())
        }

        returnStack.removeLast()
    }

    private fun visitAssignment(expr: OperatorBinary.Assignment) {
        val rhsType =
            types.expressionTypes[expr.rhs]
                ?: throw EscapeAnalysisException("Missing type of rhs of assignment expression: $expr")

        if (!canEscapeViaExpressionOfType(rhsType)) {
            visitExpression(expr.rhs)
            visitExpression(expr.lhs)
            return
        }

        assignmentRhsStack.add(mutableSetOf())
        visitExpression(expr.rhs)

        val rhs = assignmentRhsStack.last().toSet()
        assignmentRhsStack.removeLast()

        assignmentLhsStack.add(mutableSetOf())
        visitExpression(expr.lhs)

        val lhs = assignmentLhsStack.last().toSet()
        assignmentLhsStack.removeLast()

        if (rhs.isNotEmpty() && lhs.isNotEmpty()) {
            assignmentVariables.add(lhs to rhs)
        }
    }

    // We treat variable declaration with value similarly to assignments.
    private fun visitVariableDeclaration(expr: Definition.VariableDeclaration) {
        val variableType =
            types.definitionTypes[expr]
                ?: throw EscapeAnalysisException("Missing type of variable declaration: $expr")

        if (!canEscapeViaExpressionOfType(variableType)) {
            visitExpression(expr.value)
            return
        }

        assignmentRhsStack.add(mutableSetOf())
        visitExpression(expr.value)

        val rhs = assignmentRhsStack.last().toSet()
        assignmentRhsStack.removeLast()

        val lhs = setOf(variablesMap.definitions[expr]!!)

        if (rhs.isNotEmpty()) {
            assignmentVariables.add(lhs to rhs)
        }
    }
}
