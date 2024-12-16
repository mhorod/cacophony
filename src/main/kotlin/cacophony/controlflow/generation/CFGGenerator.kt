package cacophony.controlflow.generation

import cacophony.controlflow.*
import cacophony.controlflow.functions.CallGenerator
import cacophony.controlflow.functions.FunctionHandler
import cacophony.semantic.analysis.UseTypeAnalysisResult
import cacophony.semantic.names.ResolvedVariables
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

/**
 * Converts Expressions into CFG
 */
internal class CFGGenerator(
    private val resolvedVariables: ResolvedVariables,
    analyzedUseTypes: UseTypeAnalysisResult, // TODO: adjust to new specification of analyzedUseTypes
    private val function: Definition.FunctionDefinition,
    private val functionHandlers: Map<Definition.FunctionDefinition, FunctionHandler>,
    private val callGenerator: CallGenerator,
) {
    private val cfg = CFG()
    private val sideEffectAnalyzer = SideEffectAnalyzer(analyzedUseTypes)
    private val operatorHandler = OperatorHandler(cfg, this, sideEffectAnalyzer)
    private val assignmentHandler = AssignmentHandler(cfg, this)
    private val prologue = listOfNodesToExtracted(getCurrentFunctionHandler().generatePrologue())
    private val epilogue = listOfNodesToExtracted(getCurrentFunctionHandler().generateEpilogue())

    internal fun generateFunctionCFG(): CFGFragment {
        val bodyCFG = visit(function.body, EvalMode.Value, Context(null))
        val returnValueRegister = registerUse(getCurrentFunctionHandler().getResultRegister())

        val extended =
            when (bodyCFG) {
                is SubCFG.Extracted -> extendWithAssignment(bodyCFG, returnValueRegister, EvalMode.Value)
                is SubCFG.Immediate -> {
                    val node = CFGNode.Assignment(returnValueRegister, bodyCFG.access)
                    val vertex = cfg.addUnconditionalVertex(node)
                    SubCFG.Extracted(vertex, vertex, returnValueRegister)
                }
            }

        val returnVertex = cfg.addFinalVertex(CFGNode.Return)

        prologue.exit.connect(extended.entry.label)
        extended.exit.connect(epilogue.entry.label)
        epilogue.exit.connect(returnVertex.label)

        return cfg.cfgFragment(prologue.entry.label)
    }

    private fun listOfNodesToExtracted(nodes: List<CFGNode>): SubCFG.Extracted =
        nodes
            .map {
                val vertex = cfg.addUnconditionalVertex(it)
                SubCFG.Extracted(vertex, vertex, noOpOrUnit(EvalMode.SideEffect))
            }.reduce(SubCFG.Extracted::merge)

    internal fun ensureExtracted(subCFG: SubCFG, mode: EvalMode): SubCFG.Extracted =
        when (subCFG) {
            is SubCFG.Extracted -> subCFG
            is SubCFG.Immediate ->
                when (mode) {
                    is EvalMode.Value -> {
                        val register = Register.VirtualRegister()
                        val tmpWrite = CFGNode.Assignment(CFGNode.RegisterUse(register), subCFG.access)
                        val tmpRead = CFGNode.RegisterUse(register)
                        val vertex = cfg.addUnconditionalVertex(tmpWrite)
                        SubCFG.Extracted(vertex, vertex, tmpRead)
                    }

                    else -> {
                        val vertex = cfg.addUnconditionalVertex(subCFG.access)
                        SubCFG.Extracted(vertex, vertex, CFGNode.NoOp)
                    }
                }
        }

    private fun ensureExtracted(node: CFGNode): SubCFG.Extracted = ensureExtracted(SubCFG.Immediate(node), EvalMode.SideEffect)

    /**
     * Convert the expression into SubCFG extracted to a separate vertex
     */
    internal fun visitExtracted(expression: Expression, mode: EvalMode, context: Context) =
        ensureExtracted(visit(expression, mode, context), mode)

    /**
     * Convert the expression into SubCFG
     *
     * @param expression Expression to be converted
     * @param mode Mode of conversion, see [EvalMode]
     */
    internal fun visit(expression: Expression, mode: EvalMode, context: Context): SubCFG =
        // TODO: change to Layout
        when (expression) {
            is Block -> visitBlock(expression, mode, context)
            is Definition.FunctionDeclaration -> visitFunctionDeclaration(mode)
            is Definition.VariableDeclaration -> visitVariableDeclaration(expression, mode, context)
            is Empty -> visitEmpty(mode)
            is FunctionCall -> visitFunctionCall(expression, mode, context)
            is Literal -> visitLiteral(expression, mode)
            is OperatorBinary -> visitOperatorBinary(expression, mode, context)
            is OperatorUnary -> visitOperatorUnary(expression, mode, context)
            is Statement.BreakStatement -> visitBreakStatement(mode, context)
            is Statement.IfElseStatement -> visitIfElseStatement(expression, mode, context)
            is Statement.ReturnStatement -> visitReturnStatement(expression, mode, context)
            is Statement.WhileStatement -> visitWhileStatement(expression, mode, context)
            is VariableUse -> visitVariableUse(expression, mode)
            else -> error("Unexpected expression for CFG generation: $expression")
        }

    private fun visitBlock(expression: Block, mode: EvalMode, context: Context): SubCFG {
        return if (expression.expressions.isEmpty()) {
            SubCFG.Immediate(noOpOrUnit(mode))
        } else if (expression.expressions.size == 1) {
            visit(expression.expressions.first(), mode, context)
        } else {
            val last = expression.expressions.lastOrNull() ?: return SubCFG.Immediate(noOpOrUnit(mode))
            val prerequisiteSubCFGs =
                expression.expressions
                    .dropLast(1)
                    .map { visitExtracted(it, EvalMode.SideEffect, context) }
            val valueCFG = visit(last, mode, context)
            val reduced = prerequisiteSubCFGs.reduce(SubCFG.Extracted::merge)

            when (valueCFG) {
                is SubCFG.Extracted -> reduced merge valueCFG
                is SubCFG.Immediate -> SubCFG.Extracted(reduced.entry, reduced.exit, valueCFG.access)
            }
        }
    }

    private fun visitFunctionDeclaration(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitVariableDeclaration(expression: Definition.VariableDeclaration, mode: EvalMode, context: Context) =
        assignmentHandler.generateAssignment(expression, expression.value, mode, context, false)

    private fun visitEmpty(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitFunctionCall(expression: FunctionCall, mode: EvalMode, context: Context): SubCFG {
        val argumentVertices =
            expression.arguments
                .map { visitExtracted(it, EvalMode.Value, context) }

        val function = resolvedVariables[expression.function] as Definition.FunctionDeclaration
        val functionHandler =
            when (function) {
                is Definition.FunctionDefinition -> getFunctionHandler(function)
                is Definition.ForeignFunctionDeclaration -> null
            }

        val (resultRegister, resultAccess) =
            if (mode is EvalMode.SideEffect) {
                Pair(null, CFGNode.NoOp)
            } else {
                val register = Register.VirtualRegister()
                val rawAccess = CFGNode.RegisterUse(register)
                val access = if (mode is EvalMode.Conditional) CFGNode.NotEquals(rawAccess, CFGNode.ConstantKnown(0)) else rawAccess
                Pair(register, access)
            }

        val callSequence =
            callGenerator.generateCallFrom(
                getCurrentFunctionHandler(),
                function,
                functionHandler,
                argumentVertices.map { it.access },
                resultRegister,
            ).map { ensureExtracted(it) }
                .reduce(SubCFG.Extracted::merge)

        val entry =
            if (argumentVertices.isNotEmpty()) {
                val extractedArguments = argumentVertices.reduce(SubCFG.Extracted::merge)
                extractedArguments.exit.connect(callSequence.entry.label)
                extractedArguments.entry
            } else {
                callSequence.entry
            }

        return if (mode is EvalMode.Conditional) {
            val conditionVertex = cfg.addConditionalVertex(resultAccess)
            callSequence.exit.connect(conditionVertex.label)
            conditionVertex.connectTrue(mode.trueEntry.label)
            conditionVertex.connectFalse(mode.falseEntry.label)
            SubCFG.Extracted(conditionVertex, mode.exit, CFGNode.NoOp)
        } else SubCFG.Extracted(entry, callSequence.exit, resultAccess)
    }

    private fun visitLiteral(literal: Literal, mode: EvalMode): SubCFG =
        when (mode) {
            is EvalMode.Value ->
                SubCFG.Immediate(
                    when (literal) {
                        is Literal.BoolLiteral -> if (literal.value) CFGNode.TRUE else CFGNode.FALSE
                        is Literal.IntLiteral -> CFGNode.ConstantKnown(literal.value)
                    },
                )

            is EvalMode.SideEffect -> SubCFG.Immediate(CFGNode.NoOp)

            is EvalMode.Conditional -> {
                check(literal is Literal.BoolLiteral) { "Non-boolean literal cannot be used as a condition" }

                val target = if (literal.value) mode.trueEntry else mode.falseEntry
                SubCFG.Extracted(target, mode.exit, CFGNode.NoOp)
            }
        }

    private fun visitAssignment(expression: OperatorBinary.Assignment, mode: EvalMode, context: Context): SubCFG {
        val variableUse =
            expression.lhs as? VariableUse
                ?: error("Expected variable use in assignment lhs, got ${expression.lhs}")
        val definition = resolvedVariables[variableUse] ?: error("Unresolved variable $variableUse")
        return assignmentHandler.generateAssignment(definition, expression.rhs, mode, context, true)
    }

    private fun visitOperatorBinary(expression: OperatorBinary, mode: EvalMode, context: Context): SubCFG =
        when (expression) {
            is OperatorBinary.Assignment -> visitAssignment(expression, mode, context)
            is OperatorBinary.ArithmeticOperator -> operatorHandler.visitArithmeticOperator(expression, mode, context)
            is OperatorBinary.ArithmeticAssignmentOperator ->
                operatorHandler.visitArithmeticAssignmentOperator(expression, mode, context)

            is OperatorBinary.LogicalOperator -> operatorHandler.visitLogicalOperator(expression, mode, context)
        }

    private fun visitOperatorUnary(expression: OperatorUnary, mode: EvalMode, context: Context): SubCFG =
        when (expression) {
            is OperatorUnary.Negation -> operatorHandler.visitNegationOperator(expression, mode, context)
            is OperatorUnary.Minus -> operatorHandler.visitMinusOperator(expression, mode, context)
        }

    private fun visitIfElseStatement(expression: Statement.IfElseStatement, mode: EvalMode, context: Context): SubCFG {
        if (expression.testExpression is Literal.BoolLiteral) {
            return shortenTrivialIfStatement(
                expression,
                mode,
                context,
            )
        }

        val resultValueRegister = CFGNode.RegisterUse(Register.VirtualRegister())
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

        val conditionalCFG =
            visit(expression.testExpression, EvalMode.Conditional(trueCFG.entry, falseCFG.entry, exit), context)

        return if (mode !is EvalMode.Value) {
            conditionalCFG
        } else {
            val extractedConditionalCFG = ensureExtracted(conditionalCFG, mode)
            SubCFG.Extracted(extractedConditionalCFG.entry, extractedConditionalCFG.exit, resultValueRegister)
        }
    }

    internal fun extendWithAssignment(subCFG: SubCFG, destination: CFGNode.LValue, mode: EvalMode): SubCFG.Extracted =
        when (mode) {
            is EvalMode.Value -> {
                val writeResultNode = CFGNode.Assignment(destination, subCFG.access)
                val writeResultVertex = cfg.addUnconditionalVertex(writeResultNode)
                val entry =
                    if (subCFG is SubCFG.Extracted) {
                        subCFG.exit.connect(writeResultVertex.label)
                        subCFG.entry
                    } else {
                        writeResultVertex
                    }
                SubCFG.Extracted(entry, writeResultVertex, destination)
            }

            else -> ensureExtracted(subCFG, mode)
        }

    private fun shortenTrivialIfStatement(expression: Statement.IfElseStatement, mode: EvalMode, context: Context): SubCFG {
        check(expression.testExpression is Literal.BoolLiteral) { "Expected testExpression to be BoolLiteral" }

        return if (expression.testExpression.value) {
            visit(expression.doExpression, mode, context)
        } else {
            expression.elseExpression?.let {
                visit(it, mode, context)
            } ?: SubCFG.Immediate(CFGNode.NoOp)
        }
    }

    private fun visitReturnStatement(expression: Statement.ReturnStatement, mode: EvalMode, context: Context): SubCFG {
        val valueCFG = visit(expression.value, EvalMode.Value, context)
        val resultAssignment =
            CFGNode.Assignment(CFGNode.RegisterUse(getCurrentFunctionHandler().getResultRegister()), valueCFG.access)

        val resultAssignmentVertex = cfg.addUnconditionalVertex(resultAssignment)
        resultAssignmentVertex.connect(epilogue.entry.label)

        // Similarly to break, return creates an artificial exit
        val artificialExit = if (mode is EvalMode.Conditional) mode.exit else cfg.addUnconditionalVertex(CFGNode.NoOp)

        val entry =
            when (valueCFG) {
                is SubCFG.Extracted -> {
                    valueCFG.exit.connect(resultAssignmentVertex.label)
                    valueCFG.entry
                }
                is SubCFG.Immediate -> resultAssignmentVertex
            }

        return SubCFG.Extracted(entry, artificialExit, CFGNode.NoOp)
    }

    private fun visitWhileStatement(expression: Statement.WhileStatement, mode: EvalMode, context: Context): SubCFG {
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

    private fun visitBreakStatement(mode: EvalMode, context: Context): SubCFG {
        check(context.currentLoopExit != null) { "Break has to be inside while loop" }
        val vertex = cfg.addUnconditionalVertex(CFGNode.NoOp)
        vertex.connect(context.currentLoopExit.label)
        // Break "breaks" control flow by jumping to a given label.
        // This means there's no real exit from the break statement, so we introduce an unreachable one
        //  that can be connected to expressions following the break statement
        val artificialExit = if (mode is EvalMode.Conditional) mode.exit else cfg.addUnconditionalVertex(CFGNode.NoOp)
        return SubCFG.Extracted(vertex, artificialExit, CFGNode.NoOp)
    }

    private fun visitVariableUse(expression: VariableUse, mode: EvalMode): SubCFG {
        val definition = resolvedVariables[expression] ?: error("Unresolved variable $expression")
        val variableAccess =
            getCurrentFunctionHandler().generateVariableAccess(Variable.SourceVariable(definition))
        return when (mode) {
            is EvalMode.Value -> SubCFG.Immediate(variableAccess)
            is EvalMode.SideEffect -> SubCFG.Immediate(CFGNode.NoOp)
            is EvalMode.Conditional -> {
                val conditionVertex =
                    cfg.addConditionalVertex(
                        CFGNode.NotEquals(
                            variableAccess,
                            CFGNode.ConstantKnown(0),
                        ),
                    )
                conditionVertex.connectTrue(mode.trueEntry.label)
                conditionVertex.connectFalse(mode.falseEntry.label)
                SubCFG.Extracted(conditionVertex, mode.exit, CFGNode.NoOp)
            }
        }
    }

    internal fun getCurrentFunctionHandler(): FunctionHandler = getFunctionHandler(function)

    internal fun resolveVariable(variable: VariableUse) = resolvedVariables[variable] ?: error("Unresolved variable $variable")

    private fun getFunctionHandler(function: Definition.FunctionDefinition): FunctionHandler =
        functionHandlers[function] ?: error("Function $function has no handler")
}
