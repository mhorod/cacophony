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

    // Use this whenever you create a new Variable (Aux or Source) or change
    // allocation of an existing one, so that the generateVariableAccess works properly
    fun registerVariable(
        variable: Variable,
        allocation: VariableAllocation,
    )
}

class GenerateVariableAccessException(
    reason: String,
) : CompileException(reason)

class FunctionHandlerImpl(
    private val function: FunctionDeclaration,
    private val analyzedFunction: AnalyzedFunction,
) : FunctionHandler {
    private val variableAllocation: MutableMap<Variable, VariableAllocation> = mutableMapOf()

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

    // Use this function, whenever you create new AuxVariable
    override fun registerVariable(
        variable: Variable,
        allocation: VariableAllocation,
    ) {
        variableAllocation[variable] = allocation
    }

    private fun getVariableAllocation(variable: Variable): VariableAllocation =
        variableAllocation.getOrElse(variable) {
            throw IllegalArgumentException("Variable $variable have not been registered inside $this FunctionHandler")
        }

    private fun introduceStaticLinksParams(): List<CFGNode> {
        TODO("Not yet implemented")
    }
}
