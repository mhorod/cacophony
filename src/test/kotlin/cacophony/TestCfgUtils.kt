package cacophony

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.CFGVertex
import cacophony.controlflow.Register
import cacophony.controlflow.X64Register
import cacophony.controlflow.generation.ProgramCFG
import cacophony.semantic.syntaxtree.Definition

val rax = Register.FixedRegister(X64Register.RAX)
val unit = CFGNode.UNIT
val trueValue = CFGNode.TRUE
val falseValue = CFGNode.FALSE
val returnNode = CFGNode.Return

fun integer(value: Int) = CFGNode.Constant(value)

fun registerUse(register: Register) = CFGNode.RegisterUse(register)

// arithmetic
infix fun CFGNode.add(other: CFGNode) = CFGNode.Addition(this, other)

infix fun CFGNode.sub(other: CFGNode) = CFGNode.Subtraction(this, other)

infix fun CFGNode.mul(other: CFGNode) = CFGNode.Multiplication(this, other)

infix fun CFGNode.div(other: CFGNode) = CFGNode.Division(this, other)

infix fun CFGNode.mod(other: CFGNode) = CFGNode.Modulo(this, other)

fun minus(node: CFGNode) = CFGNode.Minus(node)

// assignment
infix fun CFGNode.LValue.addeq(other: CFGNode) = CFGNode.AdditionAssignment(this, other)

infix fun CFGNode.LValue.subeq(other: CFGNode) = CFGNode.SubtractionAssignment(this, other)

infix fun CFGNode.LValue.muleq(other: CFGNode) = CFGNode.MultiplicationAssignment(this, other)

infix fun CFGNode.LValue.diveq(other: CFGNode) = CFGNode.DivisionAssignment(this, other)

infix fun CFGNode.LValue.modeq(other: CFGNode) = CFGNode.ModuloAssignment(this, other)

// logical
infix fun CFGNode.eq(other: CFGNode) = CFGNode.Equals(this, other)

infix fun CFGNode.neq(other: CFGNode) = CFGNode.NotEquals(this, other)

infix fun CFGNode.lt(other: CFGNode) = CFGNode.Less(this, other)

infix fun CFGNode.leq(other: CFGNode) = CFGNode.LessEqual(this, other)

infix fun CFGNode.gt(other: CFGNode) = CFGNode.Greater(this, other)

infix fun CFGNode.geq(other: CFGNode) = CFGNode.GreaterEqual(this, other)

fun not(node: CFGNode) = CFGNode.LogicalNot(node)

fun writeRegister(
    register: Register,
    value: CFGNode,
) = CFGNode.Assignment(registerUse(register), value)

class CFGFragmentBuilder {
    private val labels: MutableMap<String, CFGLabel> = mutableMapOf()
    val vertices = mutableMapOf<CFGLabel, CFGVertex>()

    private fun getLabel(label: String): CFGLabel {
        return labels.getOrPut(label) { CFGLabel() }
    }

    fun build(): CFGFragment {
        return CFGFragment(vertices, getLabel("entry"))
    }

    fun final(node: () -> CFGNode) = CFGVertex.Final(node())

    fun jump(
        destination: String,
        node: () -> CFGNode,
    ): CFGVertex {
        return CFGVertex.Jump(node(), getLabel(destination))
    }

    fun conditional(
        trueDestination: String,
        falseDestination: String,
        condition: () -> CFGNode,
    ): CFGVertex {
        return CFGVertex.Conditional(condition(), getLabel(trueDestination), getLabel(falseDestination))
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
        init: CFGFragmentBuilder.() -> Unit,
    ) {
        val builder = CFGFragmentBuilder()
        builder.init()
        programCFG[function] = builder.build()
    }

    fun build(): ProgramCFG {
        return programCFG
    }

    fun virtualRegister(name: String): Register {
        return registers.getOrPut(name) { Register.VirtualRegister() }
    }
}

fun cfg(init: CFGBuilder.() -> Unit): ProgramCFG {
    val builder = CFGBuilder()
    builder.init()
    return builder.build()
}
