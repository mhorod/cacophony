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
    ): CFGFragment {
        TODO("Not yet implemented")
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
