package cacophony.controlflow

import cacophony.semantic.AnalyzedFunction
import cacophony.semantic.syntaxtree.Definition

class FunctionHandler(
    val function: Definition.FunctionDeclaration,
    val analyzedFunction: AnalyzedFunction,
) {
    sealed class VariableAllocation() {
        data class Reg(val register: Register)

        data class Stack(val offset: Int)
    }

    fun generateCall(
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean = false,
    ): CFGFragment {
        val registerArguments = arguments.zip(REGISTER_ARGUMENT_ORDER)
        val stackArguments = arguments.drop(registerArguments.size).map { Pair(it, Register.Virtual()) }

        val nodes: MutableList<CFGNode> = mutableListOf()

        if (respectStackAlignment) {
            // we push two copies of RSP to the stack and either leave them both there,
            // or remove one of them via RSP assignment
            for (i in 0..2) {
                nodes.add(CFGNode.Push(CFGNode.VariableUse(Register.Fixed(X64Register.RSP))))
            }

            // in an ideal world we would do something like "and rsp, ~15" or similar; for now this will do
            // at the very least split the computation of (RSP + stackArguments.size % 2 * 8) % 16
            // into two cases depending on the parity of stackArguments.size
            nodes.add(
                CFGNode.Assignment(
                    Register.Fixed(X64Register.RSP),
                    CFGNode.Addition(
                        CFGNode.VariableUse(Register.Fixed(X64Register.RSP)),
                        CFGNode.Modulo(
                            CFGNode.Addition(
                                CFGNode.VariableUse(Register.Fixed(X64Register.RSP)),
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
            nodes.add(CFGNode.Assignment(Register.Fixed(register), argument))
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
                    Register.Fixed(X64Register.RSP),
                    CFGNode.Addition(
                        CFGNode.VariableUse(Register.Fixed(X64Register.RSP)),
                        CFGNode.Constant(8 * stackArguments.size),
                    ),
                ),
            )
        }

        if (respectStackAlignment) {
            // we could remove the operations from previous `if (stackArguments.isNotEmpty())` block
            // via MemoryAccess, but for now the semantics are a bit unclear + it would introduce
            // a few ifs, which we do not need at this point
            nodes.add(CFGNode.Pop(Register.Fixed(X64Register.RSP)))
        }

        if (result != null) {
            nodes.add(CFGNode.Assignment(result, CFGNode.VariableUse(Register.Fixed(X64Register.RAX))))
        }

        // should this be a sequence?
        return mapOf(CFGLabel() to CFGVertex.Final(CFGNode.Sequence(nodes)))
    }

    fun generateVariableAccess(
        variable: SourceVariable,
        framePointer: CFGNode,
    ): CFGNode {
        TODO("Not yet implemented")
    }

    fun getVariableAllocation(variable: RealVariable): VariableAllocation {
        TODO("Not yet implemented")
    }

    fun introduceStaticLinksParams(): List<CFGNode> {
        TODO("Not yet implemented")
    }
}
