package cacophony.controlflow.generation

import cacophony.controlflow.*
import cacophony.controlflow.functions.Builtin
import cacophony.controlflow.functions.CallGenerator
import cacophony.controlflow.functions.CallableHandler
import cacophony.controlflow.functions.CallableHandlers
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.rtti.LambdaOutlineLocation
import cacophony.semantic.rtti.ObjectOutlineLocation
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.types.FunctionType
import cacophony.semantic.types.ReferentialType
import cacophony.semantic.types.TypeCheckingResult

/**
 * Converts Expressions into CFG
 */
internal class CFGGenerator(
    private val resolvedVariables: ResolvedVariables,
    private val function: LambdaExpression,
    private val callableHandlers: CallableHandlers,
    val variablesMap: VariablesMap,
    private val typeCheckingResult: TypeCheckingResult,
    private val callGenerator: CallGenerator,
    private val objectOutlineLocation: ObjectOutlineLocation,
    private val lambdaOutlineLocation: LambdaOutlineLocation,
) {
    private val cfg = CFG()
    private val operatorHandler = OperatorHandler(cfg, this)
    private val assignmentHandler = AssignmentHandler(this)
    private val prologue = listOfNodesToExtracted(getCurrentCallableHandler().generatePrologue())
    private val epilogue = listOfNodesToExtracted(getCurrentCallableHandler().generateEpilogue())

    internal fun generateFunctionCFG(): CFGFragment {
        val bodyCFG = visit(function.body, EvalMode.Value, Context(null))
        val returnValueLayout = getCurrentCallableHandler().getResultLayout()

        val extended =
            when (bodyCFG) {
                is SubCFG.Extracted -> extendWithAssignment(bodyCFG, returnValueLayout, EvalMode.Value)
                is SubCFG.Immediate -> assignLayoutWithValue(bodyCFG.access, returnValueLayout, returnValueLayout)
            }

        val resultSize = function.returnType.size()

        val returnVertex = cfg.addFinalVertex(CFGNode.Return(CFGNode.ConstantKnown(resultSize)))

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

    internal fun assignLayoutWithValue(source: Layout, destination: Layout, returnedValue: Layout): SubCFG.Extracted {
        val assignments = makeVerticesForAssignment(source, destination)
        if (assignments.isEmpty()) {
            val vertex = cfg.addUnconditionalVertex(CFGNode.NoOp)
            return SubCFG.Extracted(vertex, vertex, StructLayout(emptyMap()))
        }
        val prerequisite =
            assignments
                .dropLast(1)
                .map { SubCFG.Extracted(it, it, CFGNode.NoOp, false) }
                .reduceOrNull(SubCFG.Extracted::merge)
        val lastAssignment = assignments.last()
        val last = SubCFG.Extracted(lastAssignment, lastAssignment, returnedValue)
        return if (prerequisite != null) {
            prerequisite merge last
        } else {
            last
        }
    }

    private fun makeVerticesForAssignment(source: Layout, destination: Layout): List<GeneralCFGVertex.UnconditionalVertex> =
        when (source) {
            is SimpleLayout -> {
                if (destination is VoidLayout) {
                    listOf(cfg.addUnconditionalVertex(CFGNode.NoOp))
                } else {
                    require(destination is SimpleLayout) // by type checking
                    require(destination.access is CFGNode.LValue)
                    val write = CFGNode.Assignment(destination.access, source.access)
                    listOf(cfg.addUnconditionalVertex(write))
                }
            }

            is StructLayout -> {
                require(destination is StructLayout) // by type checking
                destination.fields.flatMap { (field, layout) -> makeVerticesForAssignment(source.fields[field]!!, layout) }
            }

            is FunctionLayout -> {
                require(destination is FunctionLayout) // by type checking
                makeVerticesForAssignment(source.code, destination.code) + makeVerticesForAssignment(source.link, destination.link)
            }

            is ClosureLayout -> {
                error("Unexpected assignment to internal layout type ClosureLayout")
            }

            is VoidLayout -> emptyList()
        }

    internal fun ensureExtracted(subCFG: SubCFG, mode: EvalMode): SubCFG.Extracted =
        when (subCFG) {
            is SubCFG.Extracted -> subCFG
            is SubCFG.Immediate ->
                when (mode) {
                    is EvalMode.Value -> {
                        val destination = generateLayoutOfVirtualRegisters(subCFG.access)
                        assignLayoutWithValue(subCFG.access, destination, destination)
                    }

                    else -> {
                        val access = subCFG.access
                        val vertex = cfg.addUnconditionalVertex(if (access is SimpleLayout) access.access else CFGNode.NoOp)
                        SubCFG.Extracted(vertex, vertex, subCFG.access)
                    }
                }
        }

    private fun ensureExtracted(node: CFGNode, holdsReference: Boolean): SubCFG.Extracted =
        ensureExtracted(SubCFG.Immediate(node, holdsReference), EvalMode.SideEffect)

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
        when (expression) {
            is Block -> visitBlock(expression, mode, context)
            is LambdaExpression -> visitLambdaExpression(expression, mode)
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
            is Struct -> visitStruct(expression, mode, context)
            is FieldRef.RValue -> visitFieldRef(expression, mode, context)
            is Allocation -> visitAllocation(expression, mode, context)
            is Dereference -> visitDereference(expression, mode, context)
            is Assignable -> visitAssignable(expression, mode)
            else -> error("Unexpected expression for CFG generation: $expression")
        }

    private fun visitAllocation(expression: Allocation, mode: EvalMode, context: Context): SubCFG {
        val calcExpression = visit(expression.value, mode, context)
        return when (mode) {
            is EvalMode.Value -> {
                val type = typeCheckingResult.expressionTypes[expression]!!
                require(type is ReferentialType) // by type checking

                val outlineLocation = objectOutlineLocation[type.type]!!

                val callCFG =
                    allocAndCopy(
                        outlineLocation,
                        calcExpression.access,
                    ) { pointerLayout -> generateLayoutOfHeapObject(pointerLayout.access, type.type) }

                when (calcExpression) {
                    is SubCFG.Extracted -> calcExpression merge callCFG
                    is SubCFG.Immediate -> callCFG
                }
            }
            // do nothing if value is not used
            is EvalMode.SideEffect -> {
                when (calcExpression) {
                    is SubCFG.Extracted -> {
                        val vertex = cfg.addUnconditionalVertex(CFGNode.NoOp)
                        calcExpression merge SubCFG.Extracted(vertex, vertex, CFGNode.NoOp, false)
                    }

                    is SubCFG.Immediate -> SubCFG.Immediate(CFGNode.NoOp, false)
                }
            }

            is EvalMode.Conditional -> throw IllegalArgumentException("Reference cannot be used as condition")
        }
    }

    private fun visitDereference(expression: Dereference, mode: EvalMode, context: Context): SubCFG {
        val pointerGeneration = visit(expression.value, EvalMode.Value, context)
        val access = pointerGeneration.access
        require(access is SimpleLayout) // by type checking
        if (mode is EvalMode.SideEffect) {
            return pointerGeneration
        }
        val layout = generateLayoutOfHeapObject(access.access, typeCheckingResult.expressionTypes[expression]!!)
        val dereference =
            when (pointerGeneration) {
                is SubCFG.Extracted -> {
                    val vertex = cfg.addUnconditionalVertex(CFGNode.NoOp)
                    pointerGeneration merge SubCFG.Extracted(vertex, vertex, layout)
                }

                is SubCFG.Immediate -> SubCFG.Immediate(layout)
            }
        return if (mode is EvalMode.Conditional) extendWithConditional(dereference, mode)
        else dereference
    }

    private fun visitFieldRef(expression: FieldRef, mode: EvalMode, context: Context): SubCFG {
        val structGeneration = ensureExtracted(visit(expression.struct(), EvalMode.Value, context), EvalMode.SideEffect)
        require(structGeneration.access is StructLayout) // by type checking
        val resultLayout = structGeneration.access.fields[expression.field]!!
        val vertex = cfg.addUnconditionalVertex(CFGNode.NoOp)
        val fieldAccess = structGeneration merge SubCFG.Extracted(vertex, vertex, noOpOr(resultLayout, mode))
        return if (mode is EvalMode.Conditional) extendWithConditional(fieldAccess, mode)
        else fieldAccess
    }

    private fun extendWithConditional(node: SubCFG, mode: EvalMode.Conditional): SubCFG.Extracted {
        val access = node.access
        // by type checking
        require(access is SimpleLayout)
        val conditionVertex = cfg.addConditionalVertex(access.access neq integer(0))
        if (node is SubCFG.Extracted) {
            node.exit.connect(conditionVertex.label)
        }
        conditionVertex.connectTrue(mode.trueEntry.label)
        conditionVertex.connectFalse(mode.falseEntry.label)
        return SubCFG.Extracted(conditionVertex, mode.exit, CFGNode.NoOp, false)
    }

    private fun visitStruct(expression: Struct, mode: EvalMode, context: Context): SubCFG {
        val fields = expression.fields.map { (name, field) -> name.name to visit(field, mode, context) }.toMap()
        if (fields.all { (_, field) -> field is SubCFG.Immediate }) {
            return SubCFG.Immediate(StructLayout(fields.mapValues { (_, subCFG) -> subCFG.access }))
        }
        val layout = StructLayout(fields.mapValues { (_, field) -> field.access })
        val structGeneration = fields.map { (_, field) -> ensureExtracted(field, mode) }.reduce(SubCFG.Extracted::merge)
        val vertex = cfg.addUnconditionalVertex(CFGNode.NoOp)
        val res = SubCFG.Extracted(vertex, vertex, layout)
        return structGeneration merge res
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

    private fun visitLambdaExpression(lambda: LambdaExpression, mode: EvalMode): SubCFG {
        val handler = callableHandlers.getClosureHandler(lambda)
        val label = handler.getFunctionLabel()
        // alloc_struct(lambdaOutlineLocation[lambda], rbp) -> ptr
        // ptr <- wrzucić na offsety odpowiednie (jakie?) variable/layout/cfgnode?
        return when (mode) {
            is EvalMode.Value -> {
                val outlineLocation = lambdaOutlineLocation.getValue(lambda)
                val offsets = handler.getCapturedVariableOffsets()
                val sourceLayout =
                    ClosureLayout(
                        offsets.mapValues { (variable, _) ->
                            SimpleLayout(handler.generateVariableAccess(variable), variable.holdsReference)
                        },
                    )

                val allocCFG =
                    allocAndCopy(outlineLocation, sourceLayout) { pointerLayout -> generateLayoutOfClosure(pointerLayout.access, offsets) }

                val closureLink = allocCFG.access
                require(closureLink is SimpleLayout)

                SubCFG.Extracted(
                    allocCFG.entry,
                    allocCFG.exit,
                    FunctionLayout(SimpleLayout(dataLabel(label)), closureLink),
                )
            }

            is EvalMode.SideEffect -> SubCFG.Immediate(noOpOrUnit(mode))
            is EvalMode.Conditional -> throw IllegalArgumentException("Lambda expression can not be used as condition")
        }
    }

    private fun visitVariableDeclaration(expression: Definition.VariableDeclaration, mode: EvalMode, context: Context): SubCFG =
        assignmentHandler.generateAssignment(
            getVariableLayout(getCurrentCallableHandler(), variablesMap.definitions[expression]!!),
            expression.value,
            mode,
            context,
            false,
        )

    private fun visitEmpty(mode: EvalMode): SubCFG = SubCFG.Immediate(noOpOrUnit(mode))

    private fun visitFunctionCall(expression: FunctionCall, mode: EvalMode, context: Context): SubCFG {
        val calleeCFG = ensureExtracted(visit(expression.function, EvalMode.Value, context), EvalMode.Value)

        val functionLayout = calleeCFG.access
        require(functionLayout is FunctionLayout)

        val argumentVertices =
            expression.arguments
                .map { visitExtracted(it, EvalMode.Value, context) }

        val functionType = typeCheckingResult.expressionTypes[expression.function]!!
        require(functionType is FunctionType) { "LHS of call should be callable but is $functionType" }

        val resultLayout =
            when (mode) {
                is EvalMode.SideEffect -> null
                else -> generateLayoutOfVirtualRegisters(functionType.result)
            }

        val callCFG = generateCall(functionType, functionLayout, argumentVertices.map { it.access }, resultLayout)

        val fullCFG =
            (argumentVertices.fold(calleeCFG, SubCFG.Extracted::merge) merge callCFG).let {
                SubCFG.Extracted(it.entry, it.exit, resultLayout ?: SimpleLayout(CFGNode.NoOp))
            }
        if (mode is EvalMode.Conditional) {
            return extendWithConditional(fullCFG, mode)
        }
        return fullCFG
    }

    private fun generateCall(functionType: FunctionType, function: FunctionLayout, arguments: List<Layout>, result: Layout?) =
        callGenerator
            .generateCallFrom(getCurrentCallableHandler(), functionType, function, arguments, result)
            .map {
                ensureExtracted(it, false)
            }.reduce(SubCFG.Extracted::merge)

    private fun visitLiteral(literal: Literal, mode: EvalMode): SubCFG =
        when (mode) {
            is EvalMode.Value ->
                SubCFG.Immediate(
                    when (literal) {
                        is Literal.BoolLiteral -> if (literal.value) CFGNode.TRUE else CFGNode.FALSE
                        is Literal.IntLiteral -> CFGNode.ConstantKnown(literal.value)
                    },
                    false,
                )

            is EvalMode.SideEffect -> SubCFG.Immediate(CFGNode.NoOp, false)

            is EvalMode.Conditional -> {
                check(literal is Literal.BoolLiteral) { "Non-boolean literal cannot be used as a condition" }

                val target = if (literal.value) mode.trueEntry else mode.falseEntry
                SubCFG.Extracted(target, mode.exit, CFGNode.NoOp, false)
            }
        }

    private fun visitAssignment(expression: OperatorBinary.Assignment, mode: EvalMode, context: Context): SubCFG {
        val lhs =
            expression.lhs as? Assignable
                ?: error("Expected Assignable in assignment lhs, got ${expression.lhs}")
        return when (expression.lhs) {
            is VariableUse, is FieldRef.LValue ->
                assignmentHandler.generateAssignment(
                    getVariableLayout(getCurrentCallableHandler(), variablesMap.lvalues[lhs]!!),
                    expression.rhs,
                    mode,
                    context,
                    true,
                )

            is Dereference -> {
                val pointer = visitDereference(expression.lhs, EvalMode.Value, context)
                val assignment =
                    assignmentHandler.generateAssignment(
                        pointer.access,
                        expression.rhs,
                        mode,
                        context,
                        true,
                    )
                when (pointer) {
                    is SubCFG.Extracted -> pointer merge ensureExtracted(assignment, mode)
                    is SubCFG.Immediate -> assignment
                }
            }
        }
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

        val resultValueLayout = generateLayoutOfVirtualRegisters(typeCheckingResult.expressionTypes[expression]!!)
        val trueCFG = extendWithAssignment(visit(expression.doExpression, mode, context), resultValueLayout, mode)
        val falseCFG =
            extendWithAssignment(
                expression.elseExpression?.let { visit(it, mode, context) } ?: SubCFG.Immediate(CFGNode.NoOp, false),
                resultValueLayout,
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
            SubCFG.Extracted(extractedConditionalCFG.entry, extractedConditionalCFG.exit, resultValueLayout)
        }
    }

    internal fun extendWithAssignment(subCFG: SubCFG, destination: Layout, mode: EvalMode): SubCFG.Extracted =
        when (mode) {
            is EvalMode.Value -> {
                val assignment = assignLayoutWithValue(subCFG.access, destination, destination)
                if (subCFG is SubCFG.Extracted) {
                    subCFG merge assignment
                } else {
                    assignment
                }
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
            } ?: SubCFG.Immediate(CFGNode.NoOp, false)
        }
    }

    private fun visitReturnStatement(expression: Statement.ReturnStatement, mode: EvalMode, context: Context): SubCFG {
        val valueCFG = visit(expression.value, EvalMode.Value, context)
        val resultAssignment =
            assignLayoutWithValue(valueCFG.access, getCurrentCallableHandler().getResultLayout(), SimpleLayout(CFGNode.NoOp, false))

        resultAssignment.exit.connect(epilogue.entry.label)

        // Similarly to break, return creates an artificial exit
        val artificialExit = if (mode is EvalMode.Conditional) mode.exit else cfg.addUnconditionalVertex(CFGNode.NoOp)

        val entry =
            when (valueCFG) {
                is SubCFG.Extracted -> {
                    valueCFG.exit.connect(resultAssignment.entry.label)
                    valueCFG.entry
                }

                is SubCFG.Immediate -> resultAssignment.entry
            }
        return SubCFG.Extracted(entry, artificialExit, VoidLayout())
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
        return SubCFG.Extracted(vertex, artificialExit, VoidLayout())
    }

    private fun visitAssignable(expression: Assignable, mode: EvalMode): SubCFG {
        val layout =
            variablesMap.lvalues[expression]?.let { variable ->
                getVariableLayout(getCurrentCallableHandler(), variable)
            } ?: let {
                require(expression is VariableUse) { "Expected $expression to be instance of VariableUse" }

                when (val function = resolvedVariables[expression]) {
                    // is LambdaExpression -> getFunctionLayout(getCurrentCallableHandler(), callableHandlers[function]!!) TODO: fix
                    is Definition.ForeignFunctionDeclaration -> getForeignFunctionLayout(function)
                    else -> error("Expected function declaration, but got $function")
                }
            }

        return when (mode) {
            is EvalMode.Value -> SubCFG.Immediate(layout)
            is EvalMode.SideEffect -> SubCFG.Immediate(CFGNode.NoOp, false)
            is EvalMode.Conditional -> extendWithConditional(SubCFG.Immediate(layout), mode)
        }
    }

    private fun getCurrentCallableHandler(): CallableHandler = getCallableHandler(function)

    private fun getCallableHandler(callable: LambdaExpression): CallableHandler = callableHandlers.getCallableHandler(callable)

    // sourceLayout opisuje gdzie są rzeczy, które chcemy przekopiować na stertę
    // resultLayout opisuje obiekt na stercie
    private fun allocAndCopy(outlineLocation: String, sourceLayout: Layout, makeResultLayout: (SimpleLayout) -> Layout): SubCFG.Extracted {
        val arguments =
            listOf(
                ensureExtracted(
                    SubCFG.Immediate(dataLabel(outlineLocation), false),
                    EvalMode.Value,
                ),
                ensureExtracted(
                    SubCFG.Immediate(registerUse(rbp, false), false),
                    EvalMode.Value,
                ),
            )

        val resultPointerLayout = SimpleLayout(registerUse(Register.VirtualRegister(true), true), true)

        val functionType = typeCheckingResult.definitionTypes[Builtin.allocStruct]!!
        val functionLayout = getForeignFunctionLayout(Builtin.allocStruct)
        require(functionType is FunctionType) { "LHS of call should be callable but is $functionType" }
        val call =
            generateCall(
                functionType,
                functionLayout,
                arguments.map { it.access },
                resultPointerLayout,
            )
        val callWithArgs = arguments.reduce(SubCFG.Extracted::merge) merge call
        val resultLayout = makeResultLayout(resultPointerLayout)

        return (
            callWithArgs merge
                assignLayoutWithValue(sourceLayout, resultLayout, resultPointerLayout)
        )
    }
}
