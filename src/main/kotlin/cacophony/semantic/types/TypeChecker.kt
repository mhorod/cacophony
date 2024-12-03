package cacophony.semantic.types

import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.TypeCheckerDiagnostics
import cacophony.semantic.names.ResolvedVariables
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
import cacophony.utils.Location

typealias TypeCheckingResult = Map<Expression, TypeExpr>

private val builtinTypes = BuiltinType::class.sealedSubclasses.associate { it.objectInstance!!.name to it.objectInstance!! }

// Result contains every variable that could be properly typed
fun checkTypes(ast: AST, diagnostics: Diagnostics, resolvedVariables: ResolvedVariables): TypeCheckingResult {
    val typer = Typer(diagnostics, resolvedVariables, builtinTypes)
    typer.typeExpression(ast)
    return typer.result
}

private class Typer(
    diagnostics: Diagnostics,
    val resolvedVariables: ResolvedVariables,
    val types: Map<String, TypeExpr>,
) {
    val result: MutableMap<Expression, TypeExpr> = mutableMapOf()
    val typedVariables: MutableMap<Definition, TypeExpr> = mutableMapOf()
    val functionContext = ArrayDeque<TypeExpr>()
    val error = ErrorHandler(diagnostics)
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
                    val argType = translateType(expression.type) ?: return null
                    typedVariables[expression] = argType
                    argType
                }

                is Definition.FunctionDeclaration -> {
                    // does not type anything inside if argument or result types are incorrect
                    val argsType = parseArgs(expression.arguments) ?: return null
                    val returnType = translateType(expression.returnType) ?: return null
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

                is Definition.ForeignFunctionDef -> TODO()

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
            val declaredType = translateType(type) ?: return null
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

    private fun translateType(type: Type): TypeExpr? {
        return when (type) {
            is Type.Basic ->
                types[type.identifier] ?: run {
                    error.unknownType(type.range)
                    null
                }

            is Type.Functional -> {
                val args = type.argumentsType.map { translateType(it) }
                val ret = translateType(type.returnType)
                FunctionType(
                    args.map { it ?: return null },
                    ret ?: return null,
                )
            }
        }
    }

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

// class responsible for the interaction with Diagnostics
private class ErrorHandler(
    val diagnostics: Diagnostics,
) {
    fun typeMismatchError(expected: TypeExpr, found: TypeExpr, range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.TypeMismatch(expected.toString(), found.toString()), range)
    }

    fun unknownType(range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.UnknownType, range)
    }

    fun expectedFunction(range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.ExpectedFunction, range)
    }

    fun expectedLvalue(range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.ExpectedLValueReference, range)
    }

    fun operationNotSupportedOn(operation: String, type: TypeExpr, range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.UnsupportedOperation(type.toString(), operation), range)
    }

    fun noCommonType(type1: TypeExpr, type2: TypeExpr, range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.NoCommonType(type1.toString(), type2.toString()), range)
    }

    fun returnOutsideFunction(range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.MisplacedReturn, range)
    }

    fun breakOutsideWhile(range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.BreakOutsideWhile, range)
    }
}

sealed class TypeExpr(
    val name: String,
) {
    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is TypeExpr) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    object VoidType : TypeExpr("Void")
}

sealed class BuiltinType private constructor(
    name: String,
) : TypeExpr(name) {
    object BooleanType : BuiltinType("Bool")

    object IntegerType : BuiltinType("Int")

    object UnitType : BuiltinType("Unit")
}

class FunctionType(
    val args: List<TypeExpr>,
    val result: TypeExpr,
) : TypeExpr(args.joinToString(", ", "[", "] -> ${result.name}"))

fun isSubtype(subtype: TypeExpr, type: TypeExpr): Boolean {
    if (subtype == TypeExpr.VoidType) return true
    return subtype.name == type.name
}
