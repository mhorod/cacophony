package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.FunctionHandler
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.controlflow.X64Register
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

internal class CFGGenerator(
    private val resolvedVariables: ResolvedVariables,
    private val analyzedFunctions: FunctionAnalysisResult,
    private val analyzedUseTypes: UseTypeAnalysisResult,
    private val function: Definition.FunctionDeclaration,
    private val functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>,
) {
    private val cfg = mutableMapOf<CFGLabel, GeneralCFGVertex>()
    private val sideEffectAnalyzer = SideEffectAnalyzer(analyzedFunctions, analyzedUseTypes)
    private val operatorHandler = OperatorHandler(this, sideEffectAnalyzer)
    private val assignmentHandler = AssignmentHandler(this)

    fun getCFGFragment(): CFGFragment {
        TODO("todo")
    }

    fun run(expression: Definition.FunctionDeclaration) {
        val cfg = generateFunctionCFG(expression)
        TODO("sus")
    }

    private fun generateFunctionCFG(function: Definition.FunctionDeclaration): SubCFG {
        val bodyCFG = visit(function.body, EvalMode.Value)
        val returnValueRegister = CFGNode.VariableUse(Register.FixedRegister(X64Register.RAX))
        val extended = extendWithAssignment(bodyCFG, returnValueRegister, EvalMode.Value)
        val returnVertex = addVertex(CFGNode.Return)
        extended.exit.connect(returnVertex.label)
        return SubCFG.Extracted(extended.entry, returnVertex, returnValueRegister)
    }

    internal fun addVertex(node: CFGNode): GeneralCFGVertex {
        val vertex = GeneralCFGVertex(node, CFGLabel())
        cfg[vertex.label] = vertex
        return vertex
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
                        val vertex = addVertex(tmpWrite)
                        SubCFG.Extracted(vertex, vertex, tmpRead)
                    }

                    else -> {
                        val vertex = addVertex(subCFG.access)
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
    ) = ensureExtracted(visit(expression, mode), mode)

    /**
     * Convert the expression into SubCFG
     *
     * @param expression Expression to be converted
     * @param mode Mode of conversion, see [EvalMode]
     */
    internal fun visit(
        expression: Expression,
        mode: EvalMode,
    ): SubCFG =
        if (mode is EvalMode.SideEffect && !sideEffectAnalyzer.hasSideEffects(expression)) {
            SubCFG.Immediate(CFGNode.NoOp)
        } else {
            when (expression) {
                is Block -> visitBlock(expression, mode)
                is Definition.FunctionDeclaration -> visitFunctionDeclaration(mode)
                is Definition.VariableDeclaration -> visitVariableDeclaration(expression, mode)
                is Empty -> visitEmpty(mode)
                is FunctionCall -> visitFunctionCall(expression, mode)
                is Literal -> visitLiteral(expression, mode)
                is OperatorBinary -> visitOperatorBinary(expression, mode)
                is OperatorUnary -> visitOperatorUnary(expression, mode)
                is Statement.BreakStatement -> visitBreakStatement(expression, mode)
                is Statement.IfElseStatement -> visitIfElseStatement(expression, mode)
                is Statement.ReturnStatement -> visitReturnStatement(expression, mode)
                is Statement.WhileStatement -> visitWhileStatement(expression, mode)
                is VariableUse -> visitVariableUse(expression, mode)
                else -> error("Unexpected expression for CFG generation: $expression")
            }
        }

    private fun visitBlock(
        expression: Block,
        mode: EvalMode,
    ): SubCFG {
        val last = expression.expressions.lastOrNull() ?: return SubCFG.Immediate(noOpOrUnit(mode))
        val prerequisiteSubCFGs =
            expression.expressions.dropLast(1)
                .filter { sideEffectAnalyzer.hasSideEffects(it) }
                .map { ensureExtracted(visit(it, EvalMode.SideEffect), EvalMode.SideEffect) }
        val valueCFG = ensureExtracted(visit(last, mode), mode)
        return prerequisiteSubCFGs.foldRight(valueCFG) { subCFG, path -> subCFG merge path }
    }

    private fun visitFunctionDeclaration(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitVariableDeclaration(
        expression: Definition.VariableDeclaration,
        mode: EvalMode,
    ) = assignmentHandler.generateAssignment(expression, expression.value, mode, false)

    private fun visitEmpty(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitFunctionCall(
        expression: FunctionCall,
        mode: EvalMode,
    ): SubCFG {
        val argumentNodes =
            expression.arguments
                .map { ensureExtracted(visit(it, EvalMode.Value), EvalMode.Value) }

        val function = resolvedVariables[expression.function] as Definition.FunctionDeclaration
        val functionHandler = getFunctionHandler(function)

        val resultRegister = if (mode is EvalMode.Value) Register.VirtualRegister() else null

        val callCFG = functionHandler.generateCall(argumentNodes.map { it.access }, resultRegister)
        TODO()
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
    ): SubCFG {
        val variableUse =
            expression.lhs as? VariableUse
                ?: error("Expected variable use in assignment lhs, got ${expression.lhs}")
        val definition = resolvedVariables[variableUse] ?: error("Unresolved variable $variableUse")
        return assignmentHandler.generateAssignment(definition, expression.rhs, mode, true)
    }

    private fun visitOperatorBinary(
        expression: OperatorBinary,
        mode: EvalMode,
    ): SubCFG =
        when (expression) {
            is OperatorBinary.Assignment -> visitAssignment(expression, mode)
            is OperatorBinary.ArithmeticOperator -> operatorHandler.visitArithmeticOperator(expression, mode)
            is OperatorBinary.ArithmeticAssignmentOperator ->
                operatorHandler.visitArithmeticAssignmentOperator(
                    expression,
                    mode,
                )

            is OperatorBinary.LogicalOperator -> operatorHandler.visitLogicalOperator(expression, mode)
        }

    private fun visitOperatorUnary(
        expression: OperatorUnary,
        mode: EvalMode,
    ): SubCFG =
        when (expression) {
            is OperatorUnary.Negation -> visitNegationOperator(expression, mode)
            is OperatorUnary.Minus -> visitMinusOperator(expression, mode)
        }

    private fun visitMinusOperator(
        expression: OperatorUnary.Minus,
        mode: EvalMode,
    ): SubCFG =
        when (mode) {
            is EvalMode.Conditional -> error("Minus is not a conditional operator")
            is EvalMode.SideEffect -> visit(expression.expression, EvalMode.SideEffect)
            is EvalMode.Value -> {
                when (val expressionCFG = visit(expression.expression, EvalMode.Value)) {
                    is SubCFG.Immediate -> SubCFG.Immediate(CFGNode.Minus(expressionCFG.access))
                    is SubCFG.Extracted ->
                        SubCFG.Extracted(
                            expressionCFG.entry,
                            expressionCFG.exit,
                            CFGNode.Minus(expressionCFG.access),
                        )
                }
            }
        }

    private fun visitNegationOperator(
        expression: OperatorUnary.Negation,
        mode: EvalMode,
    ): SubCFG =
        when (mode) {
            is EvalMode.Conditional ->
                visit(
                    expression.expression,
                    EvalMode.Conditional(mode.falseCFG, mode.trueCFG, mode.exit),
                )

            is EvalMode.SideEffect -> visit(expression.expression, EvalMode.SideEffect)
            is EvalMode.Value -> {
                when (val expressionCFG = visit(expression.expression, EvalMode.Value)) {
                    is SubCFG.Immediate -> SubCFG.Immediate(CFGNode.Negation(expressionCFG.access))
                    is SubCFG.Extracted ->
                        SubCFG.Extracted(
                            expressionCFG.entry,
                            expressionCFG.exit,
                            CFGNode.Negation(expressionCFG.access),
                        )
                }
            }
        }

    private fun visitBreakStatement(
        expression: Statement.BreakStatement,
        mode: EvalMode,
    ): SubCFG {
        TODO()
    }

    private fun visitIfElseStatement(
        expression: Statement.IfElseStatement,
        mode: EvalMode,
    ): SubCFG {
        if (expression.testExpression is Literal.BoolLiteral) return shortenTrivialIfStatement(expression, mode)

        val resultValueRegister = CFGNode.VariableUse(Register.VirtualRegister())
        val trueCFG = extendWithAssignment(visit(expression.doExpression, mode), resultValueRegister, mode)
        val falseCFG =
            extendWithAssignment(
                expression.elseExpression?.let { visit(it, mode) } ?: SubCFG.Immediate(CFGNode.NoOp),
                resultValueRegister,
                mode,
            )

        val exit = addVertex(CFGNode.NoOp)
        trueCFG.exit.connect(exit.label)
        falseCFG.exit.connect(exit.label)

        val conditionalCFG = visit(expression.testExpression, EvalMode.Conditional(trueCFG, falseCFG, exit))

        return if (mode !is EvalMode.Value) {
            conditionalCFG
        } else {
            val extractedConditionalCFG = ensureExtracted(conditionalCFG, mode)
            SubCFG.Extracted(extractedConditionalCFG.entry, extractedConditionalCFG.exit, resultValueRegister)
        }
    }

    private fun extendWithAssignment(
        cfg: SubCFG,
        destination: CFGNode.LValue,
        mode: EvalMode,
    ): SubCFG.Extracted {
        val extractedCFG = ensureExtracted(cfg, mode)
        return when (mode) {
            is EvalMode.Value -> {
                val writeResultNode = CFGNode.Assignment(destination, extractedCFG.access)
                val writeResultVertex = addVertex(writeResultNode)
                extractedCFG.exit.connect(writeResultVertex.label)
                SubCFG.Extracted(extractedCFG.entry, writeResultVertex, destination)
            }

            else -> extractedCFG
        }
    }

    private fun shortenTrivialIfStatement(
        expression: Statement.IfElseStatement,
        mode: EvalMode,
    ): SubCFG {
        check(expression.testExpression is Literal.BoolLiteral) { "Expected testExpression to be BoolLiteral" }

        return if (expression.testExpression.value) {
            visit(expression.doExpression, mode)
        } else {
            expression.elseExpression?.let {
                visit(it, mode)
            } ?: SubCFG.Immediate(CFGNode.NoOp)
        }
    }

    private fun visitReturnStatement(
        expression: Statement.ReturnStatement,
        mode: EvalMode,
    ): SubCFG {
        val valueCFG = visit(expression.value, EvalMode.Value)
        val resultAssignment = CFGNode.Assignment(CFGNode.VariableUse(Register.FixedRegister(X64Register.RAX)), valueCFG.access)
        val returnSequence = CFGNode.Sequence(listOf(resultAssignment, CFGNode.Return))
        return SubCFG.Immediate(returnSequence)
    }

    private fun visitWhileStatement(
        expression: Statement.WhileStatement,
        mode: EvalMode,
    ): SubCFG {
        TODO("Not yet implemented")
    }

    private fun visitVariableUse(
        expression: VariableUse,
        mode: EvalMode,
    ): SubCFG {
        val definition = resolvedVariables[expression] ?: error("Unresolved variable $expression")
        val variableAccess =
            SubCFG.Immediate(getFunctionHandler(function).generateVariableAccess(Variable.SourceVariable(definition)))
        return when (mode) {
            is EvalMode.Value -> variableAccess
            is EvalMode.SideEffect -> SubCFG.Immediate(CFGNode.NoOp)
            is EvalMode.Conditional -> {
                val check = ensureExtracted(variableAccess, mode)
                check.exit.connect(mode.falseCFG.entry.label)
                check.exit.connect(mode.trueCFG.entry.label)
                SubCFG.Extracted(check.entry, mode.exit, CFGNode.NoOp)
            }
        }
    }

    internal fun getCurrentFunctionHandler(): FunctionHandler = getFunctionHandler(function)

    internal fun getFunctionHandler(function: Definition.FunctionDeclaration): FunctionHandler {
        return functionHandlers[function] ?: error("Function $function has no handler")
    }
}
