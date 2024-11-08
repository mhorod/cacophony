package cacophony.controlflow

import cacophony.semantic.FunctionAnalysisResult
import cacophony.semantic.UseTypeAnalysisResult
import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Empty
import cacophony.semantic.syntaxtree.Expression
import cacophony.semantic.syntaxtree.FunctionCall
import cacophony.semantic.syntaxtree.Literal
import cacophony.semantic.syntaxtree.OperatorBinary
import cacophony.semantic.syntaxtree.OperatorUnary
import cacophony.semantic.syntaxtree.Statement
import cacophony.semantic.syntaxtree.VariableUse

fun generateCFG(
    root: Expression,
    analyzedFunctions: FunctionAnalysisResult,
    analyzedUseTypes: UseTypeAnalysisResult,
): CFGFragment {
    val generator = CFGGenerator(analyzedFunctions, analyzedUseTypes)
    generator.visit(root)
    return generator.getCFGFragment()
}

class CFGGenerator(
    private val analyzedFunctions: FunctionAnalysisResult,
    private val analyzedUseTypes: UseTypeAnalysisResult,
) {
    private val cfg = mutableMapOf<CFGLabel, CFGVertex>()

    fun getCFGFragment(): CFGFragment {
        return cfg
    }

    fun visit(expression: Expression) {
        when (expression) {
            is Block -> visitBlock(expression)
            is Definition.FunctionArgument -> visitFunctionArgument(expression)
            is Definition.FunctionDeclaration -> visitFunctionDeclaration(expression)
            is Definition.VariableDeclaration -> visitVariableDeclaration(expression)
            is Empty -> visitEmpty(expression)
            is FunctionCall -> visitFunctionCall(expression)
            is Literal -> visitLiteral(expression)
            is OperatorBinary -> visitOperatorBinary(expression)
            is OperatorUnary -> visitOperatorUnary(expression)
            is Statement.BreakStatement -> visitBreakStatement(expression)
            is Statement.IfElseStatement -> visitIfElseStatement(expression)
            is Statement.ReturnStatement -> visitReturnStatement(expression)
            is Statement.WhileStatement -> visitWhileStatement(expression)
            is VariableUse -> visitVariableUse(expression)
        }
    }

    private fun visitBlock(expression: Block) {
        TODO("Not yet implemented")
    }

    private fun visitFunctionArgument(expression: Definition.FunctionArgument) {
        TODO("Not yet implemented")
    }

    private fun visitFunctionDeclaration(expression: Definition.FunctionDeclaration) {
        TODO("Not yet implemented")
    }

    private fun visitVariableDeclaration(expression: Definition.VariableDeclaration) {
        TODO("Not yet implemented")
    }

    private fun visitEmpty(expression: Empty) {
        TODO("Not yet implemented")
    }

    private fun visitFunctionCall(expression: FunctionCall) {
        TODO("Not yet implemented")
    }

    private fun visitLiteral(expression: Literal) {
        TODO("Not yet implemented")
    }

    private fun visitOperatorBinary(expression: OperatorBinary) {
        TODO("Not yet implemented")
    }

    private fun visitOperatorUnary(expression: OperatorUnary) {
        TODO("Not yet implemented")
    }

    private fun visitBreakStatement(expression: Statement.BreakStatement) {
        TODO("Not yet implemented")
    }

    private fun visitIfElseStatement(expression: Statement.IfElseStatement) {
        TODO("Not yet implemented")
    }

    private fun visitReturnStatement(expression: Statement.ReturnStatement) {
        TODO("Not yet implemented")
    }

    private fun visitWhileStatement(expression: Statement.WhileStatement) {
        TODO("Not yet implemented")
    }

    private fun visitVariableUse(expression: VariableUse) {
        TODO("Not yet implemented")
    }
}
