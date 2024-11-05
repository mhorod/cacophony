package cacophony.controlflow

import cacophony.semantic.AnalyzedFunction

class FunctionHandler(val function: AnalyzedFunction) {
    sealed class VariableAllocation() {
        data class Reg(val register: VirtualRegister)

        data class Stack(val offset: Int)
    }

    fun generateCall(
        arguments: List<CFGNode>,
        result: Register?,
    ): CFGFragment {
        TODO("Not yet implemented")
    }

    fun generateVariableAccess(
        variable: SourceVariable,
        framePointer: CFGNode,
    ): CFGNode {
        TODO("Not yet implemented")
    }

    fun getVariableAllocation(variable: SourceVariable): VariableAllocation {
        TODO("Not yet implemented")
    }

    fun introduceStaticLinksParams(): List<CFGNode> {
        TODO("Not yet implemented")
    }
}
