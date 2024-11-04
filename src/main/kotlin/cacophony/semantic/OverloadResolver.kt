package cacophony.semantic

import cacophony.semantic.syntaxtree.*
import cacophony.utils.Diagnostics

class OverloadResolutionError(
    reason: String,
) : Exception(reason)

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
                                null -> throw OverloadResolutionError(
                                    "Cannot find an appropriate function declaration: " +
                                        "${expr.function.identifier} at ${expr.range}",
                                )
                                else -> resolvedVariables[expr.function] = overload
                            }
                        }
                        is ResolvedName.Argument -> throw OverloadResolutionError(
                            "Cannot use function argument as a function: " +
                                "${expr.function.identifier} at ${expr.function.range}",
                        )
                        is ResolvedName.Variable -> throw OverloadResolutionError(
                            "Cannot use variable as a function: " +
                                "${expr.function.identifier} at ${expr.function.range}",
                        )
                    }
                } else {
                    throw OverloadResolutionError(
                        "Function has to be a simple expression: at ${expr.function.range}",
                    )
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
                        diagnostics.report("Unexpected function call", expr.range)
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
