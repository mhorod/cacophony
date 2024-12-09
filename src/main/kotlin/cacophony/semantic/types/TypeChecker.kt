package cacophony.semantic.types

import cacophony.diagnostics.Diagnostics
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.*
import cacophony.utils.CompileException
import cacophony.utils.Location

typealias TypeCheckingResult = Map<Expression, TypeExpr>

// Result contains every variable that could be properly typed
fun checkTypes(ast: AST, diagnostics: Diagnostics, resolvedVariables: ResolvedVariables): TypeCheckingResult {
    val typer = Typer(diagnostics, resolvedVariables)
    typer.typeExpression(ast)
    return typer.result
}

private class Typer(
    diagnostics: Diagnostics,
    val resolvedVariables: ResolvedVariables,
) {
    val result: MutableMap<Expression, TypeExpr> = mutableMapOf()
    val typedVariables: MutableMap<Definition, TypeExpr> = mutableMapOf()
    val functionContext = ArrayDeque<TypeExpr>()
    val error = ErrorHandler(diagnostics)
    val translator = TypeTranslator(diagnostics)
    var whileDepth = 0

    fun typeExpression(expression: Expression): TypeExpr? {
        val expressionType: TypeExpr? =
            when (expression) {
                is Block -> {
                    if (expression.expressions.isEmpty()) {
                        BuiltinType.UnitType
                    } else {
                        var voided = false
                        for (expr in expression.expressions) {
                            val type = typeExpression(expr)
                            if (type is TypeExpr.VoidType) voided = true
                        }
                        if (voided) TypeExpr.VoidType else result[expression.expressions.last()]
                    }
                }

                is Definition.VariableDeclaration -> {
                    val deducedType = typeExpression(expression.value) ?: return null
                    val variableType = initializedType(expression.type, deducedType, expression.value.range) ?: return null
                    typedVariables[expression] = variableType
                    BuiltinType.UnitType
                }

                is Definition.FunctionArgument -> {
                    val argType = translator.translateType(expression.type) ?: return null
                    typedVariables[expression] = argType
                    argType
                }

                is Definition.FunctionDeclaration -> {
                    // does not type anything inside if argument or result types are incorrect
                    val argsType = parseArgs(expression.arguments) ?: return null
                    val returnType = translator.translateType(expression.returnType) ?: return null
                    val deducedType = FunctionType(argsType, returnType)
                    val functionType = initializedType(expression.type, deducedType, expression.range) ?: return null
                    typedVariables[expression] = functionType
                    functionContext.addLast(returnType)
                    val bodyType = typeExpression(expression.body) ?: return null
                    functionContext.removeLast()
                    if (!isSubtype(bodyType, returnType)) {
                        error.typeMismatchError(returnType, bodyType, expression.body.range)
                        return null
                    }
                    BuiltinType.UnitType
                }

                is Struct -> {
                    StructType(
                        expression.fields.map { (field, fieldExpr) ->
                            field.name to (
                                initializedType(
                                    field.type,
                                    (typeExpression(fieldExpr) ?: return null),
                                    fieldExpr.range,
                                ) ?: return null
                            )
                        }.toMap(),
                    )
                }
                is StructField -> {
                    BuiltinType.UnitType
                }

                is Empty -> BuiltinType.UnitType
                is FunctionCall -> {
                    val argsTypes = expression.arguments.map { typeExpression(it) }
                    val functionType = typeExpression(expression.function) ?: return null
                    if (functionType !is FunctionType) {
                        error.expectedFunction(expression.function.range)
                        return null
                    }
                    if (functionType.args.size != expression.arguments.size) {
                        throw IllegalStateException(
                            "Arity of function resolved in previous step does not match",
                        )
                    }
                    (argsTypes zip functionType.args)
                        .mapIndexed { ind, (deduced, required) ->
                            if (deduced == null) {
                                false
                            } else if (!isSubtype(deduced, required)) {
                                error.typeMismatchError(required, deduced, expression.arguments[ind].range)
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
                    val (lhsType, rhsType) = typeBinary(expression) ?: return null
                    if (expression.lhs !is VariableUse) {
                        error.expectedLvalue(expression.lhs.range)
                        return null
                    }
                    if (!isSubtype(rhsType, lhsType)) {
                        error.typeMismatchError(lhsType, rhsType, expression.rhs.range)
                        return null
                    }
                    lhsType
                }

                is OperatorBinary.Equals -> {
                    val (lhsType, rhsType) = typeBinary(expression) ?: return null
                    if (lhsType != rhsType) {
                        error.typeMismatchError(lhsType, rhsType, expression.range)
                        return null
                    }
                    if (lhsType != BuiltinType.IntegerType && lhsType != BuiltinType.BooleanType) {
                        error.operationNotSupportedOn("== operator", lhsType, expression.range)
                        return null
                    }
                    BuiltinType.BooleanType
                }

                is OperatorBinary.NotEquals -> {
                    val (lhsType, rhsType) = typeBinary(expression) ?: return null
                    if (lhsType != rhsType) {
                        error.typeMismatchError(lhsType, rhsType, expression.range)
                        return null
                    }
                    if (lhsType != BuiltinType.IntegerType && lhsType != BuiltinType.BooleanType) {
                        error.operationNotSupportedOn("!= operator", lhsType, expression.range)
                        return null
                    }
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
                    if (!isSubtype(innerType, BuiltinType.IntegerType)) {
                        error.operationNotSupportedOn("unary - operator", innerType, expression.expression.range)
                        return null
                    }
                    BuiltinType.IntegerType
                }

                is OperatorUnary.Negation -> {
                    val innerType = typeExpression(expression.expression) ?: return null
                    if (!isSubtype(innerType, BuiltinType.BooleanType)) {
                        error.operationNotSupportedOn("unary ! operator", innerType, expression.expression.range)
                        return null
                    }
                    BuiltinType.BooleanType
                }

                is Statement.BreakStatement -> {
                    if (whileDepth == 0) {
                        error.breakOutsideWhile(expression.range)
                        return null
                    }
                    TypeExpr.VoidType
                }

                is Statement.IfElseStatement -> {
                    val trueBranchType = typeExpression(expression.doExpression)
                    val falseBranchType =
                        if (expression.elseExpression == null) {
                            // if non-existent, the else branch has the Unit type
                            BuiltinType.UnitType
                        } else {
                            typeExpression(expression.elseExpression)
                        }
                    val conditionType = typeExpression(expression.testExpression)
                    if (conditionType == null || trueBranchType == null || falseBranchType == null) return null
                    if (!isSubtype(conditionType, BuiltinType.BooleanType)) {
                        error.typeMismatchError(BuiltinType.BooleanType, conditionType, expression.testExpression.range)
                        return null
                    }
                    if (isSubtype(trueBranchType, falseBranchType)) {
                        falseBranchType
                    } else if (isSubtype(falseBranchType, trueBranchType)) {
                        trueBranchType
                    } else {
                        error.noCommonType(trueBranchType, falseBranchType, expression.range)
                        return null
                    }
                }

                is Statement.ReturnStatement -> {
                    val returnedType = typeExpression(expression.value) ?: return null
                    if (functionContext.isEmpty()) {
                        error.returnOutsideFunction(expression.range)
                        return null
                    }
                    if (!isSubtype(returnedType, functionContext.last())) {
                        error.typeMismatchError(functionContext.last(), returnedType, expression.range)
                        return null
                    }
                    TypeExpr.VoidType
                }

                is Statement.WhileStatement -> {
                    whileDepth++
                    typeExpression(expression.doExpression)
                    whileDepth--
                    val conditionType = typeExpression(expression.testExpression) ?: return null
                    if (!isSubtype(conditionType, BuiltinType.BooleanType)) {
                        error.typeMismatchError(BuiltinType.BooleanType, conditionType, expression.testExpression.range)
                        return null
                    }
                    BuiltinType.UnitType
                }

                is VariableUse -> typedVariables[resolvedVariables[expression]!!]
            }
        if (expressionType != null) result[expression] = expressionType
        return expressionType
    }

    // returns type of x in constructions `let x (: type) = expr`
    private fun initializedType(type: Type?, deducedType: TypeExpr, range: Pair<Location, Location>): TypeExpr? {
        return if (type != null) {
            val declaredType = translator.translateType(type) ?: return null
            if (!isSubtype(deducedType, declaredType)) {
                error.typeMismatchError(declaredType, deducedType, range)
                return null
            }
            declaredType
        } else {
            deducedType
        }
    }

    private fun parseArgs(arguments: List<Definition.FunctionArgument>): List<TypeExpr>? =
        arguments.map { typeExpression(it) }.map { it ?: return null }

    private fun typeBinary(expression: OperatorBinary): Pair<TypeExpr, TypeExpr>? {
        val lhsType = typeExpression(expression.lhs)
        val rhsType = typeExpression(expression.rhs)
        if (lhsType == null || rhsType == null) return null
        return lhsType to rhsType
    }

    // Handles +=, -=, etc.
    private fun typeOperatorAssignment(expression: OperatorBinary): TypeExpr? {
        val (lhsType, rhsType) = typeBinary(expression) ?: return null
        if (expression.lhs !is VariableUse) {
            error.expectedLvalue(expression.lhs.range)
            return null
        }
        if (!isSubtype(lhsType, BuiltinType.IntegerType)) {
            error.typeMismatchError(BuiltinType.IntegerType, lhsType, expression.lhs.range)
            return null
        }
        if (!isSubtype(rhsType, BuiltinType.IntegerType)) {
            error.typeMismatchError(BuiltinType.IntegerType, rhsType, expression.rhs.range)
            return null
        }
        return BuiltinType.IntegerType
    }

    // handles +, -, etc.
    private fun typeOperatorBinary(expression: OperatorBinary, type: TypeExpr, result: TypeExpr): TypeExpr? {
        val (lhsType, rhsType) = typeBinary(expression) ?: return null
        if (!isSubtype(lhsType, type)) {
            error.typeMismatchError(type, lhsType, expression.lhs.range)
            return null
        }
        if (!isSubtype(rhsType, type)) {
            error.typeMismatchError(type, rhsType, expression.lhs.range)
            return null
        }
        return result
    }
}
