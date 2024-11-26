package cacophony.controlflow

import cacophony.controlflow.generation.ProgramCFG
import cacophony.semantic.syntaxtree.Definition

val rax = Register.FixedRegister(HardwareRegister.RAX)
val rsp = Register.FixedRegister(HardwareRegister.RSP)
val rbp = Register.FixedRegister(HardwareRegister.RBP)
val rdi = Register.FixedRegister(HardwareRegister.RDI)
val rsi = Register.FixedRegister(HardwareRegister.RSI)
val rdx = Register.FixedRegister(HardwareRegister.RDX)
val rcx = Register.FixedRegister(HardwareRegister.RCX)
val r8 = Register.FixedRegister(HardwareRegister.R8)
val r9 = Register.FixedRegister(HardwareRegister.R9)
val r10 = Register.FixedRegister(HardwareRegister.R10)
val r11 = Register.FixedRegister(HardwareRegister.R11)
val r12 = Register.FixedRegister(HardwareRegister.R12)
val r13 = Register.FixedRegister(HardwareRegister.R13)
val r14 = Register.FixedRegister(HardwareRegister.R14)
val r15 = Register.FixedRegister(HardwareRegister.R15)

val unit = CFGNode.UNIT
val trueValue = CFGNode.TRUE
val falseValue = CFGNode.FALSE
val returnNode = CFGNode.Return

fun integer(value: Int) = CFGNode.Constant(value)

fun registerUse(register: Register) = CFGNode.RegisterUse(register)

fun memoryAccess(at: CFGNode) = CFGNode.MemoryAccess(at)

// arithmetic
infix fun CFGNode.add(other: CFGNode) = CFGNode.Addition(this, other)

infix fun CFGNode.sub(other: CFGNode) = CFGNode.Subtraction(this, other)

infix fun CFGNode.mul(other: CFGNode) = CFGNode.Multiplication(this, other)

infix fun CFGNode.div(other: CFGNode) = CFGNode.Division(this, other)

infix fun CFGNode.mod(other: CFGNode) = CFGNode.Modulo(this, other)

fun minus(node: CFGNode) = CFGNode.Minus(node)

// assignment
infix fun CFGNode.LValue.assign(other: CFGNode) = CFGNode.Assignment(this, other)

infix fun CFGNode.LValue.addeq(other: CFGNode) = CFGNode.AdditionAssignment(this, other)

infix fun CFGNode.LValue.subeq(other: CFGNode) = CFGNode.SubtractionAssignment(this, other)

infix fun CFGNode.LValue.muleq(other: CFGNode) = CFGNode.MultiplicationAssignment(this, other)

infix fun CFGNode.LValue.diveq(other: CFGNode) = CFGNode.DivisionAssignment(this, other)

infix fun CFGNode.LValue.modeq(other: CFGNode) = CFGNode.ModuloAssignment(this, other)

// stack
fun pushRegister(register: Register) = CFGNode.Push(CFGNode.RegisterUse(register))

fun popRegister(register: Register) = CFGNode.Pop(CFGNode.RegisterUse(register))

fun call(function: Definition.FunctionDeclaration) = CFGNode.Call(function)

// logical
infix fun CFGNode.eq(other: CFGNode) = CFGNode.Equals(this, other)

infix fun CFGNode.neq(other: CFGNode) = CFGNode.NotEquals(this, other)

infix fun CFGNode.lt(other: CFGNode) = CFGNode.Less(this, other)

infix fun CFGNode.leq(other: CFGNode) = CFGNode.LessEqual(this, other)

infix fun CFGNode.gt(other: CFGNode) = CFGNode.Greater(this, other)

infix fun CFGNode.geq(other: CFGNode) = CFGNode.GreaterEqual(this, other)

fun not(node: CFGNode) = CFGNode.LogicalNot(node)

fun writeRegister(register: Register, value: CFGNode) = CFGNode.Assignment(registerUse(register), value)

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
