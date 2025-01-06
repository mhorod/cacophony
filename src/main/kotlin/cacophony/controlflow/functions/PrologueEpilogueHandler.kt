package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.generation.Layout
import cacophony.semantic.syntaxtree.BaseType

class PrologueEpilogueHandler(
    private val handler: FunctionHandler,
    private val callConvention: CallConvention,
    private val stackSpace: CFGNode.ConstantLazy,
    private val flattenedArguments: List<CFGNode>,
    private val resultAccess: Layout,
) {
    // Preserved registers cannot contain references
    private val spaceForPreservedRegisters: List<Register.VirtualRegister> =
        callConvention.preservedRegisters().map {
            Register.VirtualRegister()
        }

    fun generatePrologue(): List<CFGNode> {
        val nodes = mutableListOf<CFGNode>()
        nodes.add(pushRegister(rbp, false))
        nodes.add(registerUse(rbp, false) assign (registerUse(rsp, false) sub CFGNode.ConstantKnown(REGISTER_SIZE)))
        nodes.add(registerUse(rsp, false) subeq stackSpace)

        // Preserved registers don't hold references
        for ((source, destination) in callConvention.preservedRegisters() zip spaceForPreservedRegisters) {
            nodes.add(registerUse(destination, false) assign registerUse(Register.FixedRegister(source), false))
        }

//        val isReference = argTypes.map { it is BaseType.Referential } + List(arguments.size - argTypes.size) { false }
        val isReference = handler.getFunctionDeclaration().arguments.map { it.type is BaseType.Referential } + listOf(false)
        // Defined function arguments
        for ((ind, destination) in flattenedArguments.zip(isReference).withIndex()) {
            require(destination.first is CFGNode.LValue)
            nodes.add(
                (destination.first as CFGNode.LValue) assign
                    wrapAllocation(callConvention.argumentAllocation(ind), destination.second),
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
            nodes.add(registerUse(Register.FixedRegister(destination), false) assign registerUse(source, false))
        }

        nodes.addAll(
            resultAccess.flatten().withIndex().map { (i, accessInfo) ->
                wrapAllocation(getReturnLocation(i), accessInfo.holdsReference) assign accessInfo.access
            },
        )

        // Restoring RSP
        nodes.add(registerUse(rsp, false) assign (registerUse(rbp, false) add CFGNode.ConstantKnown(REGISTER_SIZE)))

        // Restoring RBP
        nodes.add(popRegister(rbp, false))

        return nodes
    }
}
