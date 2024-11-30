package cacophony.controlflow

import cacophony.controlflow.CFGNode.Addition
import cacophony.controlflow.CFGNode.MemoryAccess
import cacophony.controlflow.CFGNode.RegisterUse
import cacophony.semantic.AnalyzedFunction
import cacophony.semantic.FunctionAnalysisResult
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Definition.FunctionDeclaration
import cacophony.utils.CompileException

fun generateFunctionHandlers(
    analyzedFunctions: FunctionAnalysisResult,
    callConvention: CallConvention,
): Map<FunctionDeclaration, FunctionHandler> {
    val handlers = mutableMapOf<FunctionDeclaration, FunctionHandler>()
    val order = analyzedFunctions.entries.sortedBy { it.value.staticDepth }

    val ancestorHandlers = mutableMapOf<FunctionDeclaration, List<FunctionHandler>>()

    for ((function, analyzedFunction) in order) {
        if (analyzedFunction.parentLink == null) {
            handlers[function] = FunctionHandlerImpl(function, analyzedFunction, emptyList(), callConvention)
        } else {
            val parentHandler =
                handlers[analyzedFunction.parentLink.parent]
                    ?: throw CompileException("Parent function handler not found")
            val functionAncestorHandlers =
                listOf(parentHandler) + (ancestorHandlers[analyzedFunction.parentLink.parent] ?: emptyList())
            handlers[function] = FunctionHandlerImpl(function, analyzedFunction, functionAncestorHandlers, callConvention)
            ancestorHandlers[function] = functionAncestorHandlers
        }
    }

    return handlers
}

sealed class VariableAllocation {
    class InRegister(
        val register: Register,
    ) : VariableAllocation()

    class OnStack(
        val offset: Int,
    ) : VariableAllocation()
}

interface FunctionHandler {
    fun getFunctionDeclaration(): FunctionDeclaration

    fun generateCallFrom(
        callerFunction: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean = true,
    ): List<CFGNode>

    fun generateVariableAccess(variable: Variable): CFGNode.LValue

    fun getVariableAllocation(variable: Variable): VariableAllocation

    fun registerVariableAllocation(variable: Variable, allocation: VariableAllocation)

    // Returns static link to parent
    fun getStaticLink(): Variable.AuxVariable.StaticLinkVariable

    fun getVariableFromDefinition(varDef: Definition): Variable

    fun generateAccessToFramePointer(other: FunctionDeclaration): CFGNode

    fun allocateFrameVariable(): CFGNode.LValue

    fun generatePrologue(): List<CFGNode>

    fun generateEpilogue(): List<CFGNode>

    fun getResultRegister(): Register.VirtualRegister
}

class GenerateVariableAccessException(
    reason: String,
) : CompileException(reason)

class FunctionHandlerImpl(
    val function: FunctionDeclaration,
    private val analyzedFunction: AnalyzedFunction,
    // List of parents' handlers ordered from immediate parent.
    private val ancestorFunctionHandlers: List<FunctionHandler>,
    callConvention: CallConvention,
) : FunctionHandler {
    private val staticLink: Variable.AuxVariable.StaticLinkVariable = Variable.AuxVariable.StaticLinkVariable()
    private val definitionToVariable =
        (analyzedFunction.variables.map { it.declaration } union function.arguments).associateWith { Variable.SourceVariable(it) }

    private val variableAllocation: MutableMap<Variable, VariableAllocation> =
        run {
            val res = mutableMapOf<Variable, VariableAllocation>()
            val usedVars = analyzedFunction.variablesUsedInNestedFunctions
            val regVar =
                (
                    analyzedFunction
                        .declaredVariables()
                        .map { it.declaration } union function.arguments
                ).toSet()
                    .minus(usedVars)
            regVar.forEach { varDef ->
                res[definitionToVariable[varDef]!!] = VariableAllocation.InRegister(Register.VirtualRegister())
            }
            // offset starts at 8, since static link is placed at offset 0
            var offset = REGISTER_SIZE
            usedVars.forEach { varDef ->
                res[definitionToVariable[varDef]!!] = VariableAllocation.OnStack(offset)
                offset += REGISTER_SIZE
            }
            res
        }

    init {
        introduceStaticLinksParams()
    }

    override fun registerVariableAllocation(variable: Variable, allocation: VariableAllocation) {
        variableAllocation[variable] = allocation
    }

    override fun getFunctionDeclaration(): FunctionDeclaration = function

    // Wrapper for generateCall that additionally fills staticLink to parent function.
    // Since staticLink is not property of node itself, but rather of its children,
    // if caller is immediate parent, we have to fetch RBP instead.
    override fun generateCallFrom(
        callerFunction: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean,
    ): List<CFGNode> {
        val staticLinkVar =
            if (ancestorFunctionHandlers.isEmpty() || callerFunction === ancestorFunctionHandlers[0]) {
                RegisterUse(Register.FixedRegister(HardwareRegister.RBP))
            } else {
                callerFunction.generateAccessToFramePointer(ancestorFunctionHandlers[0].getFunctionDeclaration())
            }

        return generateCall(function, mutableListOf(staticLinkVar) + arguments, result, respectStackAlignment)
    }

    private fun traverseStaticLink(depth: Int): CFGNode =
        if (depth == 0) {
            RegisterUse(Register.FixedRegister(HardwareRegister.RBP))
        } else {
            MemoryAccess(traverseStaticLink(depth - 1))
        }

    override fun generateAccessToFramePointer(other: FunctionDeclaration): CFGNode =
        if (other == function) {
            traverseStaticLink(0)
        } else {
            traverseStaticLink(
                ancestorFunctionHandlers.indexOfFirst { it.getFunctionDeclaration() == other } + 1,
            )
        }

    override fun allocateFrameVariable(): CFGNode.LValue {
        TODO("Not yet implemented")
    }

    override fun generateVariableAccess(variable: Variable): CFGNode.LValue {
        val definedInDeclaration =
            when (variable) {
                is Variable.SourceVariable -> {
                    analyzedFunction.variables.find { it.declaration == variable.definition }?.definedIn
                }

                is Variable.AuxVariable.StaticLinkVariable -> {
                    if (getStaticLink() == variable) {
                        function
                    } else {
                        ancestorFunctionHandlers.find { it.getStaticLink() == variable }?.getFunctionDeclaration()
                    }
                }
                // For whoever finds this place in the future because of compilation error after
                // adding a new subtype of AuxVariable:
                //   If generateVariableAccess should support the new type, implement new logic here.
                //   If not, uncomment this else statement and add an appropriate unit test.
                // else -> throw GenerateVariableAccessException(
                //   "Cannot generate access to variables other than static links and source variables.")
            }

        if (definedInDeclaration == null) {
            throw GenerateVariableAccessException("Function $function has no access to $variable.")
        }

        val definedInFunctionHandler =
            ancestorFunctionHandlers.find { it.getFunctionDeclaration() == definedInDeclaration } ?: this

        return when (val variableAllocation = definedInFunctionHandler.getVariableAllocation(variable)) {
            is VariableAllocation.InRegister -> {
                RegisterUse(variableAllocation.register)
            }

            is VariableAllocation.OnStack -> {
                MemoryAccess(
                    CFGNode.Subtraction(
                        generateAccessToFramePointer(definedInDeclaration),
                        CFGNode.ConstantKnown(variableAllocation.offset),
                    ),
                )
            }
        }
    }

    override fun getVariableAllocation(variable: Variable): VariableAllocation =
        variableAllocation.getOrElse(variable) {
            throw IllegalArgumentException("Variable $variable have not been allocated inside $this FunctionHandler")
        }

    override fun getVariableFromDefinition(varDef: Definition): Variable =
        definitionToVariable.getOrElse(varDef) {
            throw IllegalArgumentException("Variable $varDef have not been defined inside function $function")
        }

    override fun getStaticLink(): Variable.AuxVariable.StaticLinkVariable = staticLink

    // Can be changed by `allocateFrameVariable()` method
    private val stackSpace: CFGNode.ConstantLazy =
        run {
            val variables =
                analyzedFunction
                    .declaredVariables()
                    .map { getVariableFromDefinition(it.declaration) } +
                    analyzedFunction.auxVariables
            val stackVariables =
                variables
                    .map { getVariableAllocation(it) }
                    .filterIsInstance<VariableAllocation.OnStack>()
                    .sortedBy { it.offset }
            run {
                // Sanity checks
                var offset = 0
                for (variable in stackVariables) {
                    if (variable.offset != offset)
                        throw IllegalStateException("Holes in stack")
                    offset += REGISTER_SIZE
                }
                if (callConvention.preservedRegisters().contains(HardwareRegister.RSP))
                    throw IllegalArgumentException("RSP amongst call preserved registers")
                if (callConvention.preservedRegisters().contains(HardwareRegister.RBP))
                    throw IllegalArgumentException("RBP amongst call preserved registers")
            }
            CFGNode.ConstantLazy(stackVariables.size * REGISTER_SIZE)
        }

    private val resultRegister = Register.VirtualRegister()

    override fun getResultRegister(): Register.VirtualRegister = resultRegister

    private val prologueEpilogueHandler =
        PrologueEpilogueHandler(
            this,
            callConvention,
            stackSpace,
            resultRegister,
        )

    override fun generatePrologue(): List<CFGNode> = prologueEpilogueHandler.generatePrologue()

    override fun generateEpilogue(): List<CFGNode> = prologueEpilogueHandler.generateEpilogue()

    // Creates staticLink auxVariable in analyzedFunction, therefore shouldn't be called multiple times.
    // Static link is created even if parent doesn't exist.
    private fun introduceStaticLinksParams() {
        registerVariableAllocation(
            staticLink,
            VariableAllocation.OnStack(0),
        )
        analyzedFunction.auxVariables.add(staticLink)
    }
}

class PrologueEpilogueHandler(
    private val handler: FunctionHandler,
    private val callConvention: CallConvention,
    private val stackSpace: CFGNode.ConstantLazy,
    private val resultAccess: Register.VirtualRegister,
) {
    private val spaceForPreservedRegisters: List<Register.VirtualRegister> =
        callConvention.preservedRegisters().map {
            Register.VirtualRegister()
        }

    fun generatePrologue(): List<CFGNode> {
        val nodes = mutableListOf<CFGNode>()
        with(handler) {
            nodes.add(pushRegister(rbp))
            nodes.add(registerUse(rbp) assign (registerUse(rsp) sub CFGNode.ConstantKnown(REGISTER_SIZE)))
            nodes.add(registerUse(rsp) subeq stackSpace)

            // Preserved registers
            for ((source, destination) in callConvention.preservedRegisters() zip spaceForPreservedRegisters) {
                nodes.add(registerUse(destination) assign registerUse(Register.FixedRegister(source)))
            }

            // Defined function arguments
            for ((ind, arg) in getFunctionDeclaration().arguments.withIndex()) {
                nodes.add(
                    wrapAllocation(getVariableAllocation(getVariableFromDefinition(arg))) assign
                        wrapAllocation(callConvention.argumentAllocation(ind)),
                )
            }

            // Static link (implicit arg)
            nodes.add(
                wrapAllocation(getVariableAllocation(getStaticLink())) assign
                    wrapAllocation(callConvention.argumentAllocation(getFunctionDeclaration().arguments.size)),
            )
        }
        return nodes
    }

    fun generateEpilogue(): List<CFGNode> {
        val nodes = mutableListOf<CFGNode>()
        // Restoring preserved registers
        for ((destination, source) in callConvention.preservedRegisters() zip spaceForPreservedRegisters) {
            nodes.add(registerUse(Register.FixedRegister(destination)) assign registerUse(source))
        }

        // Write the result to its destination
        nodes.add(registerUse(Register.FixedRegister(callConvention.returnRegister())) assign registerUse(resultAccess))

        // Restoring RSP
        nodes.add(registerUse(rsp) assign (registerUse(rbp) add CFGNode.ConstantKnown(REGISTER_SIZE)))

        // Restoring RBP
        nodes.add(popRegister(rbp))

        return nodes
    }
}

fun generateCall(
    function: FunctionDeclaration,
    arguments: List<CFGNode>,
    result: Register?,
    respectStackAlignment: Boolean = false,
): List<CFGNode> {
    if (function.arguments.size + 1 != arguments.size) {
        throw IllegalArgumentException("Wrong argument count")
    }
    val registerArguments = arguments.zip(REGISTER_ARGUMENT_ORDER)
    val stackArguments = arguments.drop(registerArguments.size).map { Pair(it, Register.VirtualRegister()) }

    val nodes: MutableList<CFGNode> = mutableListOf()

    if (respectStackAlignment) {
        // we push two copies of RSP to the stack and either leave them both there,
        // or remove one of them via RSP assignment
        val oldRSP = Register.VirtualRegister()
        nodes.add(CFGNode.Assignment(RegisterUse(oldRSP), RegisterUse(Register.FixedRegister(HardwareRegister.RSP))))

        nodes.add(CFGNode.Push(RegisterUse(oldRSP)))
        nodes.add(CFGNode.Push(RegisterUse(oldRSP)))

        // in an ideal world we would do something like "and rsp, ~15" or similar; for now this will do
        // at the very least split the computation of (RSP + stackArguments.size % 2 * 8) % 16
        // into two cases depending on the parity of stackArguments.size
        nodes.add(
            CFGNode.Assignment(
                RegisterUse(Register.FixedRegister(HardwareRegister.RSP)),
                Addition(
                    RegisterUse(Register.FixedRegister(HardwareRegister.RSP)),
                    CFGNode.Modulo(
                        Addition(
                            RegisterUse(Register.FixedRegister(HardwareRegister.RSP)),
                            CFGNode.ConstantKnown(stackArguments.size % 2 * REGISTER_SIZE),
                        ),
                        CFGNode.ConstantKnown(16),
                    ),
                ),
            ),
        )
    }

    // in what order should we evaluate arguments? gcc uses reversed order
    for ((argument, register) in registerArguments) {
        nodes.add(CFGNode.Assignment(RegisterUse(Register.FixedRegister(register)), argument))
    }
    for ((argument, register) in stackArguments) {
        nodes.add(CFGNode.Assignment(RegisterUse(register), argument))
    }

    // is this indirection necessary?
    for ((_, register) in stackArguments.reversed()) {
        nodes.add(CFGNode.Push(RegisterUse(register)))
    }

    nodes.add(CFGNode.Call(function))

    if (stackArguments.isNotEmpty()) {
        nodes.add(
            CFGNode.Assignment(
                RegisterUse(Register.FixedRegister(HardwareRegister.RSP)),
                Addition(
                    RegisterUse(Register.FixedRegister(HardwareRegister.RSP)),
                    CFGNode.ConstantKnown(REGISTER_SIZE * stackArguments.size),
                ),
            ),
        )
    }

    if (respectStackAlignment) {
        // we could remove the operations from previous `if (stackArguments.isNotEmpty())` block
        // via MemoryAccess, but for now the semantics are a bit unclear + it would introduce
        // a few ifs, which we do not need at this point
        nodes.add(CFGNode.Pop(RegisterUse(Register.FixedRegister(HardwareRegister.RSP))))
    }

    if (result != null) {
        nodes.add(CFGNode.Assignment(RegisterUse(result), RegisterUse(Register.FixedRegister(HardwareRegister.RAX))))
    }

    return nodes
}
