package cacophony.semantic

import cacophony.controlflow.Variable
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.utils.getProperTransitiveClosure
import cacophony.utils.reverseGraph

typealias FunctionAnalysisResult = Map<Definition.FunctionDeclaration, AnalyzedFunction>

data class ParentLink(
    val parent: Definition.FunctionDeclaration,
    val used: Boolean,
)

data class AnalyzedFunction(
    val parentLink: ParentLink?,
    val variables: Set<AnalyzedVariable>,
    val auxVariables: MutableSet<Variable.AuxVariable>,
    val staticDepth: Int,
    val variablesUsedInNestedFunctions: Set<Definition>,
)

data class AnalyzedVariable(
    val declaration: Definition,
    val definedIn: Definition.FunctionDeclaration,
    val useType: VariableUseType,
)

fun analyzeFunctions(
    ast: AST,
    resolvedVariables: ResolvedVariables,
    callGraph: CallGraph,
): FunctionAnalysisResult {
    val relations = findStaticFunctionRelations(ast)
    val variableDeclarationFunctions = getVariableDeclarationFunctions(relations)
    val argumentFunctions = getArgumentFunctions(relations)
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
            variableDeclarationFunctions,
            argumentFunctions,
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
    analyzedVariables: Map<Definition.FunctionDeclaration, Set<AnalyzedVariable>>,
): Map<Definition.FunctionDeclaration, Set<Definition>> {
    val result = mutableMapOf<Definition.FunctionDeclaration, MutableSet<Definition>>()
    analyzedVariables.forEach { (function, variables) ->
        variables.forEach { variable ->
            if (variable.definedIn != function) {
                result.getOrPut(variable.definedIn) { mutableSetOf() }.add(variable.declaration)
            }
        }
    }
    return result
}

fun getArgumentFunctions(
    relations: Map<Definition.FunctionDeclaration, StaticFunctionRelations>,
): Map<Definition.FunctionArgument, Definition.FunctionDeclaration> {
    return relations.flatMap { (function, _) ->
        function.arguments.map { argument ->
            argument to function
        }
    }.toMap()
}

fun makeAnalyzedFunction(
    function: Definition.FunctionDeclaration,
    staticRelations: StaticFunctionRelations,
    analyzedVariables: Map<Definition.FunctionDeclaration, Set<AnalyzedVariable>>,
    variablesUsedInNestedFunctions: Set<Definition>,
): AnalyzedFunction {
    val variables =
        analyzedVariables[function]
            ?: throw IllegalStateException("Analyzed function is missing variable information")
    val parentLink = staticRelations.parent?.let { ParentLink(it, variables.isNotEmpty()) }
    return AnalyzedFunction(parentLink, variables, mutableSetOf(), staticRelations.staticDepth, variablesUsedInNestedFunctions)
}

fun makeAnalyzedVariable(
    usedVariable: UsedVariable,
    resolvedVariables: ResolvedVariables,
    variableDeclarationFunctions: Map<Definition.VariableDeclaration, Definition.FunctionDeclaration>,
    argumentFunctions: Map<Definition.FunctionArgument, Definition.FunctionDeclaration>,
): AnalyzedVariable {
    val definition =
        resolvedVariables[usedVariable.variable] ?: throw IllegalStateException("Variable not resolved")

    val definedIn =
        when (definition) {
            is Definition.VariableDeclaration -> variableDeclarationFunctions[definition]
            is Definition.FunctionArgument -> argumentFunctions[definition]
            else -> error("Variable declaration not found in any function")
        } ?: throw IllegalStateException("Variable $definition not defined in any function")

    return AnalyzedVariable(
        definition,
        definedIn,
        usedVariable.type,
    )
}

private fun getVariableDeclarationFunctions(
    relations: StaticFunctionRelationsMap,
): Map<Definition.VariableDeclaration, Definition.FunctionDeclaration> {
    return relations.flatMap { (function, staticRelations) ->
        staticRelations.declaredVariables.map { variable ->
            variable to function
        }
    }.toMap()
}

private fun analyzedVariables(
    relations: StaticFunctionRelationsMap,
    resolvedVariables: ResolvedVariables,
    variableDeclarationFunctions: Map<Definition.VariableDeclaration, Definition.FunctionDeclaration>,
    argumentFunctions: Map<Definition.FunctionArgument, Definition.FunctionDeclaration>,
) = relations.mapValues { (function, staticRelations) ->
    getAnalyzedVariables(
        function,
        staticRelations.usedVariables,
        resolvedVariables,
        relations,
        variableDeclarationFunctions,
        argumentFunctions,
    )
}

private fun getAnalyzedVariables(
    function: Definition.FunctionDeclaration,
    usedVariables: Set<UsedVariable>,
    resolvedVariables: ResolvedVariables,
    relations: StaticFunctionRelationsMap,
    variableDeclarationFunctions: Map<Definition.VariableDeclaration, Definition.FunctionDeclaration>,
    argumentFunctions: Map<Definition.FunctionArgument, Definition.FunctionDeclaration>,
): Set<AnalyzedVariable> {
    return usedVariables
        .asSequence()
        .filter {
            resolvedVariables[it.variable] is Definition.VariableDeclaration ||
                resolvedVariables[it.variable] is Definition.FunctionArgument
        }
        .map { makeAnalyzedVariable(it, resolvedVariables, variableDeclarationFunctions, argumentFunctions) }
        .toSet()
        .filter {
            relations[it.definedIn]!!.staticDepth < relations[function]!!.staticDepth
        }
        .groupBy { it.declaration }
        .map {
            val useType = variableUseType(it.value.map { variable -> variable.useType })
            AnalyzedVariable(it.key, it.value.first().definedIn, useType)
        }
        .toSet()
}

private fun variableUseType(useTypes: List<VariableUseType>) =
    if (useTypes.isEmpty()) {
        VariableUseType.UNUSED
    } else {
        useTypes.reduce { acc, next -> acc.union(next) }
    }

private fun staticFunctionRelationsClosure(
    staticFunctionRelations: StaticFunctionRelationsMap,
    graph: Map<Definition.FunctionDeclaration, Set<Definition.FunctionDeclaration>>,
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
