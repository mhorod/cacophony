package cacophony.controlflow.generation

import cacophony.controlflow.*
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

internal class CFGGenerator(
    private val resolvedVariables: ResolvedVariables,
    analyzedUseTypes: UseTypeAnalysisResult,
    private val function: Definition.FunctionDeclaration,
    private val functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>,
) {
    private val cfg = CFG()
    private val sideEffectAnalyzer = SideEffectAnalyzer(analyzedUseTypes)
    private val operatorHandler = OperatorHandler(cfg, this, sideEffectAnalyzer)
    private val assignmentHandler = AssignmentHandler(cfg, this)

    internal fun generateFunctionCFG(): CFGFragment {
        val bodyCFG = visit(function.body, EvalMode.Value, Context(null))
        val returnValueRegister = CFGNode.VariableUse(Register.FixedRegister(X64Register.RAX))
        val extended = extendWithAssignment(bodyCFG, returnValueRegister, EvalMode.Value)
        val returnVertex = cfg.addFinalVertex(CFGNode.Return)
        extended.exit.connect(returnVertex.label)
        return cfg.getCFGFragment()
    }

    internal fun ensureExtracted(
        subCFG: SubCFG,
        mode: EvalMode,
    ): SubCFG.Extracted =
        when (subCFG) {
            is SubCFG.Extracted -> subCFG
            is SubCFG.Immediate ->
                when (mode) {
                    is EvalMode.Value -> {
                        val register = Register.VirtualRegister()
                        val tmpWrite = CFGNode.Assignment(CFGNode.VariableUse(register), subCFG.access)
                        val tmpRead = CFGNode.VariableUse(register)
                        val vertex = cfg.addUnconditionalVertex(tmpWrite)
                        SubCFG.Extracted(vertex, vertex, tmpRead)
                    }

                    else -> {
                        val vertex = cfg.addUnconditionalVertex(subCFG.access)
                        SubCFG.Extracted(vertex, vertex, CFGNode.NoOp)
                    }
                }
        }

    /**
     * Convert the expression into SubCFG extracted to a separate vertex
     */
    internal fun visitExtracted(
        expression: Expression,
        mode: EvalMode,
        context: Context,
    ) = ensureExtracted(visit(expression, mode, context), mode)

    /**
     * Convert the expression into SubCFG
     *
     * @param expression Expression to be converted
     * @param mode Mode of conversion, see [EvalMode]
     */
    internal fun visit(
        expression: Expression,
        mode: EvalMode,
        context: Context,
    ): SubCFG =
        if (mode is EvalMode.SideEffect && !sideEffectAnalyzer.hasSideEffects(expression)) {
            SubCFG.Immediate(CFGNode.NoOp)
        } else {
            when (expression) {
                is Block -> visitBlock(expression, mode, context)
                is Definition.FunctionDeclaration -> visitFunctionDeclaration(mode)
                is Definition.VariableDeclaration -> visitVariableDeclaration(expression, mode, context)
                is Empty -> visitEmpty(mode)
                is FunctionCall -> visitFunctionCall(expression, mode, context)
                is Literal -> visitLiteral(expression, mode)
                is OperatorBinary -> visitOperatorBinary(expression, mode, context)
                is OperatorUnary -> visitOperatorUnary(expression, mode, context)
                is Statement.BreakStatement -> visitBreakStatement(context)
                is Statement.IfElseStatement -> visitIfElseStatement(expression, mode, context)
                is Statement.ReturnStatement -> visitReturnStatement(expression, context)
                is Statement.WhileStatement -> visitWhileStatement(expression, mode, context)
                is VariableUse -> visitVariableUse(expression, mode)
                else -> error("Unexpected expression for CFG generation: $expression")
            }
        }

    private fun visitBlock(
        expression: Block,
        mode: EvalMode,
        context: Context,
    ): SubCFG {
        val last = expression.expressions.lastOrNull() ?: return SubCFG.Immediate(noOpOrUnit(mode))
        val prerequisiteSubCFGs =
            expression.expressions.dropLast(1)
                .filter { sideEffectAnalyzer.hasSideEffects(it) }
                .map { ensureExtracted(visit(it, EvalMode.SideEffect, context), EvalMode.SideEffect) }
        val valueCFG = ensureExtracted(visit(last, mode, context), mode)
        return prerequisiteSubCFGs.foldRight(valueCFG) { subCFG, path -> subCFG merge path }
    }

    private fun visitFunctionDeclaration(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitVariableDeclaration(
        expression: Definition.VariableDeclaration,
        mode: EvalMode,
        context: Context,
    ) = assignmentHandler.generateAssignment(expression, expression.value, mode, context, false)

    private fun visitEmpty(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitFunctionCall(
        expression: FunctionCall,
        mode: EvalMode,
        context: Context,
    ): SubCFG {
        val argumentNodes =
            expression.arguments
                .map { ensureExtracted(visit(it, EvalMode.Value, context), EvalMode.Value) }

        val extractedArguments = argumentNodes.reduce { path, next -> path merge next }

        val function = resolvedVariables[expression.function] as Definition.FunctionDeclaration
        val functionHandler = getFunctionHandler(function)

        val resultRegister = if (mode is EvalMode.Value) Register.VirtualRegister() else null

        val callVertex =
            cfg.addUnconditionalVertex(
                CFGNode.Sequence(
                    functionHandler.generateCall(
                        argumentNodes.map { it.access },
                        resultRegister,
                    ),
                ),
            )

        extractedArguments.exit.connect(callVertex.label)

        val resultAccess = resultRegister?.let { CFGNode.VariableUse(it) } ?: CFGNode.NoOp
        return SubCFG.Extracted(extractedArguments.entry, callVertex, resultAccess)
    }

    private fun visitLiteral(
        literal: Literal,
        mode: EvalMode,
    ): SubCFG =
        SubCFG.Immediate(
            if (mode is EvalMode.Value) {
                when (literal) {
                    is Literal.BoolLiteral -> if (literal.value) CFGNode.TRUE else CFGNode.FALSE
                    is Literal.IntLiteral -> CFGNode.Constant(literal.value)
                }
            } else {
                CFGNode.NoOp
            },
        )

    private fun visitAssignment(
        expression: OperatorBinary.Assignment,
        mode: EvalMode,
        context: Context,
    ): SubCFG {
        val variableUse =
            expression.lhs as? VariableUse
                ?: error("Expected variable use in assignment lhs, got ${expression.lhs}")
        val definition = resolvedVariables[variableUse] ?: error("Unresolved variable $variableUse")
        return assignmentHandler.generateAssignment(definition, expression.rhs, mode, context, true)
    }

    private fun visitOperatorBinary(
        expression: OperatorBinary,
        mode: EvalMode,
        context: Context,
    ): SubCFG =
        when (expression) {
            is OperatorBinary.Assignment -> visitAssignment(expression, mode, context)
            is OperatorBinary.ArithmeticOperator -> operatorHandler.visitArithmeticOperator(expression, mode, context)
            is OperatorBinary.ArithmeticAssignmentOperator ->
                operatorHandler.visitArithmeticAssignmentOperator(expression, mode, context)
            is OperatorBinary.LogicalOperator -> operatorHandler.visitLogicalOperator(expression, mode, context)
        }

    private fun visitOperatorUnary(
        expression: OperatorUnary,
        mode: EvalMode,
        context: Context,
    ): SubCFG =
        when (expression) {
            is OperatorUnary.Negation -> operatorHandler.visitNegationOperator(expression, mode, context)
            is OperatorUnary.Minus -> operatorHandler.visitMinusOperator(expression, mode, context)
        }

    private fun visitIfElseStatement(
        expression: Statement.IfElseStatement,
        mode: EvalMode,
        context: Context,
    ): SubCFG {
        if (expression.testExpression is Literal.BoolLiteral) return shortenTrivialIfStatement(expression, mode, context)

        val resultValueRegister = CFGNode.VariableUse(Register.VirtualRegister())
        val trueCFG = extendWithAssignment(visit(expression.doExpression, mode, context), resultValueRegister, mode)
        val falseCFG =
            extendWithAssignment(
                expression.elseExpression?.let { visit(it, mode, context) } ?: SubCFG.Immediate(CFGNode.NoOp),
                resultValueRegister,
                mode,
            )

        val exit = cfg.addUnconditionalVertex(CFGNode.NoOp)
        trueCFG.exit.connect(exit.label)
        falseCFG.exit.connect(exit.label)

        val conditionalCFG = visit(expression.testExpression, EvalMode.Conditional(trueCFG.entry, falseCFG.entry, exit), context)

        return if (mode !is EvalMode.Value) {
            conditionalCFG
        } else {
            val extractedConditionalCFG = ensureExtracted(conditionalCFG, mode)
            SubCFG.Extracted(extractedConditionalCFG.entry, extractedConditionalCFG.exit, resultValueRegister)
        }
    }

    internal fun extendWithAssignment(
        subCFG: SubCFG,
        destination: CFGNode.LValue,
        mode: EvalMode,
    ): SubCFG.Extracted {
        val extractedCFG = ensureExtracted(subCFG, mode)
        return when (mode) {
            is EvalMode.Value -> {
                val writeResultNode = CFGNode.Assignment(destination, extractedCFG.access)
                val writeResultVertex = cfg.addUnconditionalVertex(writeResultNode)
                extractedCFG.exit.connect(writeResultVertex.label)
                SubCFG.Extracted(extractedCFG.entry, writeResultVertex, destination)
            }

            else -> extractedCFG
        }
    }

    private fun shortenTrivialIfStatement(
        expression: Statement.IfElseStatement,
        mode: EvalMode,
        context: Context,
    ): SubCFG {
        check(expression.testExpression is Literal.BoolLiteral) { "Expected testExpression to be BoolLiteral" }

        return if (expression.testExpression.value) {
            visit(expression.doExpression, mode, context)
        } else {
            expression.elseExpression?.let {
                visit(it, mode, context)
            } ?: SubCFG.Immediate(CFGNode.NoOp)
        }
    }

    private fun visitReturnStatement(
        expression: Statement.ReturnStatement,
        context: Context,
    ): SubCFG {
        val valueCFG = visit(expression.value, EvalMode.Value, context)
        val resultAssignment =
            CFGNode.Assignment(CFGNode.VariableUse(Register.FixedRegister(X64Register.RAX)), valueCFG.access)
        val returnSequence = CFGNode.Sequence(listOf(resultAssignment, CFGNode.Return))
        return SubCFG.Immediate(returnSequence)
    }

    private fun visitWhileStatement(
        expression: Statement.WhileStatement,
        mode: EvalMode,
        context: Context,
    ): SubCFG {
        check(mode !is EvalMode.Conditional) { "While statement cannot be used as a condition" }
        // while (condition) do (body) is translated to
        // entry: if (condition) then (body; Jump(entry)) else exit
        val exit = cfg.addUnconditionalVertex(CFGNode.NoOp)
        val body = visitExtracted(expression.doExpression, EvalMode.SideEffect, Context(exit))
        val conditionMode = EvalMode.Conditional(body.entry, exit, exit)
        val condition = visitExtracted(expression.testExpression, conditionMode, context)
        body.exit.connect(condition.entry.label)
        return SubCFG.Extracted(condition.entry, exit, noOpOrUnit(mode))
    }

    private fun visitBreakStatement(context: Context): SubCFG {
        check(context.currentLoopExit != null) { "Break has to be inside while loop" }
        val vertex = cfg.addUnconditionalVertex(CFGNode.NoOp)

        vertex.connect(context.currentLoopExit.label)
        return SubCFG.Extracted(vertex, vertex, CFGNode.NoOp)
    }

    private fun visitVariableUse(
        expression: VariableUse,
        mode: EvalMode,
    ): SubCFG {
        val definition = resolvedVariables[expression] ?: error("Unresolved variable $expression")
        val variableAccess =
            getFunctionHandler(function).generateVariableAccess(Variable.SourceVariable(definition))
        return when (mode) {
            is EvalMode.Value -> SubCFG.Immediate(variableAccess)
            is EvalMode.SideEffect -> SubCFG.Immediate(CFGNode.NoOp)
            is EvalMode.Conditional -> {
                val conditionVertex = cfg.addConditionalVertex(variableAccess)
                conditionVertex.connectTrue(mode.trueEntry.label)
                conditionVertex.connectFalse(mode.falseEntry.label)
                SubCFG.Extracted(conditionVertex, mode.exit, CFGNode.NoOp)
            }
        }
    }

    internal fun getCurrentFunctionHandler(): FunctionHandler = getFunctionHandler(function)

    internal fun resolveVariable(variable: VariableUse) = resolvedVariables[variable] ?: error("Unresolved variable $variable")

    private fun getFunctionHandler(function: Definition.FunctionDeclaration): FunctionHandler {
        return functionHandlers[function] ?: error("Function $function has no handler")
    }
}
