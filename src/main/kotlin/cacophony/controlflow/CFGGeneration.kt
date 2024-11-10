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
    val fragments = functionHandlers.map { (function, _) ->
        generateFunctionCFG(function, functionHandlers, resolvedVariables, analyzedFunctions, analyzedUseTypes)
    }

    return fragments.flatMap { it.entries }.associate { it.toPair() }
}

fun generateFunctionCFG(
    function: Definition.FunctionDeclaration,
    functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>,
    resolvedVariables: ResolvedVariables,
    analyzedFunctions: FunctionAnalysisResult,
    analyzedUseTypes: UseTypeAnalysisResult,
): CFGFragment {
    val generator = CFGGenerator(resolvedVariables, analyzedFunctions, analyzedUseTypes, function, functionHandlers)
    generator.run(function)
    return generator.getCFGFragment()
}


class CFGGenerator(
    private val resolvedVariables: ResolvedVariables,
    private val analyzedFunctions: FunctionAnalysisResult,
    private val analyzedUseTypes: UseTypeAnalysisResult,
    private val function: Definition.FunctionDeclaration,
    private val functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>
) {
    private class GeneralCFGVertex(val node: CFGNode, val label: CFGLabel) {
        val outgoing = mutableListOf<CFGLabel>()

        fun connect(vararg labels: CFGLabel) {
            outgoing.addAll(labels)
        }
    }

    private val cfg = mutableMapOf<CFGLabel, GeneralCFGVertex>()

    /**
     * Subgraph of Control Flow Graph created from a certain AST subtree
     * @property access Pure access to the value produced by the graph
     */
    private sealed interface SubCFG {
        val access: CFGNode.Unconditional

        /**
         * Indicates that no control flow vertex was created during expression translation
         */
        data class Immediate(override val access: CFGNode.Unconditional) : SubCFG

        /**
         * Indicates that expression was translated to a graph of CFG vertices.
         *
         * @property entry Entry point to the graph i.e. the first vertex that should be executed before others
         * @property exit Exit point of the graph i.e. the last vertex that should be executed after others
         */
        data class Extracted(
            val entry: GeneralCFGVertex,
            val exit: GeneralCFGVertex,
            override val access: CFGNode.Unconditional
        ) :
            SubCFG
    }

    private sealed interface EvalMode {
        data object Value : EvalMode
        data object SideEffect : EvalMode
        data class Conditional(val trueLabel: CFGLabel, val falseLabel: CFGLabel) : EvalMode
    }

    fun getCFGFragment(): CFGFragment {
        TODO("pepela")
    }

    fun run(expression: Definition.FunctionDeclaration) {
//        val writeReturnValue = CFGNode.Assignment(Register.Fixed.RAX, visit(expression.body, EvalMode.VALUE))
//        val returnLabel = addVertex(CFGVertex.Final(CFGNode.Return()))
//        addVertex(CFGVertex.Jump(writeReturnValue, returnLabel))
        TODO("sus")
    }


    private fun hasClashingSideEffects(e1: Expression, e2: Expression): Boolean {
        TODO() // use analysedUseTypes to determine if variable uses clash
    }

    private fun hasSideEffects(expression: Expression): Boolean = TODO()

    private fun addVertex(node: CFGNode): GeneralCFGVertex {
        val vertex = GeneralCFGVertex(node, CFGLabel())
        cfg[vertex.label] = vertex
        return vertex
    }

    private fun ensureExtracted(subCFG: SubCFG, mode: EvalMode): SubCFG.Extracted = when (subCFG) {
        is SubCFG.Extracted -> subCFG
        is SubCFG.Immediate -> when (mode) {
            is EvalMode.Value -> {
                val register = Register.Virtual()
                val tmpWrite = CFGNode.Assignment(CFGNode.VariableUse(register), subCFG.access)
                val tmpRead = CFGNode.VariableUse(register)
                val vertex = addVertex(tmpWrite)
                SubCFG.Extracted(vertex, vertex, tmpRead)
            }

            else -> {
                val vertex = addVertex(subCFG.access)
                SubCFG.Extracted(vertex, vertex, CFGNode.NoOp)
            }
        }
    }

    private fun noOpOrUnit(mode: EvalMode): CFGNode.Unconditional =
        if (mode is EvalMode.Value) CFGNode.UNIT else CFGNode.NoOp

    private fun visit(expression: Expression, mode: EvalMode, dependentLabel: CFGLabel): SubCFG = when (expression) {
        is Block -> visitBlock(expression, mode, dependentLabel)
        is Definition.FunctionDeclaration -> visitFunctionDeclaration(mode)
        is Definition.VariableDeclaration -> visitVariableDeclaration(expression, mode, dependentLabel)
        is Empty -> visitEmpty(mode)
        is FunctionCall -> visitFunctionCall(expression, mode, dependentLabel)
        is Literal -> visitLiteral(expression, mode, dependentLabel)
        is OperatorBinary -> visitOperatorBinary(expression, mode, dependentLabel)
        is OperatorUnary -> visitOperatorUnary(expression, mode, dependentLabel)
        is Statement.BreakStatement -> visitBreakStatement(expression, mode, dependentLabel)
        is Statement.IfElseStatement -> visitIfElseStatement(expression, mode, dependentLabel)
        is Statement.ReturnStatement -> visitReturnStatement(expression, mode, dependentLabel)
        is Statement.WhileStatement -> visitWhileStatement(expression, mode, dependentLabel)
        is VariableUse -> visitVariableUse(expression, mode, dependentLabel)
        else -> throw IllegalStateException("Unexpected expression for CFG generation: $expression")
    }

    private fun merge(lhs: SubCFG.Extracted, rhs: SubCFG.Extracted): SubCFG.Extracted {
        lhs.exit.connect(rhs.entry.label)
        return SubCFG.Extracted(lhs.entry, rhs.exit, rhs.access)
    }

    private fun visitBlock(expression: Block, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
        val last = expression.expressions.lastOrNull() ?: return SubCFG.Immediate(noOpOrUnit(mode))
        val prerequisiteSubCFGs = expression.expressions.dropLast(1)
            .filter { hasSideEffects(it) }
            .map { ensureExtracted(visit(it, EvalMode.SideEffect, dependentLabel), EvalMode.SideEffect) }
        val valueCFG = ensureExtracted(visit(last, mode, dependentLabel), mode)
        return prerequisiteSubCFGs.foldRight(valueCFG) { subCFG, path -> merge(subCFG, path) }
    }

    private fun visitFunctionDeclaration(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitVariableDeclaration(
        expression: Definition.VariableDeclaration,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
        val variableAccess = functionHandlers[function]!!.generateVariableAccess(SourceVariable(expression))
        val valueCFG = visit(expression.value, EvalMode.Value, dependentLabel)
        val variableWrite = CFGNode.Assignment(variableAccess, valueCFG.access)
        return when (valueCFG) {
            is SubCFG.Immediate -> SubCFG.Immediate(variableWrite)
            is SubCFG.Extracted -> {
                val writeVertex = addVertex(variableWrite)
                SubCFG.Extracted(valueCFG.entry, writeVertex, noOpOrUnit(mode))
            }
        }
    }

    private fun visitEmpty(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitFunctionCall(expression: FunctionCall, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
        val nodes = expression.arguments
            .map { ensureExtracted(visit(it, EvalMode.Value, dependentLabel), EvalMode.Value) }
            .reduceRight { subCFG, path -> merge(subCFG, path) }

//        val call = functionHandler.ge
    }

    private fun visitLiteral(literal: Literal, mode: EvalMode, dependentLabel: CFGLabel): SubCFG = SubCFG.Immediate(
        if (mode == EvalMode.Value) when (literal) {
            is Literal.BoolLiteral -> if (literal.value) CFGNode.TRUE else CFGNode.FALSE
            is Literal.IntLiteral -> CFGNode.Constant(literal.value)
        } else CFGNode.NoOp
    )

    private fun visitOperatorBinary(expression: OperatorBinary, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
//
//        // lhs -> (  op(lhs_access, rhs))
//        if (hasClashingSideEffects(expression.lhs, expression.rhs)) {
//            val rhs = visit(expression.rhs, EvalMode.Value, dependentLabel)
//
//            val lhsDependentLabel = if (rhs is SubCFG.Extracted)
//                rhs.entry
//            else
//                dependentLabel
//
//            val lhs = visit(expression.lhs, EvalMode.Value, lhsDependentLabel)
//            when (lhs) {
//                is SubCFG.Immediate -> CFGVertex.Jump(lhs.access, lhsDependentLabel)
//                is SubCFG.Extracted ->
//                    cfg[lhs.exit] = CFGVertex.Jump(CFGNode.NoOp, lhsDependentLabel)
//            }
//        }
        TODO()
    }

    private fun visitOperatorUnary(expression: OperatorUnary, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
        TODO("Not yet implemented")
    }

    private fun visitBreakStatement(
        expression: Statement.BreakStatement,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
//        val exitLabel = CFGLabel()
//        val entryLabel = addVertex(CFGVertex.Jump(CFGNode.NoOp, exitLabel))
//        return SubCFG.Extracted(entryLabel, exitLabel, CFGNode.NoOp)
        TODO()
    }

    private fun visitIfElseStatement(
        expression: Statement.IfElseStatement,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
//        if (expression.testExpression is Literal.BoolLiteral)
//            return shortenTrivialIfStatement(expression, mode, dependentLabel)
//
//        val entryLabel = CFGLabel()
//        val trueLabel = CFGLabel()
//        val falseLabel = CFGLabel()
//        val exitLabel = CFGLabel()
//
//        val condition = visit(expression.testExpression, EvalMode.Conditional(trueLabel, falseLabel), entryLabel)
//
//        val trueSubCFG = visit(expression.doExpression, mode, trueLabel)
//        val falseSubCFG =
//            expression.elseExpression?.let { visit(it, mode, falseLabel) } ?: SubCFG.Immediate(CFGNode.NoOp)
//
//        val trueVertex = CFGVertex.Jump(trueSubCFG.access, exitLabel)
//        val falseVertex = CFGVertex.Jump(falseSubCFG.access, exitLabel)
//        cfg[trueLabel] = trueVertex
//        cfg[falseLabel] = falseVertex
//
//        return SubCFG.Extracted(entryLabel, exitLabel, condition.access)
        TODO()
    }

    private fun shortenTrivialIfStatement(
        expression: Statement.IfElseStatement,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
        if (expression.testExpression !is Literal.BoolLiteral)
            throw IllegalStateException("Expected testExpression to be BoolLiteral")

        return if (expression.testExpression.value)
            visit(expression.doExpression, mode, dependentLabel)
        else expression.elseExpression?.let {
            visit(it, mode, dependentLabel)
        } ?: SubCFG.Immediate(CFGNode.NoOp)
    }

    private fun visitReturnStatement(
        expression: Statement.ReturnStatement,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
        val valueCFG = visit(expression.value, EvalMode.Value, dependentLabel)
        val resultAssignment = CFGNode.Assignment(CFGNode.VariableUse(Register.Fixed.RAX), valueCFG.access)
        val returnSequence = CFGNode.Sequence(listOf(resultAssignment, CFGNode.Return))
        return SubCFG.Immediate(returnSequence)
    }

    private fun visitWhileStatement(
        expression: Statement.WhileStatement,
        mode: EvalMode,
        dependentLabel: CFGLabel
    ): SubCFG {
        TODO("Not yet implemented")
    }

    private fun visitVariableUse(expression: VariableUse, mode: EvalMode, dependentLabel: CFGLabel): SubCFG {
        val definition = resolvedVariables[expression] ?: throw IllegalStateException("Unresolved variable $expression")
        val variableAccess = functionHandlers[function]!!.generateVariableAccess(SourceVariable(definition))
        return SubCFG.Immediate(variableAccess)
    }
}
