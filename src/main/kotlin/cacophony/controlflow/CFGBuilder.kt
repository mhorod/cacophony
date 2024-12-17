package cacophony.controlflow

import cacophony.controlflow.generation.ProgramCFG
import cacophony.semantic.syntaxtree.Definition

class CFGFragmentBuilder(private val registers: MutableMap<String, Register>) {
    private val labels: MutableMap<String, CFGLabel> = mutableMapOf()
    private val vertices = mutableMapOf<CFGLabel, CFGVertex>()
    private val resultRegister = Register.VirtualRegister()

    private fun getLabel(label: String): CFGLabel = labels.getOrPut(label) { CFGLabel() }

    fun getResultRegister() = resultRegister

    fun build(): CFGFragment = CFGFragment(vertices, getLabel("entry"))

    fun jump(destination: String, node: () -> CFGNode): CFGVertex = CFGVertex.Jump(node(), getLabel(destination))

    fun final(node: () -> CFGNode): CFGVertex = CFGVertex.Final(node())

    fun conditional(trueDestination: String, falseDestination: String, condition: () -> CFGNode): CFGVertex =
        CFGVertex.Conditional(condition(), getLabel(trueDestination), getLabel(falseDestination))

    fun virtualRegister(name: String): Register = registers.getOrPut(name) { Register.VirtualRegister() }

    fun writeRegister(name: String, node: CFGNode) = writeRegister(virtualRegister(name), node)

    fun writeRegister(register: Register, name: String) = writeRegister(register, registerUse(virtualRegister(name)))

    fun pushRegister(name: String) = pushRegister(virtualRegister(name))

    fun readRegister(name: String) = registerUse(virtualRegister(name))

    infix fun String.does(vertex: CFGVertex) {
        vertices[getLabel(this)] = vertex
    }
}

class CFGBuilder {
    private val programCFG = mutableMapOf<Definition.FunctionDefinition, CFGFragment>()
    private val registers: MutableMap<String, Register> = mutableMapOf()

    fun fragment(function: Definition.FunctionDefinition, init: CFGFragmentBuilder.() -> Unit) {
        val builder = CFGFragmentBuilder(registers)
        builder.init()
        programCFG[function] = builder.build()
    }

    fun build(): ProgramCFG = programCFG
}

fun cfg(init: CFGBuilder.() -> Unit): ProgramCFG {
    val builder = CFGBuilder()
    builder.init()
    return builder.build()
}
