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
    ): List<CFGNode>

    fun generateCallFrom(
        function: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
    ): CFGFragment

    fun generateVariableAccess(variable: Variable): CFGNode

    fun getVariableAllocation(variable: Variable): VariableAllocation

    fun getStaticLink(): Variable.AuxVariable.StaticLinkVariable
}

class GenerateVariableAccessException(
    reason: String,
) : CompileException(reason)

class FunctionHandlerImpl(
    private val function: FunctionDeclaration,
    private val analyzedFunction: AnalyzedFunction,
    // List of parents' handlers ordered from immediate parent.
    private val ancestorFunctionHandlers: List<FunctionHandler>
) : FunctionHandler {
    private val staticLink: Variable.AuxVariable.StaticLinkVariable = Variable.AuxVariable.StaticLinkVariable()

    override fun getFunctionDeclaration(): FunctionDeclaration = function

    override fun generateCall(
        arguments: List<CFGNode>,
        result: Register?,
    ): List<CFGNode> {
        TODO("Not yet implemented")
    }

    // Wrapper for generateCall that additionally fills staticLink to parent function.
    // Generally when we call another function we have to pass the staticLink to it,
    // we have to find parent of called function and use variableAccess to its staticLink variable.
    // It should be possible both from the parent itself and from nested functions,
    // therefore there is option that both will have this staticLink,
    // or it can be ified in such a way that calling nested functions uses RBP directly.
    // I've decided to keep both links (to itself and to parent) in auxVariables.
    // If it causes problems it can be modified to store only staticLink to parent.
    override fun generateCallFrom(
        caller: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
    ): CFGFragment {
        val nodes: MutableList<CFGNode> = mutableListOf()
        if(ancestorFunctionHandlers.isNotEmpty()) {
            val parentFunction = ancestorFunctionHandlers[0]
            val parentLink = parentFunction.getStaticLink()
            nodes.add(
                CFGNode.MemoryWrite(
                    CFGNode.MemoryAccess(caller.generateVariableAccess(parentLink)),
                    CFGNode.MemoryAccess(this.generateVariableAccess(parentLink))
                )
            )
        }
        nodes.add(
            CFGNode.MemoryWrite(
                CFGNode.MemoryAccess(generateVariableAccess(staticLink)),
                CFGNode.VariableUse(Register.FixedRegister("RBP"))
            )
        )

        nodes.addAll(generateCall(arguments, result))
        return mapOf(CFGLabel() to CFGVertex.Final(CFGNode.Sequence(nodes)))
    }

    override fun generateVariableAccess(variable: Variable): CFGNode {
        TODO("Not yet implemented")
    }

    override fun getVariableAllocation(variable: Variable): VariableAllocation {
        TODO("Not yet implemented")
    }

    override fun getStaticLink(): Variable.AuxVariable.StaticLinkVariable {
        return staticLink
    }

    // Creates staticLink auxVariable in analyzedFunction, therefore shouldn't be called multiple times.
    private fun introduceStaticLinksParams() {
        analyzedFunction.auxVariables.add(staticLink)
        if (ancestorFunctionHandlers.isNotEmpty()) {
            analyzedFunction.auxVariables.add(ancestorFunctionHandlers[0].getStaticLink())
        }
    }
}
