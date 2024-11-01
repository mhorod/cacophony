package cacophony.semantic

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Empty
import cacophony.semantic.syntaxtree.Expression
import cacophony.semantic.syntaxtree.FunctionCall
import cacophony.semantic.syntaxtree.Literal
import cacophony.semantic.syntaxtree.OperatorBinary
import cacophony.semantic.syntaxtree.OperatorUnary
import cacophony.semantic.syntaxtree.Statement
import cacophony.semantic.syntaxtree.Type
import cacophony.semantic.syntaxtree.VariableUse
import cacophony.utils.Diagnostics

typealias TypeCheckingResult = Map<Expression, TypeExpr>

// Result contains every variable that could be properly typed
fun checkTypes(
    ast: AST,
    diagnostics: Diagnostics,
    resolvedVariables: ResolvedVariables,
): TypeCheckingResult {
    val types = mapOf("Boolean" to BuiltinType.BooleanType, "Int" to BuiltinType.IntegerType, "Unit" to BuiltinType.UnitType)
    val typer = Typer(diagnostics, resolvedVariables, types)
    typer.typeExpression(ast)
    return typer.result
}

private class Typer(val diagnostics: Diagnostics, val resolvedVariables: ResolvedVariables, val types: Map<String, TypeExpr>) {
    val result: MutableMap<Expression, TypeExpr> = mutableMapOf()
    val typedVariables: MutableMap<Definition, TypeExpr> = mutableMapOf()
    val functionContext = ArrayDeque<TypeExpr>()

    fun typeExpression(expression: Expression): TypeExpr? {
        val expressionType: TypeExpr? =
            when (expression) {
                is Block -> {
                    if (expression.expressions.isEmpty()) {
                        BuiltinType.UnitType
                    } else {
                        for (expr in expression.expressions) typeExpression(expr)
                        result[expression.expressions.last()]
                    }
                }
                is Definition.VariableDeclaration -> {
                    val initType = typeExpression(expression.initExpression) ?: return null
                    val variableType =
                        if (expression.type != null) {
                            val declaredType = translateType(expression.type)
                            // TODO: unknown type
                            if (declaredType == null) return null
                            // TODO: mismatch init vs declared
                            if (!isSubtype(initType, declaredType)) return null
                            declaredType
                        } else {
                            initType
                        }
                    typedVariables[expression] = variableType
                    BuiltinType.UnitType
                }
                is Definition.FunctionArgument -> {
                    val argType = translateType(expression.type)
                    // TODO: diagnostic
                    if (argType == null) return null
                    typedVariables[expression] = argType
                    argType
                }
                // TODO: check with type if exists
                is Definition.FunctionDeclaration -> {
                    // TODO: does not type anything inside if argument or result types are incorrect
                    val argsType = parseArgs(expression.arguments) ?: return null
                    val returnType = translateType(expression.returnType) ?: return null
                    val functionType = FunctionType(argsType, returnType)
                    typedVariables[expression] = functionType
                    functionContext.addLast(returnType)
                    val bodyType = typeExpression(expression.body) ?: return null
                    functionContext.removeLast()
                    // TODO: type mismatch
                    if (!isSubtype(bodyType, returnType)) return null
                    BuiltinType.UnitType
                }
                is Empty -> BuiltinType.UnitType
                // TODO: parse args first to report errors there as well
                is FunctionCall -> {
                    // TODO: type of lhs is ill-formed
                    val functionType = typeExpression(expression.function) ?: return null
                    // TODO: type of lhs is not functional
                    if (functionType !is FunctionType) return null
                    if (functionType.args.size != expression.arguments.size) {
                        throw IllegalStateException(
                            "Arity of function resolved in previous step does not match",
                        )
                    }
                    val argsTypes = expression.arguments.map { typeExpression(it) }
                    (argsTypes zip functionType.args).map { (deduced, required) ->
                        if (deduced == null) {
                            false
                        } // TODO: mismatched types
                        else if (!isSubtype(deduced, required)) {
                            false
                        } else {
                            true
                        }
                    }.forEach { if (!it) return null }
                    functionType.result
                }
                is Literal.BoolLiteral -> BuiltinType.BooleanType
                is Literal.IntLiteral -> BuiltinType.IntegerType
                is OperatorBinary.Addition -> typeOperatorBinary(expression, BuiltinType.IntegerType, BuiltinType.IntegerType)
                is OperatorBinary.Subtraction -> typeOperatorBinary(expression, BuiltinType.IntegerType, BuiltinType.IntegerType)
                is OperatorBinary.Multiplication -> typeOperatorBinary(expression, BuiltinType.IntegerType, BuiltinType.IntegerType)
                is OperatorBinary.Division -> typeOperatorBinary(expression, BuiltinType.IntegerType, BuiltinType.IntegerType)
                is OperatorBinary.Modulo -> typeOperatorBinary(expression, BuiltinType.IntegerType, BuiltinType.IntegerType)
                is OperatorBinary.AdditionAssignment -> typeOperatorAssignment(expression)
                is OperatorBinary.SubtractionAssignment -> typeOperatorAssignment(expression)
                is OperatorBinary.MultiplicationAssignment -> typeOperatorAssignment(expression)
                is OperatorBinary.DivisionAssignment -> typeOperatorAssignment(expression)
                is OperatorBinary.ModuloAssignment -> typeOperatorAssignment(expression)
                is OperatorBinary.Assignment -> {
                    // TODO: remove code duplication with `typeOperatorAssignment()` method
                    val lhsType =
                        if (expression.lhs is VariableUse) {
                            // this is only type of `l-value reference` we have atm
                            typeExpression(expression.lhs)
                        } else {
                            // TODO: ill formed assignment
                            null
                        }
                    val rhsType = typeExpression(expression.rhs)
                    if (lhsType == null || rhsType == null) return null
                    // TODO: wrong type
                    if (!isSubtype(rhsType, lhsType)) return null
                    lhsType
                }
                is OperatorBinary.Equals -> {
                    val lhsType = typeExpression(expression.lhs)
                    val rhsType = typeExpression(expression.rhs)
                    if (lhsType == null || rhsType == null) return null
                    // TODO: type mismatch
                    if (lhsType != rhsType) return null
                    // TODO: no == for other types
                    if (lhsType != BuiltinType.IntegerType && lhsType != BuiltinType.BooleanType) return null
                    BuiltinType.BooleanType
                }
                is OperatorBinary.NotEquals -> {
                    // TODO: remove code duplication with above
                    val lhsType = typeExpression(expression.lhs)
                    val rhsType = typeExpression(expression.rhs)
                    if (lhsType == null || rhsType == null) return null
                    // TODO: type mismatch
                    if (lhsType != rhsType) return null
                    // TODO: no != for other types
                    if (lhsType != BuiltinType.IntegerType && lhsType != BuiltinType.BooleanType) return null
                    BuiltinType.BooleanType
                }
                is OperatorBinary.Greater -> typeOperatorBinary(expression, BuiltinType.IntegerType, BuiltinType.BooleanType)
                is OperatorBinary.GreaterEqual -> typeOperatorBinary(expression, BuiltinType.IntegerType, BuiltinType.BooleanType)
                is OperatorBinary.Less -> typeOperatorBinary(expression, BuiltinType.IntegerType, BuiltinType.BooleanType)
                is OperatorBinary.LessEqual -> typeOperatorBinary(expression, BuiltinType.IntegerType, BuiltinType.BooleanType)
                is OperatorBinary.LogicalAnd -> typeOperatorBinary(expression, BuiltinType.BooleanType, BuiltinType.BooleanType)
                is OperatorBinary.LogicalOr -> typeOperatorBinary(expression, BuiltinType.BooleanType, BuiltinType.BooleanType)
                is OperatorUnary.Minus -> {
                    val innerType = typeExpression(expression.expression) ?: return null
                    // TODO: wrong type
                    if (!isSubtype(innerType, BuiltinType.IntegerType)) return null
                    BuiltinType.IntegerType
                }
                is OperatorUnary.Negation -> {
                    val innerType = typeExpression(expression.expression) ?: return null
                    // TODO: wrong type
                    if (!isSubtype(innerType, BuiltinType.BooleanType)) return null
                    BuiltinType.BooleanType
                }
                // TODO: who should check if break is inside loop?
                is Statement.BreakStatement -> BuiltinType.UnitType
                // TODO: for now `if expr then expr` is equivalent with `if expr then expr else ()`
                is Statement.IfElseStatement -> {
                    val trueBranchType = typeExpression(expression.doExpression)
                    val falseBranchType =
                        if (expression.elseExpression == null) {
                            BuiltinType.UnitType
                        } else {
                            typeExpression(expression.elseExpression)
                        }
                    val conditionType = typeExpression(expression.testExpression)
                    if (conditionType == null || trueBranchType == null || falseBranchType == null) return null
                    // TODO: expected Boolean
                    if (!isSubtype(conditionType, BuiltinType.BooleanType)) return null
                    if (isSubtype(trueBranchType, falseBranchType)) {
                        falseBranchType
                    } else if (isSubtype(falseBranchType, trueBranchType)) {
                        trueBranchType
                    } else {
                        // TODO: Mismatched branches
                        return null
                    }
                }
                is Statement.ReturnStatement -> {
                    val returnedType = typeExpression(expression.value) ?: return null
                    // TODO: return outside function body
                    if (functionContext.isEmpty()) return null
                    // TODO: type mismatch
                    if (!isSubtype(returnedType, functionContext.last())) return null
                    BuiltinType.VoidType
                }
                is Statement.WhileStatement -> {
                    typeExpression(expression.doExpression)
                    val conditionType = typeExpression(expression.testExpression) ?: return null
                    // TODO: expected Boolean
                    if (!isSubtype(conditionType, BuiltinType.BooleanType)) return null
                    BuiltinType.UnitType
                }
                is VariableUse -> typedVariables[resolvedVariables[expression]!!]
            }
        if (expressionType != null) result[expression] = expressionType
        return expressionType
    }

    private fun parseArgs(arguments: List<Definition.FunctionArgument>): List<TypeExpr>? =
        arguments.map { typeExpression(it) }.map { it ?: return null }

    private fun translateType(type: Type): TypeExpr? {
        return when (type) {
            is Type.Basic -> types[type.identifier]
            is Type.Functional ->
                FunctionType(
                    type.argumentsType.map { translateType(it) ?: return null },
                    translateType(type.returnType) ?: return null,
                )
        }
    }

    // Handles +=, -=, etc.
    private fun typeOperatorAssignment(expression: OperatorBinary): TypeExpr? {
        val lhsType =
            if (expression.lhs is VariableUse) {
                // this is only type of `l-value reference` we have atm
                typeExpression(expression.lhs)
            } else {
                // TODO: ill formed assignment
                null
            }
        val rhsType = typeExpression(expression.rhs)
        if (lhsType == null || rhsType == null) return null
        // TODO: wrong type
        if (!isSubtype(lhsType, BuiltinType.IntegerType)) return null
        if (!isSubtype(rhsType, BuiltinType.IntegerType)) return null
        return BuiltinType.IntegerType
    }

    private fun typeOperatorBinary(
        expression: OperatorBinary,
        type: TypeExpr,
        result: TypeExpr,
    ): TypeExpr? {
        val lhsType = typeExpression(expression.lhs)
        val rhsType = typeExpression(expression.rhs)
        if (lhsType == null || rhsType == null) return null
        // TODO: wrong type
        if (!isSubtype(lhsType, type)) return null
        if (!isSubtype(rhsType, type)) return null
        return result
    }
}

sealed class TypeExpr(val name: String) {
    override fun toString(): String {
        return name
    }
}

class BuiltinType private constructor(name: String) : TypeExpr(name) {
    companion object {
        val BooleanType = BuiltinType("Boolean")
        val IntegerType = BuiltinType("Int")
        val UnitType = BuiltinType("Unit")

        // TODO: Propagate Void maybe
        val VoidType = BuiltinType("Void") // Only for return statement
    }
}

data class FunctionType(val args: List<TypeExpr>, val result: TypeExpr) : TypeExpr(args.joinToString(", ", "[", "] -> ${result.name}"))

class UserDefinedType(name: String) : TypeExpr(name)

fun isSubtype(
    subtype: TypeExpr,
    type: TypeExpr,
): Boolean {
    if (subtype == BuiltinType.VoidType) return true
    return subtype.name == type.name
}
