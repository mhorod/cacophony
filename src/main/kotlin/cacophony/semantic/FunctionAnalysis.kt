package cacophony.semantic

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
    val staticDepth: Int,
)

data class AnalyzedVariable(
    val declaration: Definition.VariableDeclaration,
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
    val analyzedVariables = analyzedVariables(relations, resolvedVariables, variableDeclarationFunctions)
    val functionsUsingParentLinks = functionsUsingParentLinks(relations, analyzedVariables, callGraph)

    return relations.mapValues { (function, staticRelations) ->
        makeAnalyzedFunction(function, staticRelations, analyzedVariables, functionsUsingParentLinks)
    }
}

fun makeAnalyzedFunction(
    function: Definition.FunctionDeclaration,
    staticRelations: StaticFunctionRelations,
    analyzedVariables: Map<Definition.FunctionDeclaration, Set<AnalyzedVariable>>,
    functionsUsingParentLinks: Set<Definition.FunctionDeclaration>,
): AnalyzedFunction {
    val parentLink = staticRelations.parent?.let { ParentLink(it, function in functionsUsingParentLinks) }
    val variables =
        analyzedVariables[function]
            ?: throw IllegalStateException("Analyzed function is missing variable information")
    return AnalyzedFunction(parentLink, variables, staticRelations.staticDepth)
}

fun makeAnalyzedVariable(
    usedVariable: UsedVariable,
    resolvedVariables: ResolvedVariables,
    variableDeclarationFunctions: Map<Definition.VariableDeclaration, Definition.FunctionDeclaration>,
): AnalyzedVariable {
    val definition =
        resolvedVariables[usedVariable.variable] ?: throw IllegalStateException("Variable not resolved")
    val definedIn =
        variableDeclarationFunctions[definition]
            ?: throw IllegalStateException("Variable declaration not found in any function")
    return AnalyzedVariable(
        definition as Definition.VariableDeclaration,
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
) = relations.mapValues { (_, staticRelations) ->
    getAnalyzedVariables(
        staticRelations.declaredVariables,
        staticRelations.usedVariables,
        resolvedVariables,
        variableDeclarationFunctions,
    )
}

private fun getAnalyzedVariables(
    declaredVariables: Set<Definition.VariableDeclaration>,
    usedVariables: Set<UsedVariable>,
    resolvedVariables: ResolvedVariables,
    variableDeclarationFunctions: Map<Definition.VariableDeclaration, Definition.FunctionDeclaration>,
): Set<AnalyzedVariable> {
    return usedVariables
        .asSequence()
        .filter { resolvedVariables[it.variable] is Definition.VariableDeclaration }
        .map { makeAnalyzedVariable(it, resolvedVariables, variableDeclarationFunctions) }
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
