package cacophony.controlflow

import cacophony.controlflow.CFGNode.Addition
import cacophony.controlflow.CFGNode.Constant
import cacophony.controlflow.CFGNode.MemoryAccess
import cacophony.controlflow.CFGNode.VariableUse
import cacophony.semantic.AnalyzedFunction
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Definition.FunctionDeclaration
import cacophony.utils.CompileException

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

    fun generateCall(
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean = true,
    ): List<CFGNode>

    fun generateCallFrom(
        callerFunction: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean = true,
    ): List<CFGNode>

    fun generateVariableAccess(variable: Variable): CFGNode

    fun getVariableAllocation(variable: Variable): VariableAllocation

    // Returns static link to parent
    fun getStaticLink(): Variable.AuxVariable.StaticLinkVariable

    fun getVariableFromDefinition(varDef: Definition): Variable
}

class GenerateVariableAccessException(
    reason: String,
) : CompileException(reason)

class FunctionHandlerImpl(
    private val function: FunctionDeclaration,
    private val analyzedFunction: AnalyzedFunction,
    // List of parents' handlers ordered from immediate parent.
    private val ancestorFunctionHandlers: List<FunctionHandler>,
) : FunctionHandler {
    private val staticLink: Variable.AuxVariable.StaticLinkVariable = Variable.AuxVariable.StaticLinkVariable()
    private val definitionToVariable = analyzedFunction.variables.associate { it.declaration to Variable.SourceVariable(it.declaration) }

    private val variableAllocation: MutableMap<Variable, VariableAllocation> =
        run {
            val res = mutableMapOf<Variable, VariableAllocation>()
            val usedVars = analyzedFunction.variablesUsedInNestedFunctions
            val regVar =
                analyzedFunction.variables
                    .map { it.declaration }
                    .toSet()
                    .minus(usedVars)
            regVar.forEach { varDef ->
                res[definitionToVariable[varDef]!!] = VariableAllocation.InRegister(Register.VirtualRegister())
            }
            var offset = 0
            usedVars.forEach { varDef ->
                res[definitionToVariable[varDef]!!] = VariableAllocation.OnStack(offset)
                offset += 8
            }
            res
        }

    init {
        introduceStaticLinksParams()
    }

    private fun registerVariableAllocation(
        variable: Variable,
        allocation: VariableAllocation,
    ) {
        variableAllocation[variable] = allocation
    }

    override fun getFunctionDeclaration(): FunctionDeclaration = function

    override fun generateCall(
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean,
    ): List<CFGNode> {
        if (function.arguments.size != arguments.size) {
            throw IllegalArgumentException("Unexpected number of arguments")
        }

        val registerArguments = arguments.zip(REGISTER_ARGUMENT_ORDER)
        val stackArguments = arguments.drop(registerArguments.size).map { Pair(it, Register.VirtualRegister()) }

        val nodes: MutableList<CFGNode> = mutableListOf()

        if (respectStackAlignment) {
            // we push two copies of RSP to the stack and either leave them both there,
            // or remove one of them via RSP assignment
            val oldRSP = Register.VirtualRegister()
            nodes.add(CFGNode.Assignment(oldRSP, CFGNode.VariableUse(Register.FixedRegister(X64Register.RSP))))

            nodes.add(CFGNode.Push(CFGNode.VariableUse(oldRSP)))
            nodes.add(CFGNode.Push(CFGNode.VariableUse(oldRSP)))

            // in an ideal world we would do something like "and rsp, ~15" or similar; for now this will do
            // at the very least split the computation of (RSP + stackArguments.size % 2 * 8) % 16
            // into two cases depending on the parity of stackArguments.size
            nodes.add(
                CFGNode.Assignment(
                    Register.FixedRegister(X64Register.RSP),
                    CFGNode.Addition(
                        CFGNode.VariableUse(Register.FixedRegister(X64Register.RSP)),
                        CFGNode.Modulo(
                            CFGNode.Addition(
                                CFGNode.VariableUse(Register.FixedRegister(X64Register.RSP)),
                                CFGNode.Constant(stackArguments.size % 2 * 8),
                            ),
                            CFGNode.Constant(16),
                        ),
                    ),
                ),
            )
        }

        // in what order should we evaluate arguments? gcc uses reversed order
        for ((argument, register) in registerArguments) {
            nodes.add(CFGNode.Assignment(Register.FixedRegister(register), argument))
        }
        for ((argument, register) in stackArguments) {
            nodes.add(CFGNode.Assignment(register, argument))
        }

        // is this indirection necessary?
        for ((_, register) in stackArguments.reversed()) {
            nodes.add(CFGNode.Push(CFGNode.VariableUse(register)))
        }

        nodes.add(CFGNode.Call(function))

        if (stackArguments.isNotEmpty()) {
            nodes.add(
                CFGNode.Assignment(
                    Register.FixedRegister(X64Register.RSP),
                    CFGNode.Addition(
                        CFGNode.VariableUse(Register.FixedRegister(X64Register.RSP)),
                        CFGNode.Constant(8 * stackArguments.size),
                    ),
                ),
            )
        }

        if (respectStackAlignment) {
            // we could remove the operations from previous `if (stackArguments.isNotEmpty())` block
            // via MemoryAccess, but for now the semantics are a bit unclear + it would introduce
            // a few ifs, which we do not need at this point
            nodes.add(CFGNode.Pop(Register.FixedRegister(X64Register.RSP)))
        }

        if (result != null) {
            nodes.add(CFGNode.Assignment(result, CFGNode.VariableUse(Register.FixedRegister(X64Register.RAX))))
        }

        return nodes
    }

    // Wrapper for generateCall that additionally fills staticLink to parent function.
    // Since staticLink is not property of node itself, but rather of its children,
    // if caller is immediate parent, we have to fetch RBP instead.
    override fun generateCallFrom(
        callerFunction: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean,
    ): List<CFGNode> {
        val nodes: MutableList<CFGNode> = mutableListOf()
        val staticLinkAccess: CFGNode = generateVariableAccess(staticLink)
        if (ancestorFunctionHandlers.isNotEmpty() && callerFunction === ancestorFunctionHandlers[0]) {
            // Function is called from parent which doesn't have access to variable with its static pointer,
            // we need to get it from RBP.
            nodes.add(
                CFGNode.MemoryWrite(
                    CFGNode.MemoryAccess(staticLinkAccess),
                    CFGNode.VariableUse(Register.FixedRegister(X64Register.RBP)),
                ),
            )
        } else {
            // It's called from nested function, therefore caller should have access to staticLink.
            nodes.add(
                CFGNode.MemoryWrite(
                    CFGNode.MemoryAccess(staticLinkAccess),
                    callerFunction.generateVariableAccess(staticLink),
                ),
            )
        }

        nodes.addAll(generateCall(arguments, result, respectStackAlignment))
        return nodes
    }

    private fun generateAccessToFramePointer(other: FunctionDeclaration): CFGNode {
        if (other == function) {
            return VariableUse(Register.FixedRegister(X64Register.RBP))
        } else {
            val childOfOtherIndex =
                ancestorFunctionHandlers
                    .indexOfFirst { it.getFunctionDeclaration() == other } - 1

            val otherFramePointerVariable =
                (ancestorFunctionHandlers.getOrNull(childOfOtherIndex) ?: this)
                    .getStaticLink()

            return generateVariableAccess(otherFramePointerVariable)
        }
    }

    override fun generateVariableAccess(variable: Variable): CFGNode {
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
                //   "Cannot generate access to variables other than static links and source variables.",
                // )
            }

        if (definedInDeclaration == null) {
            throw GenerateVariableAccessException("Function $function has no access to $variable.")
        }

        val definedInFunctionHandler =
            ancestorFunctionHandlers.find { it.getFunctionDeclaration() == definedInDeclaration } ?: this

        return when (val variableAllocation = definedInFunctionHandler.getVariableAllocation(variable)) {
            is VariableAllocation.InRegister -> {
                VariableUse(variableAllocation.register)
            }
            is VariableAllocation.OnStack -> {
                MemoryAccess(
                    Addition(
                        generateAccessToFramePointer(definedInDeclaration),
                        Constant(variableAllocation.offset),
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

    // Creates staticLink auxVariable in analyzedFunction, therefore shouldn't be called multiple times.
    // Static link is created even if parent doesn't exist.
    private fun introduceStaticLinksParams() {
        // If we agree on stack frame layout someone may want to modify it to onStack variable with offset 0,
        // didn't do it now in case someone does the same in other place.
        registerVariableAllocation(
            staticLink,
            VariableAllocation.InRegister(Register.VirtualRegister()),
        )
        analyzedFunction.auxVariables.add(staticLink)
    }
}
