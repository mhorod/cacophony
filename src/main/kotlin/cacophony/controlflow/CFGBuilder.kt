package cacophony.controlflow

import cacophony.controlflow.functions.defaultCallConvention
import cacophony.controlflow.generation.ProgramCFG
import cacophony.semantic.syntaxtree.Definition

class CFGFragmentBuilder {
    private val labels: MutableMap<String, CFGLabel> = mutableMapOf()
    private val vertices = mutableMapOf<CFGLabel, CFGVertex>()

    private fun getLabel(label: String): CFGLabel = labels.getOrPut(label) { CFGLabel() }

    fun build(): CFGFragment = CFGFragment(vertices, getLabel("entry"))

    fun final(node: () -> CFGNode) = CFGVertex.Final(node())

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

        // prologue
        for ((ind, allocation) in args.withIndex()) {
            single {
                when (allocation) {
                    is VariableAllocation.InRegister -> writeRegister(allocation.register, defaultCallConvention(ind))
                    is VariableAllocation.OnStack ->
                        memoryAccess(registerUse(rsp) sub integer((allocation.offset + 1) * REGISTER_SIZE)) assign
                            defaultCallConvention(ind)
                }
            }
        }
        single { registerUse(rsp) subeq integer(stackSpace) }
        for (register in PRESERVED_REGISTERS.dropLast(1)) {
            single { pushRegister(Register.FixedRegister(register)) }
        }
        // jump to body
        curLabel does jump("bodyEntry") { pushRegister(Register.FixedRegister(PRESERVED_REGISTERS.last())) }

        // epilogue
        curLabel = "exit"
        for (register in PRESERVED_REGISTERS.reversed()) {
            single { popRegister(Register.FixedRegister(register)) }
        }
        curLabel does jump("return") { registerUse(rsp) addeq integer(stackSpace) }

        "return" does final { returnNode }
    }

    infix fun String.does(vertex: CFGVertex) {
        vertices[getLabel(this)] = vertex
    }
}

class CFGBuilder {
    private val programCFG = mutableMapOf<Definition.FunctionDeclaration, CFGFragment>()
    private val registers: MutableMap<String, Register> = mutableMapOf()

    fun fragment(
        function: Definition.FunctionDeclaration,
        args: List<VariableAllocation>,
        stackSpace: Int,
        init: CFGFragmentBuilder.() -> Unit,
    ) {
        val builder = CFGFragmentBuilder()
        builder.init()
        builder.prologueAndEpilogue(args, stackSpace)
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
