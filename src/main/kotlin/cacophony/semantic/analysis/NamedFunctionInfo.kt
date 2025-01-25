package cacophony.semantic.analysis

import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.LambdaExpression

/**
 * For each lambda expression that was defined as part of syntactically static function store this function
 */
typealias NamedFunctionInfo = Map<LambdaExpression, Definition.FunctionDefinition>

fun getNamedFunctions(ast: AST): NamedFunctionInfo {
    TODO("Implement")
}
