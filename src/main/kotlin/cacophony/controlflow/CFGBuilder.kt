package cacophony.controlflow

import cacophony.controlflow.functions.CallConvention
import cacophony.controlflow.functions.SystemVAMD64CallConvention
import cacophony.controlflow.generation.ProgramCFG
import cacophony.semantic.syntaxtree.Definition

class CFGFragmentBuilder {
    private val labels: MutableMap<String, CFGLabel> = mutableMapOf()
    private val vertices = mutableMapOf<CFGLabel, CFGVertex>()
    private val resultRegister = Register.VirtualRegister()
    private val callConvention: CallConvention = SystemVAMD64CallConvention

    private fun getLabel(label: String): CFGLabel = labels.getOrPut(label) { CFGLabel() }

    fun getResultRegister() = resultRegister

    fun build(): CFGFragment = CFGFragment(vertices, getLabel("entry"))

    fun jump(destination: String, node: () -> CFGNode): CFGVertex = CFGVertex.Jump(node(), getLabel(destination))

    fun conditional(trueDestination: String, falseDestination: String, condition: () -> CFGNode): CFGVertex =
        CFGVertex.Conditional(condition(), getLabel(trueDestination), getLabel(falseDestination))

    fun prologueAndEpilogue(args: List<VariableAllocation>, stackSpace: Int) {
        var curLabel = "entry"
        var nextLabel = 1
        val single = { getNode: () -> CFGNode ->
            curLabel does jump(nextLabel.toString(), getNode)
            curLabel = nextLabel.toString()
            nextLabel++
        }
        val spaceForPreservedRegisters = callConvention.preservedRegisters().map { Register.VirtualRegister() }
        // prologue
        single { pushRegister(rbp) }
        single { registerUse(rbp) assign (registerUse(rsp) sub CFGNode.ConstantKnown(REGISTER_SIZE)) }
        single { registerUse(rsp) subeq CFGNode.ConstantLazy { stackSpace } }
        for ((source, destination) in callConvention.preservedRegisters() zip spaceForPreservedRegisters) {
            single { registerUse(destination) assign registerUse(Register.FixedRegister(source)) }
        }
        for ((ind, allocation) in args.dropLast(1).withIndex()) {
            single { wrapAllocation(allocation) assign wrapAllocation(callConvention.argumentAllocation(ind)) }
        }
        // jump to body
        curLabel does
            jump("bodyEntry") {
                wrapAllocation(args.last()) assign wrapAllocation(callConvention.argumentAllocation(args.size - 1))
            }

        // epilogue
        curLabel = "exit"
        for ((destination, source) in callConvention.preservedRegisters() zip spaceForPreservedRegisters) {
            single { registerUse(Register.FixedRegister(destination)) assign registerUse(source) }
        }
        single { registerUse(Register.FixedRegister(callConvention.returnRegister())) assign registerUse(getResultRegister()) }
        single { registerUse(rsp) assign (registerUse(rbp) add CFGNode.ConstantKnown(REGISTER_SIZE)) }
        curLabel does jump("return") { popRegister(rbp) }
        "return" does CFGVertex.Final(returnNode)
    }

    infix fun String.does(vertex: CFGVertex) {
        vertices[getLabel(this)] = vertex
    }
}

class CFGBuilder {
    private val programCFG = mutableMapOf<Definition.FunctionDefinition, CFGFragment>()
    private val registers: MutableMap<String, Register> = mutableMapOf()

    fun fragment(
        function: Definition.FunctionDefinition,
        args: List<VariableAllocation>,
        stackSpace: Int,
        init: CFGFragmentBuilder.() -> Unit,
    ) {
        val builder = CFGFragmentBuilder()
        builder.prologueAndEpilogue(args, stackSpace)
        builder.init()
        programCFG[function] = builder.build()
    }

    fun build(): ProgramCFG = programCFG

    fun virtualRegister(name: String): Register = registers.getOrPut(name) { Register.VirtualRegister() }

    fun writeRegister(name: String, node: CFGNode) = writeRegister(virtualRegister(name), node)

    fun pushRegister(name: String) = pushRegister(virtualRegister(name))

    fun readRegister(name: String) = registerUse(virtualRegister(name))
}

fun cfg(init: CFGBuilder.() -> Unit): ProgramCFG {
    val builder = CFGBuilder()
    builder.init()
    return builder.build()
}
