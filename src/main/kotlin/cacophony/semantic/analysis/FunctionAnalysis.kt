package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.graphs.getProperTransitiveClosure
import cacophony.graphs.reverseGraph
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.LambdaExpression

typealias FunctionAnalysisResult = Map<LambdaExpression, AnalyzedFunction>

data class ParentLink(
    val parent: LambdaExpression,
    val used: Boolean,
)

/**
 * Analyzed function properties
 *
 * @property function Function definition
 * @property parentLink Link to wrapping function
 * @property variables All variables read, written, or declared in the function
 * @property auxVariables All auxiliary variables created in the function
 * @property staticDepth Depth of the function - how deeply nested it is
 * @property variablesUsedInNestedFunctions Variables declared in this function that are used in nested functions
 */

data class AnalyzedFunction(
    val function: LambdaExpression,
    val parentLink: ParentLink?,
    val variables: Set<AnalyzedVariable>,
    // TODO: probably need to change, but we needed it for prologueHandler to compile
    val arguments: List<Definition.FunctionArgument>,
    val auxVariables: MutableSet<Variable>,
    val staticDepth: Int,
    val variablesUsedInNestedFunctions: Set<Variable>,
) {
    fun declaredVariables() = variables.filter { it.definedIn == function }

    fun outerVariables() = variables.filter { it.definedIn != function }
}

data class AnalyzedVariable(
    val origin: Variable,
    val definedIn: LambdaExpression, // TODO: change to FunctionalExpression
    val useType: VariableUseType,
)

// TODO: Adjust this code so it works for all LambdaExpressions. We're only interested in the static analysis of the body
//  so there should be no distinction between named and anonymous functions.
fun analyzeFunctions(ast: AST, resolvedVariables: ResolvedVariables, variablesMap: VariablesMap): FunctionAnalysisResult {
    val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)
    val variableFunctions = getVariableFunctions(relations, variablesMap)
    val parentGraph =
        relations.mapValues { (_, staticRelations) ->
            staticRelations.parent?.let { setOf(it) } ?: emptySet()
        }
    // we need information if children declaration uses outside variables because of static links
    val childrenGraphClosedRelations =
        staticFunctionRelationsClosure(
            relations,
            reverseGraph(parentGraph),
        )
    val analyzedVariables =
        analyzedVariables(
            childrenGraphClosedRelations,
            variableFunctions,
        )
    val variablesUsedInNestedFunctions = variablesUsedInNestedFunctions(analyzedVariables)

    return relations.mapValues { (function, staticRelations) ->
        makeAnalyzedFunction(
            function,
            staticRelations,
            analyzedVariables,
            variablesUsedInNestedFunctions[function] ?: emptySet(),
        )
    }
}

fun variablesUsedInNestedFunctions(analyzedVariables: Map<LambdaExpression, Set<AnalyzedVariable>>): Map<LambdaExpression, Set<Variable>> {
    val result = mutableMapOf<LambdaExpression, MutableSet<Variable>>()
    analyzedVariables.forEach { (function, variables) ->
        variables.forEach { variable ->
            if (variable.definedIn != function) {
                result.getOrPut(variable.definedIn) { mutableSetOf() }.add(variable.origin)
            }
        }
    }
    return result
}

fun makeAnalyzedFunction(
    function: LambdaExpression,
    staticRelations: StaticFunctionRelations,
    analyzedVariables: Map<LambdaExpression, Set<AnalyzedVariable>>,
    variablesUsedInNestedFunctions: Set<Variable>,
): AnalyzedFunction {
    val variables =
        analyzedVariables[function]
            ?: throw IllegalStateException("Analyzed function is missing variable information")
    val parentLink = staticRelations.parent?.let { ParentLink(it, variables.any { it.definedIn != function }) }
    return AnalyzedFunction(
        function,
        parentLink,
        variables,
        function.arguments,
        mutableSetOf(),
        staticRelations.staticDepth,
        variablesUsedInNestedFunctions,
    )
}

fun makeAnalyzedVariable(usedVariable: UsedVariable, variableFunctions: Map<Variable, LambdaExpression>): AnalyzedVariable {
    val variable = usedVariable.variable
    val definedIn = variableFunctions[variable] ?: throw IllegalStateException("Variable $variable not defined in any function")

    return AnalyzedVariable(
        usedVariable.variable,
        definedIn,
        usedVariable.type,
    )
}

private fun getAllNestedVariables(v: Variable): List<Variable> =
    when (v) {
        is Variable.StructVariable -> {
            listOf(v) + v.fields.values.flatMap { getAllNestedVariables(it) }
        }

        is Variable.PrimitiveVariable -> listOf(v)
        is Variable.Heap -> listOf(v)
    }

private fun getVariableFunctions(relations: StaticFunctionRelationsMap, variablesMap: VariablesMap): Map<Variable, LambdaExpression> =
    relations
        .flatMap { (function, _) ->
            function.arguments.flatMap { argument ->
                getAllNestedVariables(variablesMap.definitions[argument]!!).map { it to function }
            }
        }.toMap() +
        relations
            .flatMap { (function, staticRelations) ->
                staticRelations.declaredVariables.map { variable ->
                    variable to function
                }
            }.toMap()

private fun analyzedVariables(relations: StaticFunctionRelationsMap, variableFunctions: Map<Variable, LambdaExpression>) =
    relations.mapValues { (function, _) ->
        getAnalyzedVariables(
            function,
            relations,
            variableFunctions,
        )
    }

private fun getAnalyzedVariables(
    function: LambdaExpression,
    relations: StaticFunctionRelationsMap,
    variableFunctions: Map<Variable, LambdaExpression>,
): Set<AnalyzedVariable> =
    relations[function]!!
        .usedVariables
        .asSequence()
        .map { makeAnalyzedVariable(it, variableFunctions) }
        .toSet()
        .union(
            relations[function]!!.declaredVariables.map {
                AnalyzedVariable(it, variableFunctions[it]!!, VariableUseType.UNUSED)
            },
        ).filter {
            relations[it.definedIn]!!.staticDepth < relations[function]!!.staticDepth ||
                it.definedIn == function
        }.groupBy { it.origin }
        .map {
            val useType = variableUseType(it.value.map { variable -> variable.useType })
            AnalyzedVariable(it.key, it.value.first().definedIn, useType)
        }.toSet()

private fun variableUseType(useTypes: List<VariableUseType>) =
    if (useTypes.isEmpty()) {
        VariableUseType.UNUSED
    } else {
        useTypes.reduce { acc, next -> acc.union(next) }
    }

private fun staticFunctionRelationsClosure(
    staticFunctionRelations: StaticFunctionRelationsMap,
    graph: Map<LambdaExpression, Set<LambdaExpression>>,
): StaticFunctionRelationsMap {
    val closure = getProperTransitiveClosure(graph)
    val newMap = staticFunctionRelations.toMutableMap()
    staticFunctionRelations.forEach {
        val newUsedVariables = it.value.usedVariables.toMutableSet()
        // f => (g[]; h[];...; a;b;c;)
        // f uses {a, b, c} + used[g] + used[h]
        closure[it.key]?.forEach { calledFunction ->
            newUsedVariables.addAll(staticFunctionRelations[calledFunction]!!.usedVariables)
        }
        newMap[it.key] =
            StaticFunctionRelations(
                it.value.parent,
                it.value.staticDepth,
                it.value.declaredVariables,
                newUsedVariables,
            )
    }
    return newMap
}
