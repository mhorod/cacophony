package cacophony.semantic.analysis

import cacophony.diagnostics.CallGraphDiagnostics
import cacophony.diagnostics.Diagnostics
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression
import cacophony.semantic.syntaxtree.FieldRef
import cacophony.semantic.syntaxtree.FunctionCall
import cacophony.semantic.syntaxtree.LeafExpression
import cacophony.semantic.syntaxtree.OperatorBinary
import cacophony.semantic.syntaxtree.OperatorUnary
import cacophony.semantic.syntaxtree.Statement
import cacophony.semantic.syntaxtree.Struct
import cacophony.semantic.syntaxtree.VariableUse
import kotlin.collections.mutableMapOf

typealias CallGraph = Map<Definition.FunctionDefinition, Set<Definition.FunctionDefinition>>

fun generateCallGraph(ast: AST, diagnostics: Diagnostics, resolvedVariables: ResolvedVariables): CallGraph =
    CallGraphProvider(diagnostics, resolvedVariables).generateDirectCallGraph(ast, null)

private class CallGraphProvider(
    private val diagnostics: Diagnostics,
    private val resolvedVariables: ResolvedVariables,
) {
    fun generateDirectCallGraph(node: Expression?, currentFn: Definition.FunctionDefinition?): CallGraph =
        when (node) {
            is Definition.FunctionDefinition -> generateDirectCallGraph(node.body, node)
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

            is LeafExpression -> mutableMapOf() // don't use else branch to prevent this from breaking when SyntaxTree is changed
            null -> mutableMapOf()
        }

    private fun handleDirectFunctionCall(fn: Expression, currentFn: Definition.FunctionDefinition?) =
        when (fn) {
            is VariableUse -> {
                when (val decl = resolvedVariables[fn]) {
                    is Definition.FunctionDefinition ->
                        currentFn?.let { mapOf(it to setOf(decl)) }.orEmpty()

                    is Definition.ForeignFunctionDeclaration -> emptyMap()

                    is Definition -> {
                        diagnostics.report(CallGraphDiagnostics.CallingNonFunction(fn.identifier), fn.range)
                        throw diagnostics.fatal()
                    }

                    null -> {
                        diagnostics.report(CallGraphDiagnostics.CallingNonExistentIdentifier(fn.identifier), fn.range)
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
