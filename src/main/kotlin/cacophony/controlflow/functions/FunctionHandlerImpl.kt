package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.CFGNode.MemoryAccess
import cacophony.controlflow.CFGNode.RegisterUse
import cacophony.controlflow.generation.*
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.analysis.ClosureAnalysisResult
import cacophony.semantic.analysis.EscapeAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition
import kotlin.math.max

class FunctionHandlerImpl(
    private val function: FunctionDefinition,
    private val analyzedFunction: AnalyzedFunction,
    // List of parents' handlers ordered from immediate parent.
    private val ancestorFunctionHandlers: List<FunctionHandler>,
    callConvention: CallConvention,
    private val variablesMap: VariablesMap,
    // TODO (?): closure analysis is not really used in the FunctionHandler, as it does not need to generate a closure
    private val closureAnalysisResult: ClosureAnalysisResult,
    escapeAnalysis: EscapeAnalysisResult,
) : FunctionHandler {
    private val staticLink = Variable.PrimitiveVariable("sl")
    private var stackSpace = REGISTER_SIZE
    private val variableAllocation: MutableMap<Variable.PrimitiveVariable, VariableAllocation> = mutableMapOf()

    // Initially variables may be allocated in virtualRegisters, only after spill handling we know
    // if they're truly on stack or in registers.
    // Every virtualRegister knows if it holds reference, therefore we care only about references on stack here.
    // Initialized with 0, because rbp points to caller rbp, which we treat as a reference, except for main function.
    private val referenceOffsets = if (ancestorFunctionHandlers.isNotEmpty()) mutableListOf(0) else mutableListOf()

    // This does not perform any checks and may override previous allocation.
    // Holes in the stack may be also created if stack variables are directly allocated with this method.
    override fun registerVariableAllocation(variable: Variable.PrimitiveVariable, allocation: VariableAllocation) {
        if (allocation is VariableAllocation.OnStack) {
            if (variable.holdsReference) {
                referenceOffsets.add(allocation.offset)
            }
            stackSpace = max(stackSpace, allocation.offset + REGISTER_SIZE)
        }
        if (allocation is VariableAllocation.ViaPointer && allocation.pointer is VariableAllocation.OnStack) {
            if (variable.holdsReference) {
                referenceOffsets.add(allocation.pointer.offset)
            }
            stackSpace = max(stackSpace, allocation.pointer.offset + REGISTER_SIZE)
        }
        variableAllocation[variable] = allocation
    }

    override fun allocateFrameVariable(variable: Variable.PrimitiveVariable): CFGNode.LValue {
        val currentStackSpace = stackSpace
        registerVariableAllocation(variable, VariableAllocation.OnStack(stackSpace))
        return MemoryAccess(
            CFGNode.Subtraction(
                RegisterUse(Register.FixedRegister(HardwareRegister.RBP), false),
                CFGNode.ConstantKnown(currentStackSpace),
            ),
            variable.holdsReference,
        )
    }

    // TODO: probably some part of this code should be present also in the LambdaHandler
    init {
        introduceStaticLinksParams()

        run {
            // Every variable this function declares needs a place to life. We decide where it lives as follows:
            // 1) if the variable escapes, then it accessible on the heap
            //      a) if it is used inside a nested function, then the pointer to it is placed on the stack
            //      b) otherwise, the pointer to it is placed inside a virtual register (can this happen?)
            // 2) otherwise, if the variable is used inside a nested function, then it is accessible on stack
            // 3) otherwise, the variable can be placed inside a virtual register
            val declared =
                (
                    analyzedFunction.declaredVariables().map { it.origin.getPrimitives() }.flatten() union
                        analyzedFunction.variablesUsedInNestedFunctions.filterIsInstance<Variable.PrimitiveVariable>() union
                        function.arguments.map { variablesMap.definitions[it]!!.getPrimitives() }.flatten()
                ).toSet()
            val stackAll = analyzedFunction.variablesUsedInNestedFunctions.filterIsInstance<Variable.PrimitiveVariable>().toSet()
            val escaped = escapeAnalysis.filterIsInstance<Variable.PrimitiveVariable>() intersect declared
            val nonEscaped = declared.minus(escaped)
            val escapedStack = escaped intersect stackAll
            val escapedReg = escaped.minus(escapedStack)
            val stack = nonEscaped intersect stackAll
            val reg = nonEscaped.minus(stack)

            // without lambdas, 1.a, 1.b should be empty
            // 1.a
            escapedStack.forEach {
                // There is something fishy going on with escaped variables which are structs, but maybe that's ok
                registerVariableAllocation(
                    it,
                    VariableAllocation.ViaPointer(VariableAllocation.OnStack(stackSpace), 0),
                )
            }

            // 1.b
            escapedReg.forEach {
                registerVariableAllocation(
                    it,
                    VariableAllocation.ViaPointer(VariableAllocation.InRegister(Register.VirtualRegister(true)), 0),
                )
            }

            // 2
            stack.forEach {
                allocateFrameVariable(it)
            }

            // 3
            reg.forEach {
                registerVariableAllocation(
                    it,
                    VariableAllocation.InRegister(Register.VirtualRegister(it.holdsReference)),
                )
            }
        }
    }

    override fun getFunctionDeclaration(): FunctionDefinition = function

    private fun traverseStaticLink(depth: Int): CFGNode =
        if (depth == 0) {
            registerUse(rbp, false)
        } else {
            memoryAccess(traverseStaticLink(depth - 1) sub integer(REGISTER_SIZE), false)
        }

    override fun generateAccessToFramePointer(other: FunctionDefinition): CFGNode =
        if (other == function) {
            traverseStaticLink(0)
        } else {
            traverseStaticLink(
                ancestorFunctionHandlers.indexOfFirst { it.getFunctionDeclaration() == other } + 1,
            )
        }

    override fun generateStaticLinkVariable(callerFunction: FunctionHandler): CFGNode =
        // Since staticLink is not property of node itself, but rather of its children,
        // if caller is immediate parent, we have to fetch RBP instead.
        if (ancestorFunctionHandlers.isEmpty() || callerFunction === ancestorFunctionHandlers.first()) {
            registerUse(rbp, false)
        } else {
            callerFunction.generateAccessToFramePointer(ancestorFunctionHandlers.first().getFunctionDeclaration())
        }

    override fun generateVariableAccess(variable: Variable.PrimitiveVariable): CFGNode.LValue {
        if (variable in variableAllocation) { // WARN: hack to handle arguments not used inside function
            return wrapAllocation(variableAllocation[variable]!!, variable.holdsReference)
        }
        val analyzedVariable = analyzedFunction.variables.find { it.origin == variable }
        val definedInFunctionHandler =
            if (analyzedVariable != null) {
                (ancestorFunctionHandlers + this).find { it.getFunctionDeclaration() == analyzedVariable.definedIn }
            } else {
                (ancestorFunctionHandlers + this).find { it.getStaticLink() == variable }
            }

        if (definedInFunctionHandler == null) {
            throw GenerateVariableAccessException("Function $function has no access to $variable.")
        }

        val definedInDeclaration = definedInFunctionHandler.getFunctionDeclaration()

        return when (val variableAllocation = definedInFunctionHandler.getVariableAllocation(variable)) {
            is VariableAllocation.InRegister -> {
                RegisterUse(variableAllocation.register, variable.holdsReference)
            }

            is VariableAllocation.OnStack -> {
                MemoryAccess(
                    CFGNode.Subtraction(
                        generateAccessToFramePointer(definedInDeclaration),
                        CFGNode.ConstantKnown(variableAllocation.offset),
                    ),
                    variable.holdsReference,
                )
            }
            is VariableAllocation.ViaPointer -> TODO()
        }
    }

    override fun getVariableAllocation(variable: Variable.PrimitiveVariable): VariableAllocation =
        variableAllocation.getOrElse(variable) {
            throw IllegalArgumentException("Variable $variable have not been allocated inside $this FunctionHandler")
        }

    override fun getVariableFromDefinition(varDef: Definition): Variable =
        variablesMap.definitions.getOrElse(varDef) {
            throw IllegalArgumentException("Variable $varDef have not been defined inside function $function")
        }

    override fun getStaticLink(): Variable.PrimitiveVariable = staticLink

    override fun getStackSpace(): CFGNode.ConstantLazy = CFGNode.ConstantLazy { stackSpace }

    private val resultLayout = generateLayoutOfVirtualRegisters(function.returnType)

    override fun getResultLayout(): Layout = resultLayout

    private fun getFlattenedArguments(): List<CFGNode> =
        function.arguments
            .map { variablesMap.definitions[it]!! }
            .map {
                getVariableLayout(
                    this,
                    it,
                )
            }.map { flattenLayout(it) }
            .flatten() + generateVariableAccess(getStaticLink())

    private val prologueEpilogueHandler =
        PrologueEpilogueHandler(
            this,
            callConvention,
            getStackSpace(),
            getFlattenedArguments(),
            getResultLayout(),
        )

    override fun generatePrologue(): List<CFGNode> = prologueEpilogueHandler.generatePrologue()

    override fun generateEpilogue(): List<CFGNode> = prologueEpilogueHandler.generateEpilogue()

    override fun getReferenceAccesses(): List<Int> = referenceOffsets

    // Creates staticLink auxVariable in analyzedFunction, therefore shouldn't be called multiple times.
    // Static link is created even if parent doesn't exist.
    private fun introduceStaticLinksParams() {
        registerVariableAllocation(
            staticLink,
            VariableAllocation.OnStack(REGISTER_SIZE),
        )
        analyzedFunction.auxVariables.add(staticLink)
    }
}
