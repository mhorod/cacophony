package cacophony.semantic

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression
import cacophony.semantic.syntaxtree.FunctionCall
import cacophony.semantic.syntaxtree.OperatorBinary
import cacophony.semantic.syntaxtree.OperatorUnary
import cacophony.semantic.syntaxtree.Statement
import cacophony.semantic.syntaxtree.VariableUse
import cacophony.utils.Diagnostics

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
                                null -> diagnostics.report("Cannot find an appropriate function declaration", expr.range)
                                else -> resolvedVariables[expr.function] = overload
                            }
                        }
                        is ResolvedName.Argument -> diagnostics.report("Cannot use function argument as a function", expr.function.range)
                        is ResolvedName.Variable -> diagnostics.report("Cannot use variable as a function", expr.function.range)
                    }
                } else {
                    diagnostics.report("Function has to be a simple expression", expr.function.range)
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
            else -> {}
        }
    }

    resolveOverloadsRec(ast)
    return resolvedVariables
}

typealias ResolvedVariables = Map<VariableUse, Definition>
