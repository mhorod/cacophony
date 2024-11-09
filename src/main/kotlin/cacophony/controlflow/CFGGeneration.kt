package cacophony.controlflow

import cacophony.semantic.FunctionAnalysisResult
import cacophony.semantic.ResolvedVariables
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
    resolvedVariables: ResolvedVariables,
    analyzedFunctions: FunctionAnalysisResult,
    analyzedUseTypes: UseTypeAnalysisResult,
    functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>,
): CFGFragment {
    val fragments = functionHandlers.map { (function, handler) ->
        generateFunctionCFG(function, handler, resolvedVariables, analyzedFunctions, analyzedUseTypes)
    }

    return fragments.flatMap { it.entries }.associate { it.toPair() }
}

fun generateFunctionCFG(
    function: Definition.FunctionDeclaration,
    functionHandler: FunctionHandler,
    resolvedVariables: ResolvedVariables,
    analyzedFunctions: FunctionAnalysisResult,
    analyzedUseTypes: UseTypeAnalysisResult,
): CFGFragment {
    val generator = CFGGenerator(resolvedVariables, analyzedFunctions, analyzedUseTypes, functionHandler)
    generator.run(function)
    return generator.getCFGFragment()
}


class CFGGenerator(
    private val resolvedVariables: ResolvedVariables,
    private val analyzedFunctions: FunctionAnalysisResult,
    private val analyzedUseTypes: UseTypeAnalysisResult,
    private val functionHandler: FunctionHandler
) {
    private val cfg = mutableMapOf<CFGLabel, CFGVertex>()

    enum class EvalMode {
        VALUE,
        CONDITION,
        SIDE_EFFECT
    }

    fun getCFGFragment(): CFGFragment {
        return cfg
    }

    fun run(expression: Definition.FunctionDeclaration) {
        val writeReturnValue = CFGNode.Assignment(Register.Fixed.RAX, visit(expression.body, EvalMode.VALUE))

        val returnLabel = addVertex(CFGVertex.Final(CFGNode.Return()))
        addVertex(CFGVertex.Jump(writeReturnValue, returnLabel))
    }


    private fun clashingSideEffects(e1: Expression, e2: Expression): Boolean {
        TODO() // use analysedUseTypes to determine if variable uses clash
    }

    private fun visit(expression: Expression, mode: EvalMode): CFGNode = when (expression) {
        is Block -> visitBlock(expression, mode)
        is Definition.FunctionArgument -> visitFunctionArgument(expression, mode)
        is Definition.FunctionDeclaration -> visitFunctionDeclaration(expression, mode)
        is Definition.VariableDeclaration -> visitVariableDeclaration(expression, mode)
        is Empty -> visitEmpty(expression, mode)
        is FunctionCall -> visitFunctionCall(expression, mode)
        is Literal -> visitLiteral(expression, mode)
        is OperatorBinary -> visitOperatorBinary(expression, mode)
        is OperatorUnary -> visitOperatorUnary(expression, mode)
        is Statement.BreakStatement -> visitBreakStatement(expression, mode)
        is Statement.IfElseStatement -> visitIfElseStatement(expression, mode)
        is Statement.ReturnStatement -> visitReturnStatement(expression, mode)
        is Statement.WhileStatement -> visitWhileStatement(expression, mode)
        is VariableUse -> visitVariableUse(expression, mode)
    }

    private fun visitBlock(expression: Block, mode: EvalMode): CFGNode {
        expression.expressions.dropLast(1).forEach { visit(it, EvalMode.SIDE_EFFECT) }
        expression.expressions.lastOrNull()?.let { visit(it, mode) }
        TODO("sus")
    }

    private fun visitFunctionArgument(expression: Definition.FunctionArgument, mode: EvalMode): CFGNode {
        TODO("Not yet implemented")
    }

    private fun addVertex(vertex: CFGVertex): CFGLabel {
        val label = CFGLabel()
        cfg[label] = vertex
        return label
    }

    private fun noOpOrUnit(mode: EvalMode): CFGNode = when (mode) {
        EvalMode.VALUE -> CFGNode.UNIT
        else -> CFGNode.NoOp()
    }

    private fun visitFunctionDeclaration(expression: Definition.FunctionDeclaration, mode: EvalMode): CFGNode {
        return noOpOrUnit(mode)
    }

    private fun visitVariableDeclaration(expression: Definition.VariableDeclaration, mode: EvalMode): CFGNode {
        val valueCFG = visit(expression.value, EvalMode.VALUE)
        val variableAccess = functionHandler.generateVariableAccess(SourceVariable(expression))
        val assignmentNode = CFGNode.MemoryWrite(CFGNode.MemoryAccess(variableAccess), valueCFG)
        return CFGNode.Sequence(listOf(assignmentNode, noOpOrUnit(mode)))
    }

    private fun visitEmpty(expr: Empty, mode: EvalMode): CFGNode = noOpOrUnit(mode)

    private fun visitFunctionCall(expression: FunctionCall, mode: EvalMode): CFGNode {
        TODO("Not yet implemented")
    }

    private fun visitLiteral(literal: Literal, mode: EvalMode): CFGNode = if (mode == EvalMode.VALUE) when (literal) {
        is Literal.BoolLiteral -> if (literal.value) CFGNode.TRUE else CFGNode.FALSE
        is Literal.IntLiteral -> CFGNode.Constant(literal.value)
    } else CFGNode.NoOp()

    private fun visitOperatorBinary(expression: OperatorBinary, mode: EvalMode): CFGNode {
        TODO("Not yet implemented")
    }

    private fun visitOperatorUnary(expression: OperatorUnary, mode: EvalMode): CFGNode {
        TODO("Not yet implemented")
    }

    private fun visitBreakStatement(expression: Statement.BreakStatement, mode: EvalMode): CFGNode {
        TODO("Not yet implemented")
    }

    private fun visitIfElseStatement(expression: Statement.IfElseStatement, mode: EvalMode): CFGNode {
        TODO("Not yet implemented")
    }

    private fun visitReturnStatement(expression: Statement.ReturnStatement, mode: EvalMode): CFGNode {
        TODO("Not yet implemented")
    }

    private fun visitWhileStatement(expression: Statement.WhileStatement, mode: EvalMode): CFGNode {
        TODO("Not yet implemented")
    }

    private fun visitVariableUse(expression: VariableUse, mode: EvalMode): CFGNode {
        TODO("Not yet implemented")
    }
}
