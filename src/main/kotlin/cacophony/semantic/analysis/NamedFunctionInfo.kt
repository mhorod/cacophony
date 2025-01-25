package cacophony.semantic.analysis

import cacophony.semantic.syntaxtree.*

/**
 * For each lambda expression that was defined as part of syntactically static function store this function
 */
typealias NamedFunctionInfo = Map<LambdaExpression, Definition.FunctionDefinition>

private class GetNamedFunctionsVisitor {
    private val accum: MutableMap<LambdaExpression, Definition.FunctionDefinition> = mutableMapOf()

    fun getResult(): NamedFunctionInfo = accum.toMap()

    fun visit(ast: AST) {
        when (ast) {
            is LeafExpression -> return
            is Dereference -> visit(ast.value)
            is FieldRef.LValue -> visit(ast.obj)
            is Allocation -> visit(ast.value)
            is Block -> ast.children().forEach { visit(it) }
            is Definition.FunctionDefinition -> {
                accum[ast.value] = ast
                visit(ast.value)
            }
            is Definition.VariableDefinition -> visit(ast.value)
            is FieldRef.RValue -> visit(ast.obj)
            is FunctionCall -> {
                visit(ast.function)
                ast.arguments.forEach { visit(it) }
            }
            is LambdaExpression -> visit(ast.body)
            is OperatorBinary -> {
                visit(ast.lhs)
                visit(ast.rhs)
            }
            is OperatorUnary -> {
                visit(ast.expression)
            }
            is Statement.IfElseStatement -> {
                visit(ast.testExpression)
                visit(ast.doExpression)
                ast.elseExpression?.let { visit(it) }
            }
            is Statement.ReturnStatement -> visit(ast.value)
            is Statement.WhileStatement -> {
                visit(ast.testExpression)
                visit(ast.doExpression)
            }
            is Struct -> {
                ast.fields.forEach { visit(it.value) }
            }
        }
    }
}

fun getNamedFunctions(ast: AST): NamedFunctionInfo {
    val visitor = GetNamedFunctionsVisitor()
    visitor.visit(ast)
    return visitor.getResult()
}
