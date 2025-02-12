package cacophony.semantic.types

import cacophony.diagnostics.Diagnostics
import cacophony.semantic.names.ArityResolutionResult
import cacophony.semantic.names.EntityResolutionResult
import cacophony.semantic.names.NameResolutionResult
import cacophony.semantic.names.ResolvedEntity
import cacophony.semantic.syntaxtree.*
import cacophony.utils.Location

// Result contains every variable that could be properly typed
fun checkTypes(ast: AST, nr: NameResolutionResult, diagnostics: Diagnostics): TypeCheckingResult {
    val typer = Typer(diagnostics, nr.entityResolution, nr.shapeResolution)
    typer.typeExpression(ast, Expectation.Any)
    return TypeCheckingResult(typer.result, typer.typedVariables, typer.resolvedVariables)
}

interface Expectation {
    data class Arity(val arity: Int) : Expectation

    data object Any : Expectation

    data object Value : Expectation
}

fun TypeExpr?.toExpectation(): Expectation =
    when (this) {
        BuiltinType.BooleanType -> Expectation.Value
        BuiltinType.IntegerType -> Expectation.Value
        BuiltinType.UnitType -> Expectation.Value
        is FunctionType -> Expectation.Arity(this.args.size)
        is ReferentialType -> Expectation.Value
        is StructType -> Expectation.Value
        is TypeExpr.VoidType -> throw IllegalArgumentException("Void type cannot appear in ast, thus it has no expectation")
        null -> Expectation.Any
    }

fun Type?.toExpectation(): Expectation =
    when (this) {
        is BaseType.Basic -> Expectation.Value
        is BaseType.Functional -> Expectation.Arity(this.argumentsType.size)
        is BaseType.Referential -> Expectation.Value
        is BaseType.Structural -> Expectation.Value
        null -> Expectation.Any
    }

private class Typer(
    diagnostics: Diagnostics,
    val resolvedEntities: EntityResolutionResult,
    val arities: ArityResolutionResult,
) {
    val result: MutableMap<Expression, TypeExpr> = mutableMapOf()
    val typedVariables: MutableMap<Definition, TypeExpr> = mutableMapOf()
    val functionContext = ArrayDeque<TypeExpr>()
    val error = ErrorHandler(diagnostics)
    val translator = TypeTranslator(diagnostics)
    val resolvedVariables: MutableMap<VariableUse, Definition> = mutableMapOf()
    var whileDepth = 0

    fun typeExpression(expression: Expression, expectation: Expectation): TypeExpr? {
        val expressionType: TypeExpr? =
            when (expression) {
                is Block -> {
                    if (expression.expressions.isEmpty()) {
                        BuiltinType.UnitType
                    } else {
                        var voided = false
                        for (expr in expression.expressions.dropLast(1)) {
                            val type = typeExpression(expr, Expectation.Any)
                            if (type is TypeExpr.VoidType) voided = true
                        }
                        val type = typeExpression(expression.expressions.last(), expectation)
                        if (voided) TypeExpr.VoidType else type
                    }
                }

                is Definition.VariableDefinition -> {
                    val expect =
                        if (expression.type == null) {
                            arities[expression]?.let { Expectation.Arity(it) } ?: Expectation.Value
                        } else expression.type.toExpectation()
                    val deducedType = typeExpression(expression.value, expect) ?: return null
                    val variableType = initializedType(expression.type, deducedType, expression.value.range) ?: return null
                    typedVariables[expression] = variableType
                    BuiltinType.UnitType
                }

                is Definition.FunctionArgument -> {
                    val argType = translator.translateType(expression.type) ?: return null
                    typedVariables[expression] = argType
                    argType
                }

                is Definition.FunctionDefinition -> {
                    val argsType = parseArgs(expression.value.arguments) ?: return null
                    val returnType = translator.translateType(expression.value.returnType) ?: return null
                    val deducedType = FunctionType(argsType, returnType)
                    val functionType = initializedType(expression.type, deducedType, expression.range) ?: return null
                    typedVariables[expression] = functionType
                    typeExpression(expression.value, functionType.toExpectation())
                    BuiltinType.UnitType
                }

                is LambdaExpression -> {
                    val argsType = parseArgs(expression.arguments) ?: return null
                    val returnType = translator.translateType(expression.returnType) ?: return null
                    val deducedType = FunctionType(argsType, returnType)
                    val functionType = initializedType(null, deducedType, expression.range) ?: return null
                    functionContext.addLast(returnType)
                    val bodyType = typeExpression(expression.body, returnType.toExpectation()) ?: return null
                    functionContext.removeLast()
                    if (!isSubtype(bodyType, returnType)) {
                        error.typeMismatchError(returnType, bodyType, expression.body.range)
                        return null
                    }
                    FunctionType(argsType, returnType)
                }

                is Definition.ForeignFunctionDeclaration -> {
                    val functionType =
                        translator.translateType(
                            expression.type ?: error("foreign function without a type"),
                        ) ?: return null
                    typedVariables[expression] = functionType
                    BuiltinType.UnitType
                }

                is Struct -> {
                    StructType(
                        expression.fields
                            .map { (field, fieldExpr) ->
                                field.name to (
                                    initializedType(
                                        field.type,
                                        (typeExpression(fieldExpr, field.type.toExpectation()) ?: return null),
                                        fieldExpr.range,
                                    ) ?: return null
                                )
                            }.toMap(),
                    )
                }
                is StructField -> {
                    BuiltinType.UnitType
                }
                is FieldRef -> {
                    val structType = typeExpression(expression.struct(), Expectation.Value)
                    if (structType !is StructType) {
                        error.expectedStructure(expression.struct().range)
                        return null
                    }
                    val type = structType.fields[expression.field]
                    if (type == null) {
                        error.noSuchField(expression.range, structType, expression.field)
                    }
                    type
                }

                is Empty -> BuiltinType.UnitType
                is FunctionCall -> {
                    val functionType = typeExpression(expression.function, Expectation.Arity(expression.arguments.size)) ?: return null
                    if (functionType !is FunctionType) {
                        error.expectedFunction(expression.function.range)
                        return null
                    }

                    if (functionType.args.size != expression.arguments.size) {
                        throw IllegalStateException(
                            "Arity of function resolved in previous step does not match",
                        )
                    }
                    val argsTypes =
                        expression.arguments.zip(functionType.args).map { (expr, type) ->
                            typeExpression(expr, type.toExpectation())
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
                    if (expression.lhs !is Assignable) {
                        error.expectedLvalue(expression.lhs.range)
                        return null
                    }
                    if (expression.lhs is VariableUse && resolvedEntities[expression.lhs] is ResolvedEntity.WithOverloads) {
                        error.expectedLvalue(expression.lhs.range)
                        return null
                    }
                    val lhsType = typeExpression(expression.lhs, Expectation.Any) ?: return null
                    val rhsType = typeExpression(expression.rhs, lhsType.toExpectation()) ?: return null
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
                    val innerType = typeExpression(expression.expression, Expectation.Value) ?: return null
                    if (!isSubtype(innerType, BuiltinType.IntegerType)) {
                        error.operationNotSupportedOn("unary - operator", innerType, expression.expression.range)
                        return null
                    }
                    BuiltinType.IntegerType
                }

                is OperatorUnary.Negation -> {
                    val innerType = typeExpression(expression.expression, Expectation.Value) ?: return null
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
                    val trueBranchType = typeExpression(expression.doExpression, expectation)
                    val falseBranchType =
                        if (expression.elseExpression == null) {
                            // if non-existent, the else branch has the Unit type
                            BuiltinType.UnitType
                        } else {
                            typeExpression(expression.elseExpression, expectation)
                        }
                    val conditionType = typeExpression(expression.testExpression, Expectation.Value)
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
                    if (functionContext.isEmpty()) {
                        error.returnOutsideFunction(expression.range)
                        return null
                    }

                    val returnedType = typeExpression(expression.value, functionContext.last().toExpectation()) ?: return null

                    if (!isSubtype(returnedType, functionContext.last())) {
                        error.typeMismatchError(functionContext.last(), returnedType, expression.range)
                        return null
                    }
                    TypeExpr.VoidType
                }

                is Statement.WhileStatement -> {
                    whileDepth++
                    typeExpression(expression.doExpression, Expectation.Any)
                    whileDepth--
                    val conditionType = typeExpression(expression.testExpression, Expectation.Value) ?: return null
                    if (!isSubtype(conditionType, BuiltinType.BooleanType)) {
                        error.typeMismatchError(BuiltinType.BooleanType, conditionType, expression.testExpression.range)
                        return null
                    }
                    BuiltinType.UnitType
                }

                is Allocation -> {
                    val valueType = typeExpression(expression.value, Expectation.Value) ?: return null

                    if (NON_ALLOCATABLE_TYPES.contains(valueType)) {
                        error.invalidAllocation(expression.range, valueType)
                        return null
                    }

                    ReferentialType(valueType)
                }

                is Dereference -> {
                    val referenceType = typeExpression(expression.value, Expectation.Value) ?: return null
                    if (referenceType !is ReferentialType) {
                        error.expectedReferentialType(expression.range)
                        return null
                    }
                    referenceType.type
                }

                is VariableUse -> {
                    val def =
                        when (expectation) {
                            is Expectation.Arity ->
                                when (val entity = resolvedEntities[expression]) {
                                    is ResolvedEntity.Unambiguous -> entity.definition
                                    is ResolvedEntity.WithOverloads -> {
                                        when (val def = entity.overloads[expectation.arity]) {
                                            null -> {
                                                error.tooFewOverloads(expression.range)
                                                return null
                                            }

                                            else -> def
                                        }
                                    }

                                    null -> throw IllegalArgumentException("Entity not resolved :(")
                                }
                            else -> {
                                when (val entity = resolvedEntities[expression]) {
                                    is ResolvedEntity.Unambiguous -> entity.definition
                                    is ResolvedEntity.WithOverloads -> {
                                        if (entity.overloads.size == 1) {
                                            entity.overloads.values.first()
                                        } else {
                                            error.tooManyOverloads(expression.range)
                                            return null
                                        }
                                    }
                                    null -> throw IllegalArgumentException("Entity not resolved :( $expression")
                                }
                            }
                        }
                    resolvedVariables[expression] = def
                    typedVariables[def]!!
                }
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
        arguments.map { typeExpression(it, Expectation.Any) }.map { it ?: return null }

    private fun typeBinary(expression: OperatorBinary): Pair<TypeExpr, TypeExpr>? {
        val lhsType = typeExpression(expression.lhs, Expectation.Value)
        val rhsType = typeExpression(expression.rhs, Expectation.Value)
        if (lhsType == null || rhsType == null) return null
        return lhsType to rhsType
    }

    // Handles +=, -=, etc.
    private fun typeOperatorAssignment(expression: OperatorBinary): TypeExpr? {
        val (lhsType, rhsType) = typeBinary(expression) ?: return null
        if (expression.lhs !is Assignable) {
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
