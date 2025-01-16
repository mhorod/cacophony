package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition
import kotlin.math.min

typealias EscapeAnalysisResult = Set<Variable>

fun escapeAnalysis(
    ast: AST,
    resolvedVariables: ResolvedVariables,
    functionAnalysis: FunctionAnalysisResult,
    variablesMap: VariablesMap,
): EscapeAnalysisResult {
    val baseVisitor = BaseEscapeAnalysisVisitor(resolvedVariables, variablesMap)
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
    return usageDepth.keys.filter { usageDepth[it]!! < definitionDepth[it]!! }.toSet()
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
) {
    private var currentStaticDepth = -1
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
            is LambdaExpression -> visitFunctionBody(expr.body)
            is FunctionDefinition -> visitFunctionDeclaration(expr)
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

    private fun visitFunctionBody(expr: Expression) {
        currentStaticDepth += 1

        val lastBodyExpression = if (expr is Block) expr.expressions.lastOrNull() else expr

        if (expr is Block) {
            expr.expressions.forEach { if (it !== lastBodyExpression) visitExpression(it) }
        }

        if (lastBodyExpression != null) visitReturnedExpression(lastBodyExpression)

        currentStaticDepth -= 1
    }

    private fun visitFunctionDeclaration(expr: Definition.FunctionDeclaration) {
        when (expr) {
            is Definition.ForeignFunctionDeclaration -> return
            is FunctionDefinition -> visitFunctionBody(expr.body)
        }
    }

    private fun visitReturnedExpression(expr: Expression) {
        returnStack.add(mutableSetOf())
        visitExpression(expr)

        if (returnStack.last().isNotEmpty()) {
            returnVariables.getOrPut(currentStaticDepth) { mutableSetOf() }.addAll(returnStack.last())
        }

        returnStack.removeLast()
    }

    private fun visitAssignment(expr: OperatorBinary.Assignment) {
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
