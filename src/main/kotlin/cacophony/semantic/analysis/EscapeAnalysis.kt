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

typealias EscapeAnalysisResult = Set<Variable>

class EscapeAnalysisException(reason: String) : Exception(reason)

private fun canEscapeViaExpressionOfType(type: TypeExpr?): Boolean =
    when (type) {
        is FunctionType -> true
        is StructType -> type.fields.values.any { canEscapeViaExpressionOfType(it) }
        else -> false
    }

private sealed class FunctionalEntity {
    // NOTE: Variables of structural types with functional fields are treated like
    //   functional variables in the Escape Analysis algorithm. In other words, FunctionalVariables
    //   are all variables whose type satisfies the canEscapeViaExpressionOfType(*) predicate.
    data class FunctionalVariable(val variable: Variable) : FunctionalEntity()

    data class Lambda(val lambdaExpression: LambdaExpression) : FunctionalEntity()

    companion object {
        fun from(variable: Variable) = FunctionalVariable(variable)

        fun from(lambdaExpression: LambdaExpression) = Lambda(lambdaExpression)
    }
}

fun escapeAnalysis(
    ast: AST,
    resolvedVariables: ResolvedVariables,
    functionAnalysis: FunctionAnalysisResult,
    variablesMap: VariablesMap,
    types: TypeCheckingResult,
): EscapeAnalysisResult {
    val baseVisitor = BaseEscapeAnalysisVisitor(resolvedVariables, variablesMap, types)
    baseVisitor.visit(ast)
    val baseResult = baseVisitor.getResult()

    // Set definitionDepth of all Functional Entities of static depth of lambda expressions
    // defining them, or -1 if they are defined globally.
    val definitionDepth: Map<FunctionalEntity, Int> =
        baseResult.allFunctionalEntities
            .associateWith {
                baseResult.definingLambda[it]?.let { parentLambda -> functionAnalysis[parentLambda]!!.staticDepth }
                    ?: -1
            }
    val usageDepth = definitionDepth.toMutableMap()

    // Auxiliary maps of Lambda Expressions and Functional Variables to their corresponding Functional Entities
    val lambdaToFunctionalEntity: Map<LambdaExpression, FunctionalEntity> =
        baseResult
            .allFunctionalEntities
            .filterIsInstance<FunctionalEntity.Lambda>()
            .associateBy { it.lambdaExpression }
    val variableToFunctionalEntity: Map<Variable, FunctionalEntity> =
        baseResult
            .allFunctionalEntities
            .filterIsInstance<FunctionalEntity.FunctionalVariable>()
            .associateBy { it.variable }

    // Update usageDepth for Functional Entities in return statements.
    baseResult.returned.forEach { (fromLambda, returnedEntities) ->
        returnedEntities.forEach {
            usageDepth[it] = min(usageDepth[it]!!, functionAnalysis[fromLambda]!!.staticDepth - 1)
        }
    }

    // Iterate updating usageDepth using functional assignments and relation of variables
    // used by functions until fixed point is obtained
    var fixedPointObtained = false

    while (!fixedPointObtained) {
        fixedPointObtained = true

        baseResult.assignedFunctionalEntities.forEach { (lhs, rhs) ->
            val minOfLhsDepths = lhs.minOf { usageDepth[it]!! }
            rhs.forEach {
                if (usageDepth[it]!! > minOfLhsDepths) {
                    fixedPointObtained = false
                    usageDepth[it] = minOfLhsDepths
                }
            }
        }

        functionAnalysis.forEach { (lambda, analyzedFunction) ->
            val lambdaUsageDepth = usageDepth[lambdaToFunctionalEntity[lambda]!!]!!
            analyzedFunction.variables
                .filterNot { analyzedFunction.declaredVariables().contains(it) }
                .map { it.origin }
                .filter { variableToFunctionalEntity.containsKey(it) }
                .forEach {
                    val functionalEntity = variableToFunctionalEntity[it]!!
                    if (lambdaUsageDepth < usageDepth[functionalEntity]!!) {
                        fixedPointObtained = false
                        usageDepth[functionalEntity] = lambdaUsageDepth
                    }
                }
        }
    }

    // The escaping Functional Entities are those with usageDepth < definitionDepth
    val escapingFunctionalEntities =
        baseResult.allFunctionalEntities
            .filter { usageDepth[it]!! < definitionDepth[it]!! }

    // We mark all variables used by escaping lambdas as escaping
    val escapingVariables = (escapingFunctionalEntities
        .filterIsInstance<FunctionalEntity.FunctionalVariable>()
        .map { it.variable }.toSet() union
        escapingFunctionalEntities
            .filterIsInstance<FunctionalEntity.Lambda>()
            .map { functionAnalysis[it.lambdaExpression]!! }
            .flatMap {
                it.variables.minus(it.declaredVariables().toSet())
            }
            .map { it.origin }).toMutableSet()


    // At the end, we enclose escapingVariables by relation of variable assignments
    fixedPointObtained = false

    while (!fixedPointObtained) {
        fixedPointObtained = true

        baseResult.assignedVariables.forEach { (lhs, rhs) ->
            // If any variable in LHS escapes, we deduce that all variables in rhs escape too.
            if (lhs.any { escapingVariables.contains(it) }) {
                if (rhs.any { !escapingVariables.contains(it) }) {
                    fixedPointObtained = false
                    escapingVariables.addAll(rhs)
                }
            }
        }
    }

    return escapingVariables.toSet()
}

/**
 * @property allFunctionalEntities set of all Functional Entities found in the AST
 * @property definingLambda map of Functional Entities to LambdaExpression in which they are defined
 * @property assignedFunctionalEntities list pairs of sets consisting of Functional Entities used in assignments (in the LHS and RHS)
 * @property assignedVariables same as assignedFunctionalEntities, but containing variables (of not necessarily functional types)
 * @property returned map LambdaExpression -> Set of Functional Entities occurring in return expression in it
 */
private data class BaseEscapeAnalysisResult(
    val allFunctionalEntities: Set<FunctionalEntity>,
    val definingLambda: Map<FunctionalEntity, LambdaExpression>,
    val assignedFunctionalEntities: List<Pair<Set<FunctionalEntity.FunctionalVariable>, Set<FunctionalEntity>>>,
    val assignedVariables: List<Pair<Set<Variable>, Set<Variable>>>,
    val returned: Map<LambdaExpression, Set<FunctionalEntity>>,
)

private class BaseEscapeAnalysisVisitor(
    val resolvedVariables: ResolvedVariables,
    val variablesMap: VariablesMap,
    val types: TypeCheckingResult,
) {
    private var lambdaExpressionsStack = ArrayDeque<LambdaExpression>()
    private var functionTypeStack = ArrayDeque<FunctionType>()
    private val assignmentFunctionalLhsStack = ArrayDeque<MutableSet<FunctionalEntity.FunctionalVariable>>()
    private val assignmentFunctionalRhsStack = ArrayDeque<MutableSet<FunctionalEntity>>()
    private val assignmentVariableLhsStack = ArrayDeque<MutableSet<Variable>>()
    private val assignmentVariableRhsStack = ArrayDeque<MutableSet<Variable>>()
    private val returnStack = ArrayDeque<MutableSet<FunctionalEntity>>()

    private val allFunctionalEntities = mutableSetOf<FunctionalEntity>()
    private val definingLambda = mutableMapOf<FunctionalEntity, LambdaExpression>()
    private val assignedFunctionalEntities = mutableListOf<Pair<Set<FunctionalEntity.FunctionalVariable>, Set<FunctionalEntity>>>()
    private val assignedVariables = mutableListOf<Pair<Set<Variable>, Set<Variable>>>()
    private val returned = mutableMapOf<LambdaExpression, MutableSet<FunctionalEntity>>()

    fun visit(ast: AST) {
        visitExpression(ast)
    }

    fun getResult(): BaseEscapeAnalysisResult =
        BaseEscapeAnalysisResult(
            allFunctionalEntities,
            definingLambda,
            assignedFunctionalEntities,
            assignedVariables,
            returned,
        )

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
            is OperatorBinary -> {
                visitExpression(expr.rhs)
                visitExpression(expr.lhs)
            }
            is OperatorUnary -> visitExpression(expr.expression)
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
        val type = types.definitionTypes[definition]

        if (canEscapeViaExpressionOfType(type) && variable != null) {
            // Add Variable to all currently open LHS/RHS of functional assignments and return statements
            assignmentFunctionalLhsStack.forEach { it.add(FunctionalEntity.from(variable)) }
            assignmentFunctionalRhsStack.forEach { it.add(FunctionalEntity.from(variable)) }
            returnStack.forEach { it.add(FunctionalEntity.from(variable)) }
        }

        if (variable != null) {
            // Add Variable to all currently open LHS/RHS of variable assignments
            assignmentVariableLhsStack.forEach { it.add(variable) }
            assignmentVariableRhsStack.forEach { it.add(variable) }
        }
    }

    private fun visitFunctionBody(expr: Expression) {
        val lastBodyExpression = if (expr is Block) expr.expressions.lastOrNull() else expr

        if (expr is Block) {
            expr.expressions.forEach { if (it !== lastBodyExpression) visitExpression(it) }
        }

        if (lastBodyExpression != null) visitReturnedExpression(lastBodyExpression)
    }

    private fun visitReturnedExpression(expr: Expression) {
        returnStack.add(mutableSetOf())
        visitExpression(expr)

        if (lambdaExpressionsStack.isNotEmpty() &&
            returnStack.last().isNotEmpty() &&
            canEscapeViaExpressionOfType(functionTypeStack.last().result)
        ) {
            returned.getOrPut(lambdaExpressionsStack.last()) { mutableSetOf() }.addAll(returnStack.last())
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

        assignmentFunctionalRhsStack.add(mutableSetOf())
        assignmentVariableRhsStack.add(mutableSetOf())
        visitExpression(expr.rhs)
        val functionalRhs = assignmentFunctionalRhsStack.last().toSet()
        val variableRhs = assignmentVariableRhsStack.last().toSet()
        assignmentFunctionalRhsStack.removeLast()
        assignmentVariableRhsStack.removeLast()

        assignmentFunctionalLhsStack.add(mutableSetOf())
        assignmentVariableLhsStack.add(mutableSetOf())
        visitExpression(expr.lhs)
        val functionalLhs = assignmentFunctionalLhsStack.last().toSet()
        val variableLhs = assignmentVariableLhsStack.last().toSet()
        assignmentFunctionalLhsStack.removeLast()
        assignmentVariableLhsStack.removeLast()

        if (functionalLhs.isNotEmpty() && functionalRhs.isNotEmpty()) {
            assignedFunctionalEntities.add(functionalLhs to functionalRhs)
        }
        if (variableLhs.isNotEmpty() && variableRhs.isNotEmpty()) {
            assignedVariables.add(variableLhs to variableRhs)
        }
    }

    private fun visitLambdaExpression(expr: LambdaExpression) {
        val functionalEntity = FunctionalEntity.from(expr)
        allFunctionalEntities.add(functionalEntity)

        if (lambdaExpressionsStack.isNotEmpty()) {
            definingLambda[functionalEntity] = lambdaExpressionsStack.last()
        }

        val lambdaType = types.expressionTypes[expr]
        if (lambdaType == null || lambdaType !is FunctionType) {
            throw EscapeAnalysisException("Unexpected type $lambdaType of lambda expression $expr")
        }

        // Add LambdaExpression to all currently open RSH of functional assignments and return statements.
        assignmentFunctionalRhsStack.forEach { it.add(functionalEntity) }
        returnStack.forEach { it.add(functionalEntity) }

        lambdaExpressionsStack.add(expr)
        functionTypeStack.add(types.expressionTypes[expr] as FunctionType)

        visitFunctionBody(expr.body)

        functionTypeStack.removeLast()
        lambdaExpressionsStack.removeLast()
    }

    private fun visitVariableDeclaration(expr: Definition.VariableDeclaration) {
        val variable = variablesMap.definitions[expr]!!
        val variableType =
            types.definitionTypes[expr]
                ?: throw EscapeAnalysisException("Missing type of variable declaration: $expr")

        var functionalEntity: FunctionalEntity.FunctionalVariable? = null;
        if (canEscapeViaExpressionOfType(variableType)) {
            functionalEntity = FunctionalEntity.from(variable)
            allFunctionalEntities.add(functionalEntity)
            if (lambdaExpressionsStack.isNotEmpty()) {
                definingLambda[functionalEntity] = lambdaExpressionsStack.last()
            }
        }

        // We treat a definition with value like an assignment with only one variable in LHS
        assignmentFunctionalRhsStack.add(mutableSetOf())
        assignmentVariableRhsStack.add(mutableSetOf())

        visitExpression(expr.value)

        val functionalRhs = assignmentFunctionalRhsStack.last().toSet()
        val variableRhs = assignmentVariableRhsStack.last().toSet()
        assignmentFunctionalRhsStack.removeLast()
        assignmentVariableRhsStack.removeLast()

        val functionalLhs = if (functionalEntity != null) setOf(functionalEntity) else emptySet()
        val variableLhs = setOf(variable)

        if (functionalLhs.isNotEmpty() && functionalRhs.isNotEmpty()) {
            assignedFunctionalEntities.add(functionalLhs to functionalRhs)
        }
        if (variableRhs.isNotEmpty()) {
            assignedVariables.add(variableLhs to variableRhs)
        }
    }
}
