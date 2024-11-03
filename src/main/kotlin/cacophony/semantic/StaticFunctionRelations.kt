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

fun findStaticFunctionRelations(ast: AST): StaticFunctionRelationsMap {
    val visitor = StaticFunctionsRelationsVisitor()
    visitor.visit(ast)
    return visitor.getRelations()
}

typealias StaticFunctionRelationsMap = Map<Definition.FunctionDeclaration, StaticFunctionRelations>

data class StaticFunctionRelations(
    val parent: Definition.FunctionDeclaration?,
    val staticDepth: Int,
    val declaredVariables: Set<Definition.VariableDeclaration>,
    val usedVariables: Set<UsedVariable>,
)

data class UsedVariable(val variable: VariableUse, val type: VariableUseType)

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
    val parent: Definition.FunctionDeclaration?,
    val staticDepth: Int,
    val declaredVariables: MutableSet<Definition.VariableDeclaration>,
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
        fun empty(
            parent: Definition.FunctionDeclaration?,
            staticDepth: Int,
        ) = MutableStaticFunctionRelations(
            parent,
            staticDepth,
            mutableSetOf(),
            mutableSetOf(),
        )
    }
}

private class StaticFunctionsRelationsVisitor {
    private val relations = mutableMapOf<Definition.FunctionDeclaration, MutableStaticFunctionRelations>()
    private val functionStack = ArrayDeque<Definition.FunctionDeclaration>()

    fun visit(ast: AST) {
        ast.expressions.forEach { visitExpression(it) }
    }

    fun getRelations(): Map<Definition.FunctionDeclaration, StaticFunctionRelations> =
        relations.mapValues { it.value.toStaticFunctionRelations() }

    private fun visitExpression(expr: Expression) {
        when (expr) {
            is Block -> visitBlock(expr)
            is Definition.VariableDeclaration -> visitVariableDeclaration(expr)
            is Definition.FunctionDeclaration -> visitFunctionDeclaration(expr)
            is FunctionCall -> visitFunctionCall(expr)
            is Statement.IfElseStatement -> visitIfElseStatement(expr)
            is Statement.WhileStatement -> visitWhileStatement(expr)
            is Statement.ReturnStatement -> visitReturnStatement(expr)
            is OperatorUnary -> visitUnaryOperator(expr)
            is OperatorBinary -> visitBinaryOperator(expr)
            is VariableUse -> visitVariableUse(expr)
            else -> {
                // do nothing for expressions without nested expressions
            }
        }
    }

    private fun visitVariableUse(expr: VariableUse) {
        functionStack.lastOrNull()?.let {
            relations[it]?.usedVariables?.add(UsedVariable(expr, VariableUseType.READ))
        }
    }

    private fun visitBinaryOperator(expr: OperatorBinary) {
        when (expr) {
            is OperatorBinary.Assignment -> visitAssignment(expr)
            else -> {
                visitExpression(expr.lhs)
                visitExpression(expr.rhs)
            }
        }
    }

    private fun visitAssignment(expr: OperatorBinary.Assignment) {
        when (expr.lhs) {
            is VariableUse -> visitVariableWrite(expr.lhs)
            else -> visitExpression(expr.lhs)
        }
    }

    private fun visitVariableWrite(expr: VariableUse) {
        functionStack.lastOrNull()?.let {
            relations[it]?.usedVariables?.add(UsedVariable(expr, VariableUseType.WRITE))
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

    private fun visitFunctionDeclaration(expr: Definition.FunctionDeclaration) {
        val parent = functionStack.lastOrNull()
        val depth = parent?.let { relations[it]?.staticDepth?.let { d -> d + 1 } } ?: 0

        functionStack.addLast(expr)
        relations[expr] = MutableStaticFunctionRelations.empty(parent, depth)
        visitExpression(expr.body)
        functionStack.removeLast()
    }

    private fun visitVariableDeclaration(expr: Definition.VariableDeclaration) {
        functionStack.lastOrNull()?.let {
            relations[it]?.declaredVariables?.add(expr)
        }
        visitExpression(expr.value)
    }

    private fun visitBlock(expr: Block) {
        expr.expressions.forEach { visitExpression(it) }
    }
}
