package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.semantic.syntaxtree.Assignable
import cacophony.semantic.syntaxtree.OperatorBinary
import cacophony.semantic.syntaxtree.OperatorUnary

/**
 * Converts operators into CFG
 */
internal class OperatorHandler(
    private val cfg: CFG,
    private val cfgGenerator: CFGGenerator,
    private val sideEffectAnalyzer: SideEffectAnalyzer,
) {
    internal fun visitArithmeticOperator(expression: OperatorBinary.ArithmeticOperator, mode: EvalMode, context: Context): SubCFG =
        visitBinaryOperator(expression, mode, context)

    private fun visitBinaryOperator(expression: OperatorBinary, mode: EvalMode, context: Context): SubCFG {
        val lhsCFG = cfgGenerator.visit(expression.lhs, mode, context)
        val rhsCFG = cfgGenerator.visit(expression.rhs, mode, context)

        val safeLhs =
            if (sideEffectAnalyzer.hasClashingSideEffects(expression.lhs, expression.rhs)) {
                // If there are clashing side effects, lhs must be extracted to a separate vertex
                cfgGenerator.ensureExtracted(lhsCFG, mode)
            } else {
                lhsCFG
            }
        val lhsAccess = safeLhs.access
        val rhsAccess = rhsCFG.access
        // by type checking
        require(lhsAccess is SimpleLayout)
        require(rhsAccess is SimpleLayout)
        val access =
            when (mode) {
                is EvalMode.Conditional -> error("Arithmetic operator $expression cannot be used as conditional")
                is EvalMode.SideEffect -> CFGNode.NoOp
                is EvalMode.Value -> makeOperatorNode(expression, lhsAccess.access, rhsAccess.access)
            }
        return join(safeLhs, rhsCFG, access, lhsAccess.holdsReference || rhsAccess.holdsReference)
    }

    private fun makeOperatorNode(op: OperatorBinary, lhs: CFGNode, rhs: CFGNode): CFGNode =
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
    private fun join(lhs: SubCFG, rhs: SubCFG, access: CFGNode, holdsReference: Boolean): SubCFG {
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
                else -> return SubCFG.Immediate(access, holdsReference)
            }
        return SubCFG.Extracted(entry, exit, access, holdsReference)
    }

    private fun makeAssignOperatorNode(
        expression: OperatorBinary.ArithmeticAssignmentOperator,
        lhs: CFGNode.LValue,
        rhs: CFGNode,
    ): CFGNode =
        when (expression) {
            is OperatorBinary.AdditionAssignment -> CFGNode.AdditionAssignment(lhs, rhs)
            is OperatorBinary.DivisionAssignment -> CFGNode.DivisionAssignment(lhs, rhs)
            is OperatorBinary.ModuloAssignment -> CFGNode.ModuloAssignment(lhs, rhs)
            is OperatorBinary.MultiplicationAssignment -> CFGNode.MultiplicationAssignment(lhs, rhs)
            is OperatorBinary.SubtractionAssignment -> CFGNode.SubtractionAssignment(lhs, rhs)
        }

    internal fun visitArithmeticAssignmentOperator(
        expression: OperatorBinary.ArithmeticAssignmentOperator,
        mode: EvalMode,
        context: Context,
    ): SubCFG {
        val assignable =
            expression.lhs as? Assignable
                ?: error("Expected Assignable use in assignment lhs, got ${expression.lhs}")
        val lhs = cfgGenerator.visit(assignable, EvalMode.Value, context)
        val rhs = cfgGenerator.visit(expression.rhs, EvalMode.Value, context)
        val lhsAccess = lhs.access
        val rhsAccess = rhs.access
        // by type checking
        require(lhsAccess is SimpleLayout)
        require(rhsAccess is SimpleLayout)
        require(lhsAccess.access is CFGNode.LValue)
        val operatorNode = makeAssignOperatorNode(expression, lhsAccess.access, rhsAccess.access)
        return when (rhs) {
            is SubCFG.Immediate -> SubCFG.Immediate(operatorNode, lhsAccess.holdsReference)
            is SubCFG.Extracted -> {
                val assignmentVertex = cfg.addUnconditionalVertex(operatorNode)
                rhs.exit.connect(assignmentVertex.label)
                SubCFG.Extracted(rhs.entry, assignmentVertex, noOpOr(SimpleLayout(lhsAccess.access, lhsAccess.holdsReference), mode))
            }
        }
    }

    internal fun visitLogicalOperator(expression: OperatorBinary.LogicalOperator, mode: EvalMode, context: Context): SubCFG =
        when (expression) {
            is OperatorBinary.LogicalAnd -> visitLogicalAndOperator(expression, mode, context)
            is OperatorBinary.LogicalOr -> visitLogicalOrOperator(expression, mode, context)
            else -> visitNormalLogicalOperator(expression, mode, context)
        }

    private fun visitNormalLogicalOperator(expression: OperatorBinary.LogicalOperator, mode: EvalMode, context: Context): SubCFG {
        // If we are computing logical operator in Conditional mode then we want to compute both sides
        // and then perform jump basing on the operation on their values.
        val innerMode = if (mode is EvalMode.Conditional) EvalMode.Value else mode
        val valueCFG = visitBinaryOperator(expression, innerMode, context)
        val valueAccess = valueCFG.access
        // by type checking
        require(valueAccess is SimpleLayout)
        return when (mode) {
            is EvalMode.Conditional -> {
                val conditionVertex = cfg.addConditionalVertex(valueAccess.access)
                val entry =
                    if (valueCFG is SubCFG.Extracted) {
                        valueCFG.exit.connect(conditionVertex.label)
                        valueCFG.entry
                    } else {
                        conditionVertex
                    }
                conditionVertex.connectTrue(mode.trueEntry.label)
                conditionVertex.connectFalse(mode.falseEntry.label)
                SubCFG.Extracted(entry, mode.exit, CFGNode.NoOp, false)
            }

            else -> valueCFG
        }
    }

    private fun visitLogicalOrOperator(expression: OperatorBinary.LogicalOr, mode: EvalMode, context: Context): SubCFG {
        val rhs = cfgGenerator.visitExtracted(expression.rhs, mode, context)
        return when (mode) {
            is EvalMode.Conditional -> {
                // In conditional mode lhs || rhs translates to:
                // if lhs then Jump(trueLabel) else (if rhs then Jump(trueLabel) else Jump(falseLabel))

                val innerMode = EvalMode.Conditional(mode.trueEntry, rhs.entry, mode.exit)
                val lhs = cfgGenerator.visitExtracted(expression.lhs, innerMode, context)
                SubCFG.Extracted(lhs.entry, mode.exit, CFGNode.NoOp, false)
            }

            is EvalMode.Value -> {
                // In value mode lhs || rhs translates to
                // (if lhs then (reg = TRUE) else (reg = rhs)) -> reg

                val exit = cfg.addUnconditionalVertex(CFGNode.NoOp)

                val access = CFGNode.RegisterUse(Register.VirtualRegister(), false)
                val writeTrue = cfg.addUnconditionalVertex(CFGNode.Assignment(access, CFGNode.TRUE))
                writeTrue.connect(exit.label)

                val extendedRhs = cfgGenerator.extendWithAssignment(rhs, SimpleLayout(access, false), EvalMode.Value)
                extendedRhs.exit.connect(exit.label)

                val innerMode = EvalMode.Conditional(writeTrue, extendedRhs.entry, exit)
                val lhs = cfgGenerator.visitExtracted(expression.lhs, innerMode, context)
                SubCFG.Extracted(lhs.entry, exit, access, false)
            }

            is EvalMode.SideEffect -> {
                // In side effect mode lhs || rhs translates to
                // (if lhs then Jump(exit) else rhs)

                val exit = cfg.addUnconditionalVertex(CFGNode.NoOp)
                rhs.exit.connect(exit.label)

                val innerMode = EvalMode.Conditional(exit, rhs.entry, exit)
                val lhs = cfgGenerator.visitExtracted(expression.lhs, innerMode, context)
                SubCFG.Extracted(lhs.entry, exit, CFGNode.NoOp, false)
            }
        }
    }

    private fun visitLogicalAndOperator(expression: OperatorBinary.LogicalAnd, mode: EvalMode, context: Context): SubCFG {
        val rhs = cfgGenerator.visitExtracted(expression.rhs, mode, context)
        return when (mode) {
            is EvalMode.Conditional -> {
                // In conditional mode lhs && rhs translates to:
                // if lhs then (if rhs then Jump(trueLabel) else Jump(falseLabel)) else Jump(falseLabel)

                val innerMode = EvalMode.Conditional(rhs.entry, mode.falseEntry, mode.exit)
                val lhs = cfgGenerator.visitExtracted(expression.lhs, innerMode, context)
                SubCFG.Extracted(lhs.entry, mode.exit, CFGNode.NoOp, false)
            }

            is EvalMode.Value -> {
                // In value mode lhs && rhs translates to
                // (if lhs then (reg = rhs) else (reg = FALSE)) -> reg

                val exit = cfg.addUnconditionalVertex(CFGNode.NoOp)

                val access = CFGNode.RegisterUse(Register.VirtualRegister(), false)
                val writeFalse = cfg.addUnconditionalVertex(CFGNode.Assignment(access, CFGNode.FALSE))
                writeFalse.connect(exit.label)

                val extendedRhs = cfgGenerator.extendWithAssignment(rhs, SimpleLayout(access, false), EvalMode.Value)
                extendedRhs.exit.connect(exit.label)

                val innerMode = EvalMode.Conditional(extendedRhs.entry, writeFalse, exit)
                val lhs = cfgGenerator.visitExtracted(expression.lhs, innerMode, context)
                SubCFG.Extracted(lhs.entry, exit, access, false)
            }

            is EvalMode.SideEffect -> {
                // In side effect mode lhs && rhs translates to
                // (if lhs then rhs else Jump(exit))

                val exit = cfg.addUnconditionalVertex(CFGNode.NoOp)
                rhs.exit.connect(exit.label)

                val innerMode = EvalMode.Conditional(rhs.entry, exit, exit)
                val lhs = cfgGenerator.visitExtracted(expression.lhs, innerMode, context)
                SubCFG.Extracted(lhs.entry, exit, CFGNode.NoOp, false)
            }
        }
    }

    internal fun visitMinusOperator(expression: OperatorUnary.Minus, mode: EvalMode, context: Context): SubCFG =
        when (mode) {
            is EvalMode.Conditional -> error("Minus is not a conditional operator")
            is EvalMode.SideEffect -> cfgGenerator.visit(expression.expression, EvalMode.SideEffect, context)
            is EvalMode.Value -> {
                val expressionCFG = cfgGenerator.visit(expression.expression, EvalMode.Value, context)
                val expressionAccess = expressionCFG.access
                // by type checking
                require(expressionAccess is SimpleLayout)
                when (expressionCFG) {
                    is SubCFG.Immediate -> SubCFG.Immediate(CFGNode.Minus(expressionAccess.access), false)
                    is SubCFG.Extracted ->
                        SubCFG.Extracted(
                            expressionCFG.entry,
                            expressionCFG.exit,
                            CFGNode.Minus(expressionAccess.access),
                            false,
                        )
                }
            }
        }

    internal fun visitNegationOperator(expression: OperatorUnary.Negation, mode: EvalMode, context: Context): SubCFG =
        when (mode) {
            is EvalMode.Conditional ->
                cfgGenerator.visit(
                    expression.expression,
                    EvalMode.Conditional(mode.falseEntry, mode.trueEntry, mode.exit),
                    context,
                )

            is EvalMode.SideEffect -> cfgGenerator.visit(expression.expression, EvalMode.SideEffect, context)
            is EvalMode.Value -> {
                val expressionCFG = cfgGenerator.visit(expression.expression, EvalMode.Value, context)
                val expressionAccess = expressionCFG.access
                // by type checking
                require(expressionAccess is SimpleLayout)
                when (expressionCFG) {
                    is SubCFG.Immediate -> SubCFG.Immediate(CFGNode.LogicalNot(expressionAccess.access), false)
                    is SubCFG.Extracted ->
                        SubCFG.Extracted(
                            expressionCFG.entry,
                            expressionCFG.exit,
                            CFGNode.LogicalNot(expressionAccess.access),
                            false,
                        )
                }
            }
        }
}
