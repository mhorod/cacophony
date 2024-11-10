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
        callerFunction: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
    ): CFGFragment

    fun generateVariableAccess(variable: Variable): CFGNode

    fun getVariableAllocation(variable: Variable): VariableAllocation

    // Returns static link to parent
    fun getStaticLink(): Variable.AuxVariable.StaticLinkVariable
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

    override fun getFunctionDeclaration(): FunctionDeclaration = function

    override fun generateCall(
        arguments: List<CFGNode>,
        result: Register?,
    ): List<CFGNode> {
        TODO("Not yet implemented")
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

    override fun getVariableAllocation(variable: Variable): VariableAllocation {
        TODO("Not yet implemented")
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
