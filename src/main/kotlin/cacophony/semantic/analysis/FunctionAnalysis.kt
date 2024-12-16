package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.graphs.getProperTransitiveClosure
import cacophony.graphs.reverseGraph
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition

typealias FunctionAnalysisResult = Map<Definition.FunctionDefinition, AnalyzedFunction>

data class ParentLink(
    val parent: Definition.FunctionDefinition,
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
    val function: Definition.FunctionDefinition,
    val parentLink: ParentLink?,
    val variables: Set<AnalyzedVariable>,
    val auxVariables: MutableSet<Variable>,
    val staticDepth: Int,
    val variablesUsedInNestedFunctions: Set<Variable>,
) {
    fun declaredVariables() = variables.filter { it.definedIn == function }

    fun outerVariables() = variables.filter { it.definedIn != function }
}

data class AnalyzedVariable(
    val origin: Variable,
    val definedIn: Definition.FunctionDefinition,
    val useType: VariableUseType,
)

fun analyzeFunctions(
    ast: AST,
    resolvedVariables: ResolvedVariables,
    callGraph: CallGraph,
    variablesMap: VariablesMap,
): FunctionAnalysisResult {
    val relations = findStaticFunctionRelations(ast)
    val variableFunctions = getVariableFunctions(relations, variablesMap)
    val parentGraph =
        relations.mapValues { (_, staticRelations) ->
            staticRelations.parent?.let { setOf(it) } ?: emptySet()
        }
    val callGraphClosedRelations =
        staticFunctionRelationsClosure(
            relations,
            callGraph,
        )
    // we need information if children declaration uses outside variables because of static links
    val childrenGraphClosedRelations =
        staticFunctionRelationsClosure(
            callGraphClosedRelations,
            reverseGraph(parentGraph),
        )
    val analyzedVariables =
        analyzedVariables(
            childrenGraphClosedRelations,
            resolvedVariables,
            variableFunctions,
            variablesMap,
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

fun variablesUsedInNestedFunctions(
    analyzedVariables: Map<Definition.FunctionDefinition, Set<AnalyzedVariable>>,
): Map<Definition.FunctionDefinition, Set<Variable>> {
    val result = mutableMapOf<Definition.FunctionDefinition, MutableSet<Variable>>()
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
    function: Definition.FunctionDefinition,
    staticRelations: StaticFunctionRelations,
    analyzedVariables: Map<Definition.FunctionDefinition, Set<AnalyzedVariable>>,
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
        mutableSetOf(),
        staticRelations.staticDepth,
        variablesUsedInNestedFunctions,
    )
}

fun makeAnalyzedVariable(
    usedVariable: UsedVariable,
    resolvedVariables: ResolvedVariables,
    variableFunctions: Map<Variable, Definition.FunctionDefinition>,
    variablesMap: VariablesMap,
): AnalyzedVariable {
    val definition =
        resolvedVariables[usedVariable.variable] ?: throw IllegalStateException("Variable not resolved")

    val definedIn =
        when (definition) {
            is Definition.VariableDeclaration -> variableFunctions[variablesMap.definitions[definition]]
            is Definition.FunctionArgument -> variableFunctions[variablesMap.definitions[definition]]
            else -> error("Variable declaration not found in any function")
        } ?: throw IllegalStateException("Variable $definition not defined in any function")

    return AnalyzedVariable(
        variablesMap.definitions[definition]!!,
        definedIn,
        usedVariable.type,
    )
}

private fun getVariableFunctions(
    relations: StaticFunctionRelationsMap,
    variablesMap: VariablesMap,
): Map<Variable, Definition.FunctionDefinition> =
    relations
        .flatMap { (function, _) ->
            function.arguments.map { argument ->
                variablesMap.definitions[argument]!! to function
            }
        }.toMap() +
        relations
            .flatMap { (function, staticRelations) ->
                staticRelations.declaredVariables.map { variableDefinition ->
                    variablesMap.definitions[variableDefinition]!! to function
                }
            }.toMap()

private fun analyzedVariables(
    relations: StaticFunctionRelationsMap,
    resolvedVariables: ResolvedVariables,
    variableFunctions: Map<Variable, Definition.FunctionDefinition>,
    variablesMap: VariablesMap,
) = relations.mapValues { (function, _) ->
    getAnalyzedVariables(
        function,
        relations,
        resolvedVariables,
        variableFunctions,
        variablesMap,
    )
}

private fun getAnalyzedVariables(
    function: Definition.FunctionDefinition,
    relations: StaticFunctionRelationsMap,
    resolvedVariables: ResolvedVariables,
    variableFunctions: Map<Variable, Definition.FunctionDefinition>,
    variablesMap: VariablesMap,
): Set<AnalyzedVariable> =
    relations[function]!!
        .usedVariables
        .asSequence()
        .filter {
            resolvedVariables[it.variable] is Definition.VariableDeclaration ||
                resolvedVariables[it.variable] is Definition.FunctionArgument
        }.map { makeAnalyzedVariable(it, resolvedVariables, variableFunctions, variablesMap) }
        .toSet()
        .union(
            relations[function]!!.declaredVariables.map {
                AnalyzedVariable(variablesMap.definitions[it]!!, variableFunctions[variablesMap.definitions[it]]!!, VariableUseType.UNUSED)
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
    graph: Map<Definition.FunctionDefinition, Set<Definition.FunctionDefinition>>,
): StaticFunctionRelationsMap {
    val closure = getProperTransitiveClosure(graph)
    val newMap = staticFunctionRelations.toMutableMap()
    staticFunctionRelations.forEach {
        val newUsedVariables = it.value.usedVariables.toMutableSet()
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
