package cacophony.controlflow

import cacophony.semantic.AnalyzedFunction
import cacophony.semantic.syntaxtree.Definition.FunctionDeclaration
import cacophony.utils.CompileException

sealed class VariableAllocation {
    class InRegister(
        register: Register,
    ) : VariableAllocation()

    class OnStack(
        offset: Int,
    ) : VariableAllocation()
}

interface FunctionHandler {
    fun getFunctionDeclaration(): FunctionDeclaration

    fun generateCall(
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean = false,
    ): CFGFragment

    fun generateVariableAccess(variable: Variable): CFGNode

    fun getVariableAllocation(variable: Variable): VariableAllocation
}

class GenerateVariableAccessException(
    reason: String,
) : CompileException(reason)

class FunctionHandlerImpl(
    private val function: FunctionDeclaration,
    private val analyzedFunction: AnalyzedFunction,
) : FunctionHandler {
    override fun getFunctionDeclaration(): FunctionDeclaration = function

    override fun generateCall(
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean,
    ): CFGFragment {
        val registerArguments = arguments.zip(REGISTER_ARGUMENT_ORDER)
        val stackArguments = arguments.drop(registerArguments.size).map { Pair(it, Register.VirtualRegister()) }

        val nodes: MutableList<CFGNode> = mutableListOf()

        if (respectStackAlignment) {
            // we push two copies of RSP to the stack and either leave them both there,
            // or remove one of them via RSP assignment
            val oldRSP = Register.VirtualRegister()
            nodes.add(CFGNode.Assignment(oldRSP, CFGNode.VariableUse(Register.FixedRegister(X64Register.RSP))))
            for (i in 0..2) {
                nodes.add(CFGNode.Push(CFGNode.VariableUse(oldRSP)))
            }

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

        // should this be a sequence?
        return mapOf(CFGLabel() to CFGVertex.Final(CFGNode.Sequence(nodes)))
    }

    override fun generateVariableAccess(variable: Variable): CFGNode {
        TODO("Not yet implemented")
    }

    override fun getVariableAllocation(variable: Variable): VariableAllocation {
        TODO("Not yet implemented")
    }

    private fun introduceStaticLinksParams(): List<CFGNode> {
        TODO("Not yet implemented")
    }
}
