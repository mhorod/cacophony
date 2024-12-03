package cacophony.controlflow.functions

import cacophony.controlflow.*

class PrologueEpilogueHandler(
    private val handler: FunctionHandler,
    private val callConvention: CallConvention,
    private val stackSpace: CFGNode.ConstantLazy,
    private val resultAccess: Register.VirtualRegister,
) {
    private val spaceForPreservedRegisters: List<Register.VirtualRegister> =
        callConvention.preservedRegisters().map {
            Register.VirtualRegister()
        }

    fun generatePrologue(): List<CFGNode> {
        val nodes = mutableListOf<CFGNode>()
        with(handler) {
            nodes.add(pushRegister(rbp))
            nodes.add(registerUse(rbp) assign (registerUse(rsp) sub CFGNode.ConstantKnown(REGISTER_SIZE)))
            nodes.add(registerUse(rsp) subeq stackSpace)

            // Preserved registers
            for ((source, destination) in callConvention.preservedRegisters() zip spaceForPreservedRegisters) {
                nodes.add(registerUse(destination) assign registerUse(Register.FixedRegister(source)))
            }

            // Defined function arguments
            for ((ind, arg) in getFunctionDeclaration().arguments.withIndex()) {
                nodes.add(
                    wrapAllocation(getVariableAllocation(getVariableFromDefinition(arg))) assign
                        wrapAllocation(callConvention.argumentAllocation(ind + 1)),
                )
            }

            // Static link (implicit arg)
            nodes.add(
                wrapAllocation(getVariableAllocation(getStaticLink())) assign
                    wrapAllocation(callConvention.argumentAllocation(getFunctionDeclaration().arguments.size)),
            )
        }
        return nodes
    }

    fun generateEpilogue(): List<CFGNode> {
        val nodes = mutableListOf<CFGNode>()
        // Restoring preserved registers
        for ((destination, source) in callConvention.preservedRegisters() zip spaceForPreservedRegisters) {
            nodes.add(registerUse(Register.FixedRegister(destination)) assign registerUse(source))
        }

        // Write the result to its destination
        nodes.add(registerUse(Register.FixedRegister(callConvention.returnRegister())) assign registerUse(resultAccess))

        // Restoring RSP
        nodes.add(registerUse(rsp) assign (registerUse(rbp) add CFGNode.ConstantKnown(REGISTER_SIZE)))

        // Restoring RBP
        nodes.add(popRegister(rbp))

        return nodes
    }
}
