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
import kotlin.math.exp

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

    private sealed interface SubCFG {
        val access: CFGNode.Unconditional

        data class Immediate(override val access: CFGNode.Unconditional) : SubCFG
        data class Extracted(val entry: CFGLabel, val exit: CFGLabel, override val access: CFGNode.Unconditional) :
            SubCFG
    }

    private sealed interface EvalMode {
        data object Value : EvalMode
        data object SideEffect : EvalMode
        data class Conditional(val trueLabel: CFGLabel, val falseLabel: CFGLabel) : EvalMode
    }

    fun getCFGFragment(): CFGFragment {
        return cfg
    }

    fun run(expression: Definition.FunctionDeclaration) {
//        val writeReturnValue = CFGNode.Assignment(Register.Fixed.RAX, visit(expression.body, EvalMode.VALUE))
//        val returnLabel = addVertex(CFGVertex.Final(CFGNode.Return()))
//        addVertex(CFGVertex.Jump(writeReturnValue, returnLabel))
        TODO("sus")
    }


    private fun clashingSideEffects(e1: Expression, e2: Expression): Boolean {
        TODO() // use analysedUseTypes to determine if variable uses clash
    }

    private fun visit(expression: Expression, mode: EvalMode, dependentLabel: CFGLabel): SubCFG = when (expression) {
        is Block -> visitBlock(expression, mode, dependentLabel)
        is Definition.FunctionArgument -> visitFunctionArgument(expression, mode, dependentLabel)
        is Definition.FunctionDeclaration -> visitFunctionDeclaration(expression, mode, dependentLabel)
        is Definition.VariableDeclaration -> visitVariableDeclaration(expression, mode, dependentLabel)
        is Empty -> visitEmpty(expression, mode, dependentLabel)
        is FunctionCall -> visitFunctionCall(expression, mode, dependentLabel)
        is Literal -> visitLiteral(expression, mode, dependentLabel)
        is OperatorBinary -> visitOperatorBinary(expression, mode, dependentLabel)
        is OperatorUnary -> visitOperatorUnary(expression, mode, dependentLabel)
        is Statement.BreakStatement -> visitBreakStatement(expression, mode, dependentLabel)
        is Statement.IfElseStatement -> visitIfElseStatement(expression, mode, dependentLabel)
        is Statement.ReturnStatement -> visitReturnStatement(expression, mode, dependentLabel)
        is Statement.WhileStatement -> visitWhileStatement(expression, mode, dependentLabel)
        is VariableUse -> visitVariableUse(expression, mode, dependentLabel)
    }

    private fun visitBlock(expression: Block, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
        expression.expressions.dropLast(1).forEach { visit(it, EvalMode.SideEffect, dependentLabel) }
        expression.expressions.lastOrNull()?.let { visit(it, mode, dependentLabel) }
        TODO("sus")
    }

    private fun visitFunctionArgument(
        expression: Definition.FunctionArgument,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
        TODO("Not yet implemented")
    }

    private fun addVertex(vertex: CFGVertex): CFGLabel {
        val label = CFGLabel()
        cfg[label] = vertex
        return label
    }

    private fun noOpOrUnit(mode: EvalMode): CFGNode.Unconditional =
        if (mode is EvalMode.Value) CFGNode.UNIT else CFGNode.NoOp

    private fun visitFunctionDeclaration(
        expression: Definition.FunctionDeclaration,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG =
        SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitVariableDeclaration(
        expression: Definition.VariableDeclaration,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
        val valueCFG = visit(expression.value, EvalMode.Value, dependentLabel)
        if (valueCFG is SubCFG.Extracted)
            cfg[valueCFG.exit] = CFGVertex.Jump(CFGNode.NoOp, dependentLabel)

        val variableAccess = functionHandler.generateVariableAccess(SourceVariable(expression))
        val variableWrite = CFGNode.Assignment(variableAccess, valueCFG.access)
        return SubCFG.Immediate(CFGNode.Sequence(listOf(variableWrite, noOpOrUnit(mode))))
    }

    private fun visitEmpty(expr: Empty, mode: EvalMode, dependentLabel: CFGLabel): SubCFG =
        SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitFunctionCall(expression: FunctionCall, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
        TODO("Not yet implemented")
    }

    private fun visitLiteral(literal: Literal, mode: EvalMode, dependentLabel: CFGLabel): SubCFG = SubCFG.Immediate(
        if (mode == EvalMode.Value) when (literal) {
            is Literal.BoolLiteral -> if (literal.value) CFGNode.TRUE else CFGNode.FALSE
            is Literal.IntLiteral -> CFGNode.Constant(literal.value)
        } else CFGNode.NoOp
    )

    private fun visitOperatorBinary(expression: OperatorBinary, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
        TODO("Not yet implemented")
    }

    private fun visitOperatorUnary(expression: OperatorUnary, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
        TODO("Not yet implemented")
    }

    private fun visitBreakStatement(
        expression: Statement.BreakStatement,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
        TODO("Not yet implemented")
    }

    private fun visitIfElseStatement(
        expression: Statement.IfElseStatement,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
        if (expression.testExpression is Literal.BoolLiteral)
            return shortenTrivialIfStatement(expression, mode, dependentLabel)

        val entryLabel = CFGLabel()
        val trueLabel = CFGLabel()
        val falseLabel = CFGLabel()
        val exitLabel = CFGLabel()

        val condition = visit(expression.testExpression, EvalMode.Conditional(trueLabel, falseLabel), entryLabel)

        val trueSubCFG = visit(expression.doExpression, mode, trueLabel)
        val falseSubCFG =
            expression.elseExpression?.let { visit(it, mode, falseLabel) } ?: SubCFG.Immediate(CFGNode.NoOp)

        val trueVertex = CFGVertex.Jump(trueSubCFG.access, exitLabel)
        val falseVertex = CFGVertex.Jump(falseSubCFG.access, exitLabel)
        cfg[trueLabel] = trueVertex
        cfg[falseLabel] = falseVertex

        return SubCFG.Extracted(entryLabel, exitLabel, condition.access)
    }

    private fun shortenTrivialIfStatement(expression: Statement.IfElseStatement, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
        if (expression.testExpression !is Literal.BoolLiteral)
            throw IllegalStateException("Expected testExpression to be BoolLiteral")

        return if (expression.testExpression.value)
            visit(expression.doExpression, mode, dependentLabel)
        else expression.elseExpression?.let {
            visit(
                it,
                mode,
                dependentLabel
            )
        } ?: SubCFG.Immediate(CFGNode.NoOp)
    }

    private fun visitReturnStatement(
        expression: Statement.ReturnStatement,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
        TODO("Not yet implemented")
    }

    private fun visitWhileStatement(
        expression: Statement.WhileStatement,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
        TODO("Not yet implemented")
    }

    private fun visitVariableUse(expression: VariableUse, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
        TODO("Not yet implemented")
    }
}
