package cacophony.semantic

import cacophony.controlflow.Variable
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.utils.getReachableFrom
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
    val analyzedVariables =
        analyzedVariables(relations, resolvedVariables, variableDeclarationFunctions, argumentFunctions)
    val functionsUsingParentLinks = functionsUsingParentLinks(relations, analyzedVariables, callGraph)
    val variablesUsedInNestedFunctions = variablesUsedInNestedFunctions(analyzedVariables)

    return relations.mapValues { (function, staticRelations) ->
        makeAnalyzedFunction(
            function,
            staticRelations,
            analyzedVariables,
            functionsUsingParentLinks,
            variablesUsedInNestedFunctions[function] ?: emptySet(),
        )
    }
}

fun variablesUsedInNestedFunctions(
    analyzedVariables: Map<Definition.FunctionDeclaration, Set<AnalyzedVariable>>,
): Map<Definition.FunctionDeclaration, Set<Definition>> {
    val result = mutableMapOf<Definition.FunctionDeclaration, MutableSet<Definition>>()
    analyzedVariables.forEach { function, variables ->
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
    functionsUsingParentLinks: Set<Definition.FunctionDeclaration>,
    variablesUsedInNestedFunctions: Set<Definition>,
): AnalyzedFunction {
    val parentLink = staticRelations.parent?.let { ParentLink(it, function in functionsUsingParentLinks) }
    val variables =
        analyzedVariables[function]
            ?: throw IllegalStateException("Analyzed function is missing variable information")
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

private fun functionsUsingParentLinks(
    relations: StaticFunctionRelationsMap,
    analyzedVariables: Map<Definition.FunctionDeclaration, Set<AnalyzedVariable>>,
    callGraph: CallGraph,
): Set<Definition.FunctionDeclaration> {
    val calledByGraph = reverseGraph(callGraph)
    val parentGraph =
        relations.mapValues { (_, staticRelations) -> staticRelations.parent?.let { setOf(it) } ?: emptySet() }
    val directUsages = relations.keys.filter { usesParentLinkDirectly(it, analyzedVariables[it] ?: emptySet()) }
    val nestedUsages = getReachableFrom(directUsages, parentGraph)
    return getReachableFrom(nestedUsages, calledByGraph)
}

private fun usesParentLinkDirectly(
    function: Definition.FunctionDeclaration,
    analyzedVariables: Set<AnalyzedVariable>,
) = analyzedVariables.any { it.definedIn != function }

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
        staticRelations.declaredVariables,
        staticRelations.usedVariables,
        function.arguments,
        resolvedVariables,
        variableDeclarationFunctions,
        argumentFunctions,
    )
}

private fun getAnalyzedVariables(
    declaredVariables: Set<Definition.VariableDeclaration>,
    usedVariables: Set<UsedVariable>,
    arguments: List<Definition.FunctionArgument>,
    resolvedVariables: ResolvedVariables,
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
        .union(
            declaredVariables.map {
                AnalyzedVariable(
                    it,
                    variableDeclarationFunctions[it]!!,
                    VariableUseType.UNUSED,
                )
            },
        )
        .union(
            arguments.map {
                AnalyzedVariable(
                    it,
                    argumentFunctions[it]!!,
                    VariableUseType.UNUSED,
                )
            },
        )
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
