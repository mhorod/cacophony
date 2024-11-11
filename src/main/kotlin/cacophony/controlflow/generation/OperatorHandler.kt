package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.semantic.syntaxtree.OperatorBinary

internal class OperatorHandler(
    private val cfg: CFG,
    private val cfgGenerator: CFGGenerator,
    private val sideEffectAnalyzer: SideEffectAnalyzer,
) {
    internal fun visitArithmeticOperator(
        expression: OperatorBinary.ArithmeticOperator,
        mode: EvalMode,
    ): SubCFG = visitBinaryOperator(expression, mode)

    private fun visitBinaryOperator(
        expression: OperatorBinary,
        mode: EvalMode,
    ): SubCFG {
        val lhsCFG = cfgGenerator.visit(expression.lhs, mode)
        val rhsCFG = cfgGenerator.visit(expression.rhs, mode)
        val access =
            when (mode) {
                is EvalMode.Conditional -> error("Arithmetic operator $expression cannot be used as conditional")
                is EvalMode.SideEffect -> CFGNode.NoOp
                is EvalMode.Value -> makeOperatorNode(expression, lhsCFG.access, rhsCFG.access)
            }
        val safeLhs =
            if (sideEffectAnalyzer.hasClashingSideEffects(expression.lhs, expression.rhs)) {
                // If there are clashing side effects, lhs must be extracted to a separate vertex
                cfgGenerator.ensureExtracted(lhsCFG, mode)
            } else {
                lhsCFG
            }
        return join(safeLhs, rhsCFG, access)
    }

    private fun makeOperatorNode(
        op: OperatorBinary,
        lhs: CFGNode,
        rhs: CFGNode,
    ): CFGNode.Unconditional =
        when (op) {
            is OperatorBinary.Addition -> CFGNode.Addition(lhs, rhs)
            is OperatorBinary.Division -> CFGNode.Division(lhs, rhs)
            is OperatorBinary.Modulo -> CFGNode.Modulo(lhs, rhs)
            is OperatorBinary.Multiplication -> CFGNode.Multiplication(lhs, rhs)
            is OperatorBinary.Subtraction -> CFGNode.Subtraction(lhs, rhs)
            is OperatorBinary.Equals -> CFGNode.Equals(lhs, rhs)
            is OperatorBinary.Greater -> CFGNode.Greater(lhs, rhs)
            is OperatorBinary.GreaterEqual -> CFGNode.GreaterEqual(lhs, rhs)
            is OperatorBinary.Less -> CFGNode.Less(lhs, rhs)
            is OperatorBinary.LessEqual -> CFGNode.LessEqual(lhs, rhs)
            is OperatorBinary.NotEquals -> CFGNode.NotEquals(lhs, rhs)
            is OperatorBinary.LogicalAnd -> error("LogicalAnd operator has to be handled separately")
            is OperatorBinary.LogicalOr -> error("LogicalOr operator has to be handled separately")
            else -> error("Other operators have to be handled separately")
        }

    /**
     * Joins `lhs` and `rhs` into one sequence of vertices with the provided `access` as the result value computation
     */
    private fun join(
        lhs: SubCFG,
        rhs: SubCFG,
        access: CFGNode.Unconditional,
    ): SubCFG {
        val (entry, exit) =
            when {
                lhs is SubCFG.Extracted && rhs is SubCFG.Extracted -> {
                    // If both lhs and rhs are extracted to separate subgraphs, we connect those subgraphs sequentially
                    lhs.exit.connect(rhs.entry.label)
                    Pair(lhs.entry, rhs.exit)
                }
                // If only one of lhs and rhs is extracted to a separate subgraph then we only need it as a dependency (exit and entry)
                // and the other one is computed immediately when computing the result
                lhs is SubCFG.Extracted -> Pair(lhs.entry, lhs.exit)
                rhs is SubCFG.Extracted -> Pair(rhs.entry, rhs.exit)
                else -> return SubCFG.Immediate(access)
            }
        return SubCFG.Extracted(entry, exit, access)
    }

    internal fun visitArithmeticAssignmentOperator(
        expression: OperatorBinary.ArithmeticAssignmentOperator,
        mode: EvalMode,
    ): SubCFG {
        TODO()
    }

    internal fun visitLogicalOperator(
        expression: OperatorBinary.LogicalOperator,
        mode: EvalMode,
    ): SubCFG {
        return when (expression) {
            is OperatorBinary.LogicalAnd -> visitLogicalAndOperator(expression, mode)
            is OperatorBinary.LogicalOr -> visitLogicalOrOperator(expression, mode)
            else -> visitNormalLogicalOperator(expression, mode)
        }
    }

    private fun visitNormalLogicalOperator(
        expression: OperatorBinary.LogicalOperator,
        mode: EvalMode,
    ): SubCFG {
        val valueCFG = visitBinaryOperator(expression, mode)
        return when (mode) {
            is EvalMode.Conditional -> {
                val conditionVertex = cfg.addConditionalVertex(valueCFG.access)
                if (valueCFG is SubCFG.Extracted) {
                    valueCFG.exit.connect(conditionVertex.label)
                }
                conditionVertex.connectTrue(mode.trueCFG.entry.label)
                conditionVertex.connectFalse(mode.falseCFG.entry.label)
                SubCFG.Extracted(conditionVertex, mode.exit, CFGNode.NoOp)
            }
            else -> valueCFG
        }
    }

    private fun visitLogicalOrOperator(
        expression: OperatorBinary.LogicalOr,
        mode: EvalMode,
    ): SubCFG {
        if (mode is EvalMode.Conditional) {
            // In conditional mode lhs || rhs translates to:
            // if lhs then (if rhs then Jump(trueLabel) else Jump(falseLabel)) else Jump(falseLabel)

            val rhs = cfgGenerator.visitExtracted(expression.rhs, mode) // branch if lhs is false
            val innerMode = EvalMode.Conditional(mode.trueCFG, rhs, mode.exit)
            val lhs = cfgGenerator.visitExtracted(expression.lhs, innerMode)
            return SubCFG.Extracted(lhs.entry, mode.exit, CFGNode.NoOp)
        }
        return TODO()
    }

    private fun visitLogicalAndOperator(
        expression: OperatorBinary.LogicalAnd,
        mode: EvalMode,
    ): SubCFG {
        if (mode is EvalMode.Conditional) {
            // In conditional mode lhs && rhs translates to:
            // if lhs then (if rhs then Jump(trueLabel) else Jump(falseLabel)) else Jump(falseLabel)

            val rhs = cfgGenerator.visitExtracted(expression.rhs, mode) // branch if lhs is true
            val innerMode = EvalMode.Conditional(rhs, mode.falseCFG, mode.exit)
            val lhs = cfgGenerator.visitExtracted(expression.lhs, innerMode)
            return SubCFG.Extracted(lhs.entry, mode.exit, CFGNode.NoOp)
        }
        TODO()
    }
}
