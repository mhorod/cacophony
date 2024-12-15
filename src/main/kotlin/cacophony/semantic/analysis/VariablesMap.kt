package cacophony.semantic.analysis

import cacophony.controlflow.Variable
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Assignable
import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression
import cacophony.semantic.syntaxtree.FieldRef
import cacophony.semantic.syntaxtree.FunctionCall
import cacophony.semantic.syntaxtree.LeafExpression
import cacophony.semantic.syntaxtree.OperatorBinary
import cacophony.semantic.syntaxtree.OperatorUnary
import cacophony.semantic.syntaxtree.Statement
import cacophony.semantic.syntaxtree.Struct
import cacophony.semantic.syntaxtree.VariableUse
import cacophony.semantic.types.StructType
import cacophony.semantic.types.TypeCheckingResult
import cacophony.semantic.types.TypeExpr

data class VariablesMap(
    val lvalues: Map<Assignable, Variable>,
    val definitions: Map<Definition, Variable>,
)

fun createVariablesMap(ast: AST, resolvedVariables: ResolvedVariables, types: TypeCheckingResult): VariablesMap {
    val definitions = VariableDefinitionMapBuilder(types).build(ast)
    val lvalues = AssignableMapBuilder(resolvedVariables, definitions).build(ast)
    return VariablesMap(lvalues, definitions)
}

private class AssignableMapBuilder(val resolvedVariables: ResolvedVariables, val definitions: Map<Definition, Variable>) {
    private val lvalues = mutableMapOf<Assignable, Variable>()

    fun build(ast: AST): Map<Assignable, Variable> {
        visit(ast)
        return lvalues
    }

    fun visit(expression: Expression) {
        when (expression) {
            is FieldRef.LValue -> visitFieldRefLValue(expression)

            is VariableUse -> visitVariableUse(expression)

            is Block -> expression.expressions.forEach { visit(it) }

            is Definition.FunctionDefinition -> visit(expression.body)
            is Definition.VariableDeclaration -> visit(expression.value)

            is FieldRef.RValue -> visit(expression.obj)

            is FunctionCall -> expression.arguments.forEach { visit(it) }

            is OperatorBinary -> {
                visit(expression.lhs)
                visit(expression.rhs)
            }

            is OperatorUnary -> visit(expression.expression)

            is Statement.IfElseStatement -> {
                visit(expression.testExpression)
                visit(expression.doExpression)
                expression.elseExpression?.let { visit(it) }
            }

            is Statement.ReturnStatement -> visit(expression.value)
            is Statement.WhileStatement -> {
                visit(expression.testExpression)
                visit(expression.doExpression)
            }

            is Struct -> expression.fields.values.forEach { visit(it) }
            is LeafExpression -> {
                /* do nothing */
            }
        }
    }

    private fun visitVariableUse(expression: VariableUse) {
        val definition = resolvedVariables.getValue(expression)
        val variable = definitions.getValue(definition)
        lvalues[expression] = variable
    }

    private fun visitFieldRefLValue(expression: FieldRef.LValue) {
        visit(expression.obj)
        val variable = lvalues.getValue(expression.obj)
        val field = (variable as Variable.StructVariable).fields.getValue(expression.field)
        lvalues[expression] = field
    }
}

private class VariableDefinitionMapBuilder(val types: TypeCheckingResult) {
    private val definitions = mutableMapOf<Definition, Variable>()

    fun build(ast: AST): Map<Definition, Variable> {
        visit(ast)
        return definitions
    }

    fun visit(expression: Expression) {
        when (expression) {
            is FieldRef.LValue -> visit(expression.obj)
            is FieldRef.RValue -> visit(expression.obj)

            is Block -> expression.expressions.forEach { visit(it) }
            is Definition.FunctionArgument -> visitFunctionArgument(expression)

            is Definition.FunctionDefinition -> {
                expression.arguments.forEach { visit(it) }
                visit(expression.body)
            }
            is Definition.VariableDeclaration -> visitVariableDeclaration(expression)

            is FunctionCall -> expression.arguments.forEach { visit(it) }

            is OperatorBinary -> {
                visit(expression.lhs)
                visit(expression.rhs)
            }

            is OperatorUnary -> visit(expression.expression)

            is Statement.IfElseStatement -> {
                visit(expression.testExpression)
                visit(expression.doExpression)
                expression.elseExpression?.let { visit(it) }
            }

            is Statement.ReturnStatement -> visit(expression.value)
            is Statement.WhileStatement -> {
                visit(expression.testExpression)
                visit(expression.doExpression)
            }

            is Struct -> expression.fields.values.forEach { visit(it) }

            is LeafExpression -> {
                /* do nothing */
            }
        }
    }

    private fun visitFunctionArgument(expression: Definition.FunctionArgument) {
        val type = types.definitionTypes[expression]
        val variable = createVariable(type!!)
        definitions[expression] = variable
    }

    private fun visitVariableDeclaration(expression: Definition.VariableDeclaration) {
        visit(expression.value)
        val type = types.definitionTypes[expression]
        val variable = createVariable(type!!)
        definitions[expression] = variable
    }

    private fun createVariable(type: TypeExpr): Variable {
        return when (type) {
            is StructType -> Variable.StructVariable(type.fields.mapValues { createVariable(it.value) })
            else -> Variable.PrimitiveVariable()
        }
    }
}
