package cacophony.controlflow.functions

import cacophony.codegen.BlockLabel
import cacophony.controlflow.*
import cacophony.controlflow.generation.Layout
import cacophony.semantic.rtti.getStackFrameLocation
import cacophony.semantic.syntaxtree.BaseType

class PrologueEpilogueHandler(
    private val handler: CallableHandler,
    private val callConvention: CallConvention,
    private val stackSpace: CFGNode.ConstantLazy,
    private val flattenedArguments: List<CFGNode>,
    private val resultAccess: Layout,
    private val heapVariablePointers: Map<Variable.PrimitiveVariable, Variable.PrimitiveVariable>,
    // TODO: heap variables should be allocated in prologue
) {
    // Preserved registers cannot contain references
    private val spaceForPreservedRegisters: List<Register.VirtualRegister> =
        callConvention.preservedRegisters().map {
            Register.VirtualRegister()
        }

    fun generatePrologue(): List<CFGNode> {
        val nodes = mutableListOf<CFGNode>()
        nodes.add(registerUse(rsp) subeq integer(REGISTER_SIZE))
        // TODO: how to cover lambdas here?
        nodes.add(pushLabel(getStackFrameLocation((handler as FunctionHandler).getFunctionDeclaration())))
        nodes.add(pushRegister(rbp, false))
        nodes.add(registerUse(rbp, false) assign registerUse(rsp, false))
        nodes.add(registerUse(rsp, false) subeq stackSpace)

        nodes.add(CFGNode.RawCall(BlockLabel.cleanReferences))

        // Preserved registers don't hold references
        for ((source, destination) in callConvention.preservedRegisters() zip spaceForPreservedRegisters) {
            nodes.add(registerUse(destination, false) assign registerUse(Register.FixedRegister(source), false))
        }

        val isReference =
            handler
                .getAnalyzedFunction()
                .arguments
                .flatMap { it.type.flatten() }
                .map { it is BaseType.Referential } + listOf(false)

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
        // + 1 argument for static link
        callConvention.returnAllocation(index, handler.getAnalyzedFunction().arguments.sumOf { it.type.size() } + 1)

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
        nodes.add(writeRegister(rsp, registerUse(rbp, false) add integer(3 * REGISTER_SIZE)))

        // Restoring RBP
        nodes.add(writeRegister(rbp, memoryAccess(registerUse(rsp, false) sub integer(3 * REGISTER_SIZE))))

        return nodes
    }
}
