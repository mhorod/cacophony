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

    private fun getIdentifier(): String {
        return function.identifier + function.arguments.size.toString()
    }

    fun generateCall(
        arguments: List<CFGNode>,
        result: Register?,
    ): CFGFragment {
        val registerArguments = arguments.take(REGISTER_ARGUMENT_ORDER.size)
        val registerDestinations = REGISTER_ARGUMENT_ORDER.take(registerArguments.size)

        val stackArguments = arguments.drop(REGISTER_ARGUMENT_ORDER.size)
        val stackVirtualRegisters = stackArguments.map { Register.Virtual() }

        val nodes: MutableList<CFGNode> = mutableListOf()

        // in what order should we evaluate arguments? gcc uses reversed order
        for ((argument, register) in registerArguments.zip(registerDestinations)) {
            nodes.add(CFGNode.Assignment(Register.Fixed(register), argument))
        }
        for ((argument, register) in stackArguments.zip(stackVirtualRegisters)) {
            nodes.add(CFGNode.Assignment(register, argument))
        }

        // possible optimization for later: skip last push/pop
        for (register in stackVirtualRegisters.reversed()) {
            nodes.add(CFGNode.Pop(register))
        }
        nodes.add(CFGNode.Call(getIdentifier()))

        // possible optimization for later: change to "add rsp, 8*stackArgumentCount"
        for (argument in stackArguments) {
            nodes.add(CFGNode.Pop())
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
