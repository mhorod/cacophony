package cacophony.controlflow.functions

import cacophony.controlflow.*

class PrologueEpilogueHandler(
    private val handler: FunctionHandler,
    private val callConvention: (Int) -> CFGNode,
    private val preservedRegisters: List<HardwareRegister>, // without RSP
) {
    private val numberOfStackVariables =
        with(handler) {
            val variables =
                getFunctionAnalysis()
                    .declaredVariables()
                    .map { getVariableFromDefinition(it.declaration) } +
                    getFunctionAnalysis().auxVariables
            val stackVariables =
                variables
                    .map { getVariableAllocation(it) }
                    .filterIsInstance<VariableAllocation.OnStack>()
                    .sortedBy { it.offset }
            run {
                // Sanity checks
                var offset = 0
                for (variable in stackVariables) {
                    if (variable.offset != offset)
                        throw IllegalStateException("Holes in stack")
                    offset += REGISTER_SIZE
                }
                if (preservedRegisters.contains(HardwareRegister.RSP))
                    throw IllegalArgumentException("RSP amongst call preserved registers")
            }
            stackVariables.size
        }

    private fun lvalueFromAllocation(allocation: VariableAllocation): CFGNode.LValue =
        when (allocation) {
            is VariableAllocation.InRegister -> registerUse(allocation.register)
            is VariableAllocation.OnStack -> memoryAccess(registerUse(rsp) sub integer(allocation.offset + REGISTER_SIZE))
        }

    fun generatePrologue(): List<CFGNode> {
        val nodes = mutableListOf<CFGNode>()
        with(handler) {
            // Move first, then change RSP, so that the offsets from the callConvention hold
            // However, keep the existence of red zone in mind

            // Defined function arguments
            for ((ind, arg) in getFunctionDeclaration().arguments.withIndex()) {
                nodes.add(
                    lvalueFromAllocation(getVariableAllocation(getVariableFromDefinition(arg))) assign
                        callConvention(ind),
                )
            }
            // Static link (implicit arg)
            nodes.add(
                lvalueFromAllocation(getVariableAllocation(getStaticLink())) assign
                    callConvention(getFunctionDeclaration().arguments.size),
            )
            // Space for all stack variables
            nodes.add(registerUse(rsp) subeq integer(numberOfStackVariables * REGISTER_SIZE))
            // Preserved registers
            for (register in preservedRegisters) {
                nodes.add(pushRegister(Register.FixedRegister(register)))
            }
        }
        return nodes
    }

    fun generateEpilogue(): List<CFGNode> {
        val nodes = mutableListOf<CFGNode>()
        // Restoring preserved registers
        for (register in preservedRegisters.reversed()) {
            nodes.add(popRegister(Register.FixedRegister(register)))
        }
        // Restoring RSP
        nodes.add(registerUse(rsp) addeq integer(numberOfStackVariables * REGISTER_SIZE))
        return nodes
    }
}
