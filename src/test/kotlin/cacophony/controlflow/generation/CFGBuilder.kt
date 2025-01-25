package cacophony.controlflow.generation

import cacophony.controlflow.*
import cacophony.semantic.syntaxtree.LambdaExpression

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

    // These functions are only used in tests, as I value my sanity I won't care about references there.
    fun virtualRegister(name: String): Register = registers.getOrPut(name) { Register.VirtualRegister() }

    fun writeRegister(name: String, node: CFGNode) = cacophony.controlflow.writeRegister(virtualRegister(name), node)

    fun writeRegister(register: Register, name: String) =
        cacophony.controlflow.writeRegister(register, registerUse(virtualRegister(name), false))

    fun pushRegister(name: String) = pushRegister(virtualRegister(name), false)

    fun registerUse(name: String) = cacophony.controlflow.registerUse(virtualRegister(name), false)

    fun dataLabel(name: String) = cacophony.controlflow.dataLabel(name)

    infix fun String.does(vertex: CFGVertex) {
        vertices[getLabel(this)] = vertex
    }
}

class CFGBuilder {
    private val programCFG = mutableMapOf<LambdaExpression, CFGFragment>()
    private val registers: MutableMap<String, Register> = mutableMapOf()

    fun fragment(function: LambdaExpression, init: CFGFragmentBuilder.() -> Unit) {
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
