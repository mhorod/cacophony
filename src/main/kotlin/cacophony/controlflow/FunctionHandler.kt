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

    // TODO discuss this
    private fun getIdentifier(): String {
        return function.identifier + function.arguments.size.toString()
    }

    fun generateCall(
        arguments: List<CFGNode>,
        result: Register?,
    ): CFGFragment {
        val registerArguments = arguments.zip(REGISTER_ARGUMENT_ORDER)
        val stackArguments = arguments.drop(registerArguments.size).map { Pair(it, Register.Virtual()) }

        val nodes: MutableList<CFGNode> = mutableListOf()

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

        nodes.add(CFGNode.Call(getIdentifier()))

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
