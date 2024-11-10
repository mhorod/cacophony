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
    val fragments =
        functionHandlers.map { (function, _) ->
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
    private val functionHandlers: Map<Definition.FunctionDeclaration, FunctionHandler>,
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
            override val access: CFGNode.Unconditional,
        ) :
            SubCFG
    }

    private sealed interface EvalMode {
        data object Value : EvalMode

        data object SideEffect : EvalMode

        data class Conditional(val trueCFG: SubCFG.Extracted, val falseCFG: SubCFG.Extracted) : EvalMode
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

    private fun hasClashingSideEffects(
        e1: Expression,
        e2: Expression,
    ): Boolean {
        TODO() // use analysedUseTypes to determine if variable uses clash
    }

    private fun hasSideEffects(expression: Expression): Boolean = TODO()

    private fun addVertex(node: CFGNode): GeneralCFGVertex {
        val vertex = GeneralCFGVertex(node, CFGLabel())
        cfg[vertex.label] = vertex
        return vertex
    }

    private fun ensureExtracted(
        subCFG: SubCFG,
        mode: EvalMode,
    ): SubCFG.Extracted =
        when (subCFG) {
            is SubCFG.Extracted -> subCFG
            is SubCFG.Immediate ->
                when (mode) {
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

    private fun visit(
        expression: Expression,
        mode: EvalMode,
    ): SubCFG =
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

    private fun merge(
        lhs: SubCFG.Extracted,
        rhs: SubCFG.Extracted,
    ): SubCFG.Extracted {
        lhs.exit.connect(rhs.entry.label)
        return SubCFG.Extracted(lhs.entry, rhs.exit, rhs.access)
    }

    private fun visitBlock(
        expression: Block,
        mode: EvalMode,
    ): SubCFG {
        val last = expression.expressions.lastOrNull() ?: return SubCFG.Immediate(noOpOrUnit(mode))
        val prerequisiteSubCFGs =
            expression.expressions.dropLast(1)
                .filter { hasSideEffects(it) }
                .map { ensureExtracted(visit(it, EvalMode.SideEffect), EvalMode.SideEffect) }
        val valueCFG = ensureExtracted(visit(last, mode), mode)
        return prerequisiteSubCFGs.foldRight(valueCFG) { subCFG, path -> merge(subCFG, path) }
    }

    private fun visitFunctionDeclaration(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitVariableDeclaration(
        expression: Definition.VariableDeclaration,
        mode: EvalMode,
    ) = generateAssignment(expression, expression.value, mode, false)

    private fun visitEmpty(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitFunctionCall(
        expression: FunctionCall,
        mode: EvalMode,
    ): SubCFG {
        val argumentNodes =
            expression.arguments
                .map { ensureExtracted(visit(it, EvalMode.Value), EvalMode.Value) }

        val function = resolvedVariables[expression.function] as Definition.FunctionDeclaration
        val functionHandler = functionHandlers[function]!!

        val resultRegister = if (mode is EvalMode.Value) Register.Virtual() else null

        val callCFG = functionHandler.generateCall(argumentNodes.map { it.access }, resultRegister)
        TODO()
    }

    private fun visitLiteral(
        literal: Literal,
        mode: EvalMode,
    ): SubCFG =
        SubCFG.Immediate(
            if (mode == EvalMode.Value) {
                when (literal) {
                    is Literal.BoolLiteral -> if (literal.value) CFGNode.TRUE else CFGNode.FALSE
                    is Literal.IntLiteral -> CFGNode.Constant(literal.value)
                }
            } else {
                CFGNode.NoOp
            },
        )

    private fun makeOperatorNode(
        op: OperatorBinary.ArithmeticOperator,
        lhs: CFGNode,
        rhs: CFGNode,
    ): CFGNode.Unconditional = when (op) {
        is OperatorBinary.Addition -> CFGNode.Addition(lhs, rhs)
        is OperatorBinary.Division -> CFGNode.Division(lhs, rhs)
        is OperatorBinary.Modulo -> CFGNode.Modulo(lhs, rhs)
        is OperatorBinary.Multiplication -> CFGNode.Multiplication(lhs, rhs)
        is OperatorBinary.Subtraction -> CFGNode.Subtraction(lhs, rhs)
    }

    private fun joinedEntry(
        lhs: SubCFG,
        rhs: SubCFG,
    ): GeneralCFGVertex {
        return when {
            lhs is SubCFG.Extracted && rhs is SubCFG.Extracted -> {
                lhs.exit.connect(rhs.entry.label)
                lhs.entry
            }

            lhs is SubCFG.Extracted -> lhs.entry
            rhs is SubCFG.Extracted -> rhs.entry
            else -> error("Unexpected state")
        }
    }

    private fun noOpOr(
        value: CFGNode.Unconditional,
        mode: EvalMode,
    ): CFGNode.Unconditional = if (mode is EvalMode.Value) value else CFGNode.NoOp

    private fun noOpOrUnit(mode: EvalMode): CFGNode.Unconditional = noOpOr(CFGNode.UNIT, mode)

    private fun generateAssignment(
        variable: Definition,
        value: Expression,
        mode: EvalMode,
        propagate: Boolean,
    ): SubCFG {
        val variableAccess =
            functionHandlers[function]?.generateVariableAccess(SourceVariable(variable))
                ?: error("No handler for function $function")
        val valueCFG = visit(value, EvalMode.Value)
        val variableWrite = CFGNode.Assignment(variableAccess, valueCFG.access)
        return when (valueCFG) {
            is SubCFG.Immediate -> SubCFG.Immediate(variableWrite)
            is SubCFG.Extracted -> {
                val writeVertex = addVertex(variableWrite)
                SubCFG.Extracted(
                    valueCFG.entry,
                    writeVertex,
                    noOpOr(if (propagate) variableAccess else CFGNode.UNIT, mode),
                )
            }
        }
    }

    private fun visitAssignment(
        expression: OperatorBinary.Assignment,
        mode: EvalMode,
    ): SubCFG {
        val variableUse =
            expression.lhs as? VariableUse
                ?: error("Expected variable use in assignment lhs, got ${expression.lhs}")
        val definition = resolvedVariables[variableUse] ?: error("Unresolved variable $variableUse")
        return generateAssignment(definition, expression.rhs, mode, true)
    }

    private fun visitArithmeticOperator(
        expression: OperatorBinary.ArithmeticOperator,
        mode: EvalMode,
    ): SubCFG = {
        when (mode) {
            is EvalMode.Conditional -> error("Arithmetic operator $expression cannot be used as conditional")
            is EvalMode.SideEffect -> TODO()
            is EvalMode.Value -> {
                val lhs = visit(expression.lhs, EvalMode.Value)
                val rhs = visit(expression.rhs, EvalMode.Value)

                if (!hasClashingSideEffects(expression.lhs, expression.rhs)) {
                    if (lhs is SubCFG.Immediate && rhs is SubCFG.Immediate) {
                        return SubCFG.Immeddiate(makeOperatorNode(expression, lhs.access, rhs.access))
                    } else {
                        val extractedLhs = ensureExtracted(lhs, EvalMode.Value)

                        val opVertex = addVertex(makeOperatorNode(expression, lhs.access, rhs.access))

                        // lhs --> (rhs) --> opVertex
                        if (rhs is SubCFG.Extracted) {

                        } else {
                        }
                    }
                } else {
                }
            }
        }
    }

    private fun visitArithmeticAssignmentOperator(
        expression: OperatorBinary.ArithmeticAssignmentOperator,
        mode: EvalMode,
    ): SubCFG {
        TODO()
    }

    private fun visitLogicalOperator(
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
        // a && b  -->  if a then b else false
        TODO("Not yet implemented")
    }

    private fun visitLogicalAndOperator(
        expression: OperatorBinary.LogicalAnd,
        mode: EvalMode,
    ): SubCFG {
        // Value: a && b  -->  if a then b else false
        // Conditional: a && b  -->  if a then (if b then jump True else jump False) else jump False
        // SideEffect: if a then b

        // if a && (b || c) then X else Y
        //
        // if a then ( [b || c](X, Y) ) else Y
        //
        // if a || (b && c) then X else Y
        // if a then X else ( [b == c](X, Y) )

        // if (B.A.G.N.O. <- we are here) then T else F
        //
        //   condition
        //    /   \
        //   T     F
        //    \   /
        //     exit
        if (mode is EvalMode.Conditional) {
            val rhs = ensureExtracted(visit(expression.rhs, mode), mode)
            val innerMode = EvalMode.Conditional(rhs, mode.falseCFG)
            val lhs = ensureExtracted(visit(expression.lhs, innerMode), innerMode)

            val exit = addVertex(CFGNode.NoOp)
            return SubCFG.Extracted(lhs.entry, exit, CFGNode.NoOp)
        }
        TODO()
    }

    private fun visitOperatorBinary(
        expression: OperatorBinary,
        mode: EvalMode,
    ): SubCFG =
        when (expression) {
            is OperatorBinary.Assignment -> visitAssignment(expression, mode)
            is OperatorBinary.ArithmeticOperator -> visitArithmeticOperator(expression, mode)
            is OperatorBinary.ArithmeticAssignmentOperator -> visitArithmeticAssignmentOperator(expression, mode)
            is OperatorBinary.LogicalOperator -> visitLogicalOperator(expression, mode)
        }
//
//        val lhs = visit(expression.lhs, EvalMode.Value)
//        val rhs = visit(expression.rhs, EvalMode.Value)
//
//        if (hasClashingSideEffects(expression.lhs, expression.rhs)) {
//            val lhs = ensureExtracted(lhs, EvalMode.Value)
//
//            val opNode = makeOperatorNode(expression, lhs.access, rhs.access)
//            val op = addVertex(opNode)
//
//            if (rhs is SubCFG.Extracted) {
//                lhs.exit.connect(rhs.entry.label)
//                rhs.exit.connect(op.label)
//            } else {
//                lhs.exit.connect(op.label)
//            }
//
//            return SubCFG.Extracted(lhs.entry, op, opNode)
//        } else {
//            if (lhs !is SubCFG.Extracted && rhs !is SubCFG.Extracted) {
//                return SubCFG.Immediate(makeOperatorNode(expression, lhs.access, rhs.access))
//            } else {
//                val op = addVertex(makeOperatorNode(expression, lhs.access, rhs.access))
//
//                val entry = joinedEntry(lhs, rhs)
//
//                if (lhs is SubCFG.Extracted) {
//                    lhs.exit.connect(op.label)
//                }
//                if (rhs is SubCFG.Extracted) {
//                    rhs.exit.connect(op.label)
//                }
//
//                return SubCFG.Extracted(entry, op, makeOperatorNode(expression, lhs.access, rhs.access))
//            }
//        }
//    }

    private fun visitOperatorUnary(
        expression: OperatorUnary,
        mode: EvalMode,
    ): SubCFG =
        when (expression) {
            is OperatorUnary.Negation -> visitNegationOperator(expression, mode)
            is OperatorUnary.Minus -> visitMinusOperator(expression, mode)
        }

    private fun visitMinusOperator(expression: OperatorUnary.Minus, mode: CFGGenerator.EvalMode): CFGGenerator.SubCFG =
        when (mode) {
            is EvalMode.Conditional -> error("Minus is not a conditional operator")
            is EvalMode.SideEffect -> visit(expression.expression, EvalMode.SideEffect)
            is EvalMode.Value -> {
                val expressionCFG = visit(expression.expression, EvalMode.Value)
                when (expressionCFG) {
                    is SubCFG.Immediate -> SubCFG.Immediate(CFGNode.Minus(expressionCFG.access))
                    is SubCFG.Extracted -> SubCFG.Extracted(
                        expressionCFG.entry,
                        expressionCFG.exit,
                        CFGNode.Minus(expressionCFG.access)
                    )
                }
            }
        }

    private fun visitNegationOperator(
        expression: OperatorUnary.Negation,
        mode: CFGGenerator.EvalMode
    ): CFGGenerator.SubCFG =
        when (mode) {
            is EvalMode.Conditional -> visit(expression.expression, EvalMode.Conditional(mode.falseCFG, mode.trueCFG))
            is EvalMode.SideEffect -> visit(expression.expression, EvalMode.SideEffect)
            is EvalMode.Value -> {
                val expressionCFG = visit(expression.expression, EvalMode.Value)
                when (expressionCFG) {
                    is SubCFG.Immediate -> SubCFG.Immediate(CFGNode.Negation(expressionCFG.access))
                    is SubCFG.Extracted -> SubCFG.Extracted(
                        expressionCFG.entry,
                        expressionCFG.exit,
                        CFGNode.Negation(expressionCFG.access)
                    )
                }
            }
        }

    private fun visitBreakStatement(
        expression: Statement.BreakStatement,
        mode: EvalMode,
    ): SubCFG {
//        val exitLabel = CFGLabel()
//        val entryLabel = addVertex(CFGVertex.Jump(CFGNode.NoOp, exitLabel))
//        return SubCFG.Extracted(entryLabel, exitLabel, CFGNode.NoOp)
        TODO()
    }

    private fun visitIfElseStatement(
        expression: Statement.IfElseStatement,
        mode: EvalMode,
    ): SubCFG {
        if (expression.testExpression is Literal.BoolLiteral) return shortenTrivialIfStatement(expression, mode)

        val resultValueRegister = CFGNode.VariableUse(Register.Virtual())
        val trueCFG = extendWithAssignment(visit(expression.doExpression, mode), resultValueRegister, mode)
        val falseCFG =
            extendWithAssignment(
                expression.elseExpression?.let { visit(it, mode) } ?: SubCFG.Immediate(CFGNode.NoOp),
                resultValueRegister,
                mode,
            )

        val conditionalCFG = visit(expression.testExpression, EvalMode.Conditional(trueCFG, falseCFG))

        return if (mode !is EvalMode.Value) conditionalCFG else {
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
        if (expression.testExpression !is Literal.BoolLiteral) {
            throw IllegalStateException("Expected testExpression to be BoolLiteral")
        }

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
        val resultAssignment = CFGNode.Assignment(CFGNode.VariableUse(Register.Fixed.RAX), valueCFG.access)
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
            SubCFG.Immediate(getFunctionHandler(function).generateVariableAccess(SourceVariable(definition)))
        return when (mode) {
            is EvalMode.Value -> variableAccess
            is EvalMode.SideEffect -> SubCFG.Immediate(CFGNode.NoOp)
            is EvalMode.Conditional -> {
                val check = ensureExtracted(variableAccess, mode)
                check.exit.connect(mode.falseCFG.entry.label)
                check.exit.connect(mode.trueCFG.entry.label)
                val join = addVertex(CFGNode.NoOp)
                mode.falseCFG.exit.connect(join.label)
                mode.trueCFG.exit.connect(join.label)
                SubCFG.Extracted(check.entry, join, CFGNode.NoOp)
            }
        }
    }

    private fun getFunctionHandler(function: Definition.FunctionDeclaration): FunctionHandler {
        return functionHandlers[function] ?: error("Function $function has no handler")
    }
}
