package cacophony.semantic.syntaxtree

fun declarationIsFunctionDeclaration(declaration: Definition.VariableDeclaration): Boolean = (declaration.value is LambdaExpression)
