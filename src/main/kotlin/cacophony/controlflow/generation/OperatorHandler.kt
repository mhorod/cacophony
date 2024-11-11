package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.semantic.syntaxtree.OperatorBinary

internal class OperatorHandler(
    private val cfgGenerator: CFGGenerator,
    private val sideEffectAnalyzer: SideEffectAnalyzer,
) {
    private fun makeOperatorNode(
        op: OperatorBinary.ArithmeticOperator,
        lhs: CFGNode,
        rhs: CFGNode,
    ): CFGNode.Unconditional =
        when (op) {
            is OperatorBinary.Addition -> CFGNode.Addition(lhs, rhs)
            is OperatorBinary.Division -> CFGNode.Division(lhs, rhs)
            is OperatorBinary.Modulo -> CFGNode.Modulo(lhs, rhs)
            is OperatorBinary.Multiplication -> CFGNode.Multiplication(lhs, rhs)
            is OperatorBinary.Subtraction -> CFGNode.Subtraction(lhs, rhs)
        }

    internal fun visitArithmeticOperator(
        expression: OperatorBinary.ArithmeticOperator,
        mode: EvalMode,
    ): SubCFG =
        when (mode) {
            is EvalMode.Conditional -> error("Arithmetic operator $expression cannot be used as conditional")
            is EvalMode.SideEffect -> visitArithmeticOperatorAsSideEffects(expression)
            is EvalMode.Value -> visitArithmeticOperatorAsValue(expression)
        }

    private fun visitArithmeticOperatorAsSideEffects(expression: OperatorBinary.ArithmeticOperator): SubCFG {
        val lhs = cfgGenerator.visit(expression.lhs, EvalMode.SideEffect)
        val rhs = cfgGenerator.visit(expression.lhs, EvalMode.SideEffect)

        return if (sideEffectAnalyzer.hasClashingSideEffects(expression.lhs, expression.rhs)) {
            generateArithmeticOperatorAsNonClashingSideEffects(expression, lhs, rhs)
        } else {
            generateArithmeticOperatorAsClashingSideEffects(expression, lhs, rhs)
        }
    }

    private fun visitArithmeticOperatorAsValue(expression: OperatorBinary.ArithmeticOperator): SubCFG {
        val lhs = cfgGenerator.visit(expression.lhs, EvalMode.Value)
        val rhs = cfgGenerator.visit(expression.rhs, EvalMode.Value)

        return if (sideEffectAnalyzer.hasClashingSideEffects(expression.lhs, expression.rhs)) {
            generateArithmeticOperatorAsValueWithClashingSideEffects(expression, lhs, rhs)
        } else {
            generateArithmeticOperatorAsValueWithoutClashingSideEffects(expression, lhs, rhs)
        }
    }

    private fun generateArithmeticOperatorAsValueWithClashingSideEffects(
        op: OperatorBinary.ArithmeticOperator,
        lhs: SubCFG,
        rhs: SubCFG,
    ): SubCFG {
        // If there are clashing side effects, lhs must execute first
        val extractedLhs = cfgGenerator.ensureExtracted(lhs, EvalMode.Value)
        val exit =
            when (rhs) {
                // If rhs can be computed immediately, we don't need any more dependencies
                is SubCFG.Immediate -> extractedLhs.exit
                is SubCFG.Extracted -> {
                    // Otherwise we connect the dependencies sequentially
                    extractedLhs.exit.connect(rhs.entry.label)
                    rhs.exit
                }
            }
        return SubCFG.Extracted(extractedLhs.entry, exit, makeOperatorNode(op, extractedLhs.access, rhs.access))
    }

    private fun generateArithmeticOperatorAsValueWithoutClashingSideEffects(
        op: OperatorBinary.ArithmeticOperator,
        lhs: SubCFG,
        rhs: SubCFG,
    ): SubCFG {
        return if (lhs is SubCFG.Immediate && rhs is SubCFG.Immediate) {
            // If both lhs and rhs can be computed immediately then arithmetic operator can also be
            SubCFG.Immediate(makeOperatorNode(op, lhs.access, rhs.access))
        } else {
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
                    else -> error("Either lhs or rhs has to be extracted in this branch")
                }
            SubCFG.Extracted(entry, exit, makeOperatorNode(op, lhs.access, rhs.access))
        }
    }

    private fun generateArithmeticOperatorAsNonClashingSideEffects(
        op: OperatorBinary.ArithmeticOperator,
        lhs: SubCFG,
        rhs: SubCFG,
    ): SubCFG {
        TODO()
    }

    private fun generateArithmeticOperatorAsClashingSideEffects(
        op: OperatorBinary.ArithmeticOperator,
        lhs: SubCFG,
        rhs: SubCFG,
    ): SubCFG {
        TODO()
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
        TODO("Not yet implemented")
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
