package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.generation.Layout

class PrologueEpilogueHandler(
    private val handler: FunctionHandler,
    private val callConvention: CallConvention,
    private val stackSpace: CFGNode.ConstantLazy,
    private val flattenedArguments: List<CFGNode>,
    private val resultAccess: Layout,
) {
    private val spaceForPreservedRegisters: List<Register.VirtualRegister> =
        callConvention.preservedRegisters().map {
            Register.VirtualRegister()
        }

    fun generatePrologue(): List<CFGNode> {
        val nodes = mutableListOf<CFGNode>()
        nodes.add(pushRegister(rbp))
        nodes.add(registerUse(rbp) assign (registerUse(rsp) sub CFGNode.ConstantKnown(REGISTER_SIZE)))
        nodes.add(registerUse(rsp) subeq stackSpace)

        // Preserved registers
        for ((source, destination) in callConvention.preservedRegisters() zip spaceForPreservedRegisters) {
            nodes.add(registerUse(destination) assign registerUse(Register.FixedRegister(source)))
        }

        // Defined function arguments
        for ((ind, destination) in flattenedArguments.withIndex()) {
            require(destination is CFGNode.LValue)
            nodes.add(
                destination assign
                    wrapAllocation(callConvention.argumentAllocation(ind)),
            )
        }
        return nodes
    }

    private val returnLocations =
        listOf(
            VariableAllocation.InRegister(Register.FixedRegister(HardwareRegister.RAX)),
            VariableAllocation.InRegister(Register.FixedRegister(HardwareRegister.RDI)),
            VariableAllocation.InRegister(Register.FixedRegister(HardwareRegister.RSI)),
            VariableAllocation.InRegister(Register.FixedRegister(HardwareRegister.RDX)),
            VariableAllocation.InRegister(Register.FixedRegister(HardwareRegister.RCX)),
            VariableAllocation.InRegister(Register.FixedRegister(HardwareRegister.R8)),
            VariableAllocation.InRegister(Register.FixedRegister(HardwareRegister.R9)),
        )

    private fun getReturnLocation(index: Int): VariableAllocation =
        callConvention.returnAllocation(index, handler.getFunctionDeclaration().arguments.sumOf { it.type.size() })

    fun generateEpilogue(): List<CFGNode> {
        val nodes = mutableListOf<CFGNode>()
        // Restoring preserved registers
        for ((destination, source) in callConvention.preservedRegisters() zip spaceForPreservedRegisters) {
            nodes.add(registerUse(Register.FixedRegister(destination)) assign registerUse(source))
        }

        nodes.addAll(
            resultAccess.flatten().withIndex().map { (i, access) ->
                wrapAllocation(getReturnLocation(i)) assign access
            },
        )

        // Restoring RSP
        nodes.add(registerUse(rsp) assign (registerUse(rbp) add CFGNode.ConstantKnown(REGISTER_SIZE)))

        // Restoring RBP
        nodes.add(popRegister(rbp))

        return nodes
    }
}
