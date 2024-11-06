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
