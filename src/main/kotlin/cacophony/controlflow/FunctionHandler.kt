package cacophony.controlflow

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

    init {
        introduceStaticLinksParams()
    }

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
    ): CFGFragment {
        val nodes: MutableList<CFGNode> = mutableListOf()
        val staticLinkAccess: CFGNode = generateVariableAccess(staticLink)
        if (analyzedFunction.parentLink?.parent === getFunctionDeclaration()) {
            // Function is called from parent which doesn't have access to variable with its static pointer,
            // we need to get it from RBP.
            nodes.add(
                CFGNode.MemoryWrite(
                    CFGNode.MemoryAccess(staticLinkAccess),
                    CFGNode.VariableUse(Register.FixedRegister("RBP")),
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

        nodes.addAll(generateCall(arguments, result))
        return mapOf(CFGLabel() to CFGVertex.Final(CFGNode.Sequence(nodes)))
    }

    override fun generateVariableAccess(variable: Variable): CFGNode {
        TODO("Not yet implemented")
    }

    override fun getVariableAllocation(variable: Variable): VariableAllocation =
        variableAllocation.getOrElse(variable) {
            throw IllegalArgumentException("Variable $variable have not been allocated inside $this FunctionHandler")
        }

    override fun getVariableFromDefinition(varDef: Definition): Variable =
        definitionToVariable.getOrElse(varDef) {
            throw IllegalArgumentException("Variable $varDef have not been defined inside function $function")
        }

    override fun getStaticLink(): Variable.AuxVariable.StaticLinkVariable {
        return staticLink
    }

    // Creates staticLink auxVariable in analyzedFunction, therefore shouldn't be called multiple times.
    // Static link is created even if parent doesn't exist.
    private fun introduceStaticLinksParams() {
        // I truly don't know who is responsible for layout of the stack
        // TODO: uncomment (and fix) after #127 is merged
//            registerVariable(
//                staticLink,
//                VariableAllocation.OnStack(/*what should be there?*/),
//            )
        analyzedFunction.auxVariables.add(staticLink)
    }
}
