package cacophony.semantic

import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.ORDiagnostics
import cacophony.semantic.syntaxtree.*

fun resolveOverloads(
    ast: AST,
    diagnostics: Diagnostics,
    nr: NameResolutionResult,
): ResolvedVariables {
    val resolvedVariables = mutableMapOf<VariableUse, Definition>()

    fun resolveOverloadsRec(expr: Expression) {
        when (expr) {
            is Block -> expr.expressions.forEach { resolveOverloadsRec(it) }
            is Definition.VariableDeclaration -> resolveOverloadsRec(expr.value)
            is Definition.FunctionDeclaration -> resolveOverloadsRec(expr.body)
            is FunctionCall -> {
                if (expr.function is VariableUse) {
                    when (val resName = nr[expr.function]!!) {
                        is ResolvedName.Function -> {
                            when (val overload = resName.def[expr.arguments.size]) {
                                null ->
                                    diagnostics.report(ORDiagnostics.IdentifierNotFound(expr.function.identifier), expr.function.range)
                                else -> resolvedVariables[expr.function] = overload
                            }
                        }
                        is ResolvedName.Argument ->
                            diagnostics.report(ORDiagnostics.UsingArgumentAsFunction(expr.function.identifier), expr.function.range)
                        is ResolvedName.Variable ->
                            diagnostics.report(ORDiagnostics.UsingVariableAsFunction(expr.function.identifier), expr.function.range)
                    }
                } else {
                    diagnostics.report(ORDiagnostics.FunctionIsNotVariableUse, expr.function.range)
                }
                expr.arguments.forEach { resolveOverloadsRec(it) }
            }
            is Statement.IfElseStatement -> {
                resolveOverloadsRec(expr.testExpression)
                resolveOverloadsRec(expr.doExpression)
                expr.elseExpression?.let { resolveOverloadsRec(it) }
            }
            is Statement.ReturnStatement -> resolveOverloadsRec(expr.value)
            is Statement.WhileStatement -> {
                resolveOverloadsRec(expr.testExpression)
                resolveOverloadsRec(expr.doExpression)
            }
            is VariableUse -> {
                when (val resName = nr[expr]!!) {
                    is ResolvedName.Variable -> {
                        resolvedVariables[expr] = resName.def
                    }
                    is ResolvedName.Argument -> {
                        resolvedVariables[expr] = resName.def
                    }
                    is ResolvedName.Function -> {
                        diagnostics.report(ORDiagnostics.UnexpectedFunctionCall, expr.range)
                    }
                }
            }
            is OperatorUnary -> resolveOverloadsRec(expr.expression)
            is OperatorBinary -> {
                resolveOverloadsRec(expr.lhs)
                resolveOverloadsRec(expr.rhs)
            }
            is LeafExpression -> {} // don't use else branch to prevent this from breaking when SyntaxTree is changed
        }
    }

    resolveOverloadsRec(ast)
    return resolvedVariables
}

typealias ResolvedVariables = Map<VariableUse, Definition>
