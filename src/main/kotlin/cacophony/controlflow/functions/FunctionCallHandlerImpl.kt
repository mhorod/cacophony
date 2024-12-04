package cacophony.controlflow.functions

import cacophony.controlflow.CFGNode
import cacophony.controlflow.CFGNode.RegisterUse
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition

open class FunctionCallHandlerImpl(
    val function: FunctionDefinition,
    // List of parents' handlers ordered from immediate parent.
    private val ancestorFunctionHandlers: List<FunctionHandler>,
) : FunctionCallHandler {
    // Wrapper for generateCall that additionally fills staticLink to parent function.
    // Since staticLink is not property of node itself, but rather of its children,
    // if caller is immediate parent, we have to fetch RBP instead.
    override fun generateCallFrom(
        callerFunction: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean,
    ): List<CFGNode> {
        val staticLinkVar =
            if (ancestorFunctionHandlers.isEmpty() || callerFunction === ancestorFunctionHandlers.first()) {
                RegisterUse(Register.FixedRegister(HardwareRegister.RBP))
            } else {
                callerFunction.generateAccessToFramePointer(ancestorFunctionHandlers.first().getFunctionDeclaration())
            }

        return generateCall(function, arguments + mutableListOf(staticLinkVar), result, respectStackAlignment)
    }
}
