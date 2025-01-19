package cacophony.semantic.names

import cacophony.diagnostics.Diagnostics
import cacophony.semantic.syntaxtree.*

typealias ResolvedVariables = Map<VariableUse, Definition>

fun resolveOverloads(ast: AST, nr: NameResolutionResult, diagnostics: Diagnostics): ResolvedVariables {
    val resolvedVariables = mutableMapOf<VariableUse, Definition>()

    fun resolveOverloadsRec(expr: Expression) {
        when (expr) {
            is Block -> expr.expressions.forEach { resolveOverloadsRec(it) }
            is Definition.VariableDeclaration -> resolveOverloadsRec(expr.value)
            is Definition.FunctionDefinition -> resolveOverloadsRec(expr.body)
            is FunctionCall -> {
                resolveOverloadsRec(expr.function)
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

            is LambdaExpression -> resolveOverloadsRec(expr.body)

            is Struct -> {
                expr.fields.values.forEach { resolveOverloadsRec(it) }
            }

            is FieldRef -> {
                resolveOverloadsRec(expr.struct())
            }

            is VariableUse -> {
                when (val resName = nr[expr]!!) {
                    is ResolvedName.Variable -> {
                        resolvedVariables[expr] = resName.def
                    }

                    is ResolvedName.Argument -> {
                        resolvedVariables[expr] = resName.def
                    }
                }
            }

            is OperatorUnary -> resolveOverloadsRec(expr.expression)
            is OperatorBinary -> {
                resolveOverloadsRec(expr.lhs)
                resolveOverloadsRec(expr.rhs)
            }

            is Allocation -> resolveOverloadsRec(expr.value)

            is Dereference -> resolveOverloadsRec(expr.value)

            is LeafExpression -> {} // don't use else branch to prevent this from breaking when SyntaxTree is changed
        }
    }

    resolveOverloadsRec(ast)
    return resolvedVariables
}
