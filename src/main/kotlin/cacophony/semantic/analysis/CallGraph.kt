package cacophony.semantic.analysis

import cacophony.diagnostics.CallGraphDiagnostics
import cacophony.diagnostics.Diagnostics
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.types.FunctionType
import cacophony.semantic.types.TypeCheckingResult
import kotlin.collections.mutableMapOf

// Justyna: CallGraph powinien być ze statycznych w statyczne, prawda?
typealias CallGraph = Map<LambdaExpression, Set<LambdaExpression>>

fun generateCallGraph(
    ast: AST,
    resolvedVariables: ResolvedVariables,
    types: TypeCheckingResult,
    namedFunctionInfo: NamedFunctionInfo,
    diagnostics: Diagnostics,
): CallGraph = CallGraphProvider(diagnostics, resolvedVariables, types, namedFunctionInfo).generateDirectCallGraph(ast, null)

private class CallGraphProvider(
    private val diagnostics: Diagnostics,
    private val resolvedVariables: ResolvedVariables,
    private val types: TypeCheckingResult,
    private val namedFunctionInfo: NamedFunctionInfo,
) {
    fun generateDirectCallGraph(node: Expression?, currentFn: LambdaExpression?): CallGraph =
        when (node) {
            is LambdaExpression -> generateDirectCallGraph(node.body, node)
            is FunctionCall ->
                merge(
                    handleDirectFunctionCall(node.function, currentFn),
                    *node.arguments.map { generateDirectCallGraph(it, currentFn) }.toTypedArray(),
                )

            is Definition.VariableDeclaration -> generateDirectCallGraph(node.value, currentFn)
            is Statement.IfElseStatement ->
                merge(
                    generateDirectCallGraph(node.testExpression, currentFn),
                    generateDirectCallGraph(node.doExpression, currentFn),
                    generateDirectCallGraph(node.elseExpression, currentFn),
                )

            is Statement.WhileStatement ->
                merge(
                    generateDirectCallGraph(node.testExpression, currentFn),
                    generateDirectCallGraph(node.doExpression, currentFn),
                )

            is LambdaExpression -> generateDirectCallGraph(node.body, currentFn)

            is Statement.ReturnStatement -> generateDirectCallGraph(node.value, currentFn)
            is OperatorUnary -> generateDirectCallGraph(node.expression, currentFn)
            is OperatorBinary ->
                merge(
                    generateDirectCallGraph(node.lhs, currentFn),
                    generateDirectCallGraph(node.rhs, currentFn),
                )

            is Block ->
                merge(*node.expressions.map { generateDirectCallGraph(it, currentFn) }.toTypedArray())

            is Struct ->
                merge(
                    *node.fields.values
                        .map { generateDirectCallGraph(it, currentFn) }
                        .toTypedArray(),
                )

            is FieldRef -> generateDirectCallGraph(node.struct(), currentFn)

            is Allocation -> generateDirectCallGraph(node.value, currentFn)

            is Dereference -> generateDirectCallGraph(node.value, currentFn)

            is LeafExpression -> mutableMapOf() // don't use else branch to prevent this from breaking when SyntaxTree is changed
            null -> mutableMapOf()
        }

    // TODO: fix
    private fun handleDirectFunctionCall(fn: Expression, currentFn: LambdaExpression?) =
        when (fn) {
            is VariableUse -> {
                when (val decl = resolvedVariables[fn]) {
                    null -> {
                        diagnostics.report(CallGraphDiagnostics.CallingNonExistentIdentifier(fn.identifier), fn.range)
                        throw diagnostics.fatal()
                    }

                    is Definition.ForeignFunctionDeclaration -> emptyMap()

                    is Definition.VariableDeclaration ->
                        if (declarationIsFunctionDeclaration(decl))
                            currentFn?.let { mapOf(it to setOf(decl)) }.orEmpty()
                        else if (types.definitionTypes[decl]!! is FunctionType) {
                            emptyMap()
                        } else {
                            diagnostics.report(CallGraphDiagnostics.CallingNonFunction(fn.identifier), fn.range)
                            throw diagnostics.fatal()
                        }

                    else -> {
                        diagnostics.report(CallGraphDiagnostics.CallingNonFunction(fn.identifier), fn.range)
                        throw diagnostics.fatal()
                    }
                }
            }

            else -> generateDirectCallGraph(fn, currentFn)
        }
}

private fun <K, V> merge(to: MutableMap<K, MutableSet<V>>, vararg other: Map<K, Set<V>>) =
    other.forEach { from -> from.forEach { (k, v) -> to.getOrPut(k) { mutableSetOf() } += v } }.let { to }

private fun <K, V> merge(vararg maps: Map<K, Set<V>>) = merge(mutableMapOf(), *maps)
