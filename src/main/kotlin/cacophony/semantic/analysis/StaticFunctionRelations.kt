package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.*

fun findStaticFunctionRelations(ast: AST, resolvedVariables: ResolvedVariables, variablesMap: VariablesMap): StaticFunctionRelationsMap {
    val visitor = StaticFunctionsRelationsVisitor(resolvedVariables, variablesMap)
    visitor.visit(ast)
    return visitor.getRelations()
}

typealias StaticFunctionRelationsMap = Map<Definition.FunctionDefinition, StaticFunctionRelations>

data class StaticFunctionRelations(
    val parent: Definition.FunctionDefinition?,
    val staticDepth: Int,
    val declaredVariables: Set<Variable>,
    val usedVariables: Set<UsedVariable>,
)

data class UsedVariable(val variable: Variable, val type: VariableUseType)

enum class VariableUseType {
    UNUSED,
    READ,
    WRITE,
    READ_WRITE,
    ;

    fun union(other: VariableUseType): VariableUseType =
        when {
            this == UNUSED -> other
            other == UNUSED -> this
            this == other -> this
            else -> READ_WRITE
        }
}

private data class MutableStaticFunctionRelations(
    val parent: Definition.FunctionDefinition?,
    val staticDepth: Int,
    val declaredVariables: MutableSet<Variable>,
    val usedVariables: MutableSet<UsedVariable>,
) {
    fun toStaticFunctionRelations() =
        StaticFunctionRelations(
            parent,
            staticDepth,
            declaredVariables,
            usedVariables,
        )

    companion object {
        fun empty(parent: Definition.FunctionDefinition?, staticDepth: Int) =
            MutableStaticFunctionRelations(
                parent,
                staticDepth,
                mutableSetOf(),
                mutableSetOf(),
            )
    }
}

private class StaticFunctionsRelationsVisitor(
    val resolvedVariables: ResolvedVariables,
    val variablesMap: VariablesMap,
) {
    private val relations = mutableMapOf<Definition.FunctionDefinition, MutableStaticFunctionRelations>()
    private val functionStack = ArrayDeque<Definition.FunctionDefinition>()

    fun visit(ast: AST) = visitExpression(ast)

    fun getRelations(): Map<Definition.FunctionDefinition, StaticFunctionRelations> =
        relations.mapValues { it.value.toStaticFunctionRelations() }

    private fun markNestedVariables(variable: Variable, useType: VariableUseType) {
        functionStack.lastOrNull()?.let {
            relations[it]?.usedVariables?.add(UsedVariable(variable, useType))
        }
        if (variable is Variable.StructVariable) {
            variable.fields.forEach {
                markNestedVariables(it.value, useType)
            }
        }
    }

    private fun addVariableDeclaration(variable: Variable) {
        functionStack.lastOrNull()?.let {
            relations[it]?.declaredVariables?.add(variable)
        }
        if (variable is Variable.StructVariable) {
            variable.fields.values.forEach { addVariableDeclaration(it) }
        }
    }

    private fun visitExpression(expr: Expression) {
        when (expr) {
            is Block -> visitBlock(expr)
            is FieldRef.LValue -> visitFieldRefLValue(expr)
            is FieldRef.RValue -> visitFieldRefRValue(expr)
            is Definition.VariableDeclaration -> visitVariableDeclaration(expr)
            is Definition.FunctionDefinition -> visitFunctionDeclaration(expr)
            is FunctionCall -> visitFunctionCall(expr)
            is Statement.IfElseStatement -> visitIfElseStatement(expr)
            is Statement.WhileStatement -> visitWhileStatement(expr)
            is Statement.ReturnStatement -> visitReturnStatement(expr)
            is OperatorUnary -> visitUnaryOperator(expr)
            is OperatorBinary -> visitBinaryOperator(expr)
            is VariableUse -> visitVariableUse(expr)
            is Struct -> visitStruct(expr)
            else -> {
                // do nothing for expressions without nested expressions
            }
        }
    }

    private fun visitAssignable(expr: Assignable, type: VariableUseType) {
        val variable = variablesMap.lvalues[expr]!!
        // Mark all nested variables as read
        markNestedVariables(variable, type)
        // Mark all parents as read
        var nestedExpression: Expression = expr
        while (nestedExpression is Assignable) {
            functionStack.lastOrNull()?.let {
                relations[it]?.usedVariables?.add(UsedVariable(variablesMap.lvalues[nestedExpression]!!, type))
            }
            if (nestedExpression !is FieldRef) {
                break
            }
            nestedExpression = nestedExpression.struct()
        }
    }

    private fun visitVariableUse(expr: VariableUse) {
        val definition = resolvedVariables[expr]
        if (definition is Definition.FunctionDefinition)
            return
        if (functionStack.isNotEmpty())
            markNestedVariables(variablesMap.lvalues[expr]!!, VariableUseType.READ)
    }

    private fun visitBinaryOperator(expr: OperatorBinary) {
        when (expr) {
            is OperatorBinary.Assignment -> visitAssignment(expr)
            is OperatorBinary.AdditionAssignment,
            is OperatorBinary.SubtractionAssignment,
            is OperatorBinary.MultiplicationAssignment,
            is OperatorBinary.DivisionAssignment,
            is OperatorBinary.ModuloAssignment,
            -> visitCompoundAssignment(expr)

            else -> {
                visitExpression(expr.lhs)
                visitExpression(expr.rhs)
            }
        }
    }

    private fun visitAssignment(expr: OperatorBinary.Assignment) {
        if (expr.lhs is VariableUse || expr.lhs is FieldRef) {
            visitAssignable(expr.lhs as Assignable, VariableUseType.WRITE)
        } else {
            TODO("unimplemented branch for different assignment type")
//            visitExpression(expr.lhs)
        }
        visitExpression(expr.rhs)
    }

    private fun visitCompoundAssignment(expr: OperatorBinary) {
        when (expr.lhs) {
            is VariableUse -> visitVariableReadWrite(expr.lhs)
            else -> visitExpression(expr.lhs)
        }
        visitExpression(expr.rhs)
    }

    private fun visitVariableReadWrite(expr: VariableUse) {
        functionStack.lastOrNull()?.let {
            relations[it]?.usedVariables?.add(UsedVariable(variablesMap.lvalues[expr]!!, VariableUseType.READ_WRITE))
        }
    }

    private fun visitUnaryOperator(expr: OperatorUnary) = visitExpression(expr.expression)

    private fun visitReturnStatement(expr: Statement.ReturnStatement) = visitExpression(expr.value)

    private fun visitWhileStatement(expr: Statement.WhileStatement) {
        visitExpression(expr.testExpression)
        visitExpression(expr.doExpression)
    }

    private fun visitIfElseStatement(expr: Statement.IfElseStatement) {
        visitExpression(expr.testExpression)
        visitExpression(expr.doExpression)
        expr.elseExpression?.let { visitExpression(it) }
    }

    private fun visitFunctionCall(expr: FunctionCall) {
        visitExpression(expr.function)
        expr.arguments.forEach { visitExpression(it) }
    }

    private fun visitFunctionDeclaration(expr: Definition.FunctionDefinition) {
        val parent = functionStack.lastOrNull()
        val depth = parent?.let { relations[it]?.staticDepth?.let { d -> d + 1 } } ?: 0

        functionStack.addLast(expr)
        relations[expr] = MutableStaticFunctionRelations.empty(parent, depth)
        visitExpression(expr.body)
        functionStack.removeLast()
    }

    private fun visitVariableDeclaration(expr: Definition.VariableDeclaration) {
        val variable = variablesMap.definitions[expr]!!
        addVariableDeclaration(variable)
        visitExpression(expr.value)
    }

    private fun visitBlock(expr: Block) {
        expr.expressions.forEach { visitExpression(it) }
    }

    // We're not inside assignment, therefore it's read
    private fun visitFieldRefLValue(expr: FieldRef.LValue) {
        visitAssignable(expr, VariableUseType.READ)
    }

    private fun visitFieldRefRValue(expr: FieldRef.RValue) {
        visitExpression(expr.obj)
    }

    private fun visitStruct(expr: Struct) {
        expr.fields.values.forEach { visit(it) }
    }
}
