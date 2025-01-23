package cacophony.semantic.analysis

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.LambdaExpression

typealias NamedFunctionInfo = Map<LambdaExpression, Definition.VariableDeclaration>

fun getNamedFunctions(ast: AST): NamedFunctionInfo {
    TODO("Implement")
}
