package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

val rax = Register.FixedRegister(HardwareRegister.RAX)
val rbx = Register.FixedRegister(HardwareRegister.RBX)
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

fun returnNode(resultSize: Int) = CFGNode.Return(CFGNode.ConstantKnown(resultSize))

fun integer(value: Int) = CFGNode.ConstantKnown(value)

fun writeRegister(register: Register, value: CFGNode, holdsReference: Boolean = false) =
    CFGNode.Assignment(registerUse(register, holdsReference), value)

fun registerUse(register: Register, holdsReference: Boolean = false) = CFGNode.RegisterUse(register, holdsReference)

fun memoryAccess(at: CFGNode, holdsReference: Boolean = false) = CFGNode.MemoryAccess(at, holdsReference)

fun dataLabel(label: String) = CFGNode.DataLabel(label)

fun pushLabel(label: String) = CFGNode.Push(dataLabel(label))

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
fun pushRegister(register: Register, holdsReference: Boolean) = CFGNode.Push(CFGNode.RegisterUse(register, holdsReference))

fun popRegister(register: Register, holdsReference: Boolean) = CFGNode.Pop(CFGNode.RegisterUse(register, holdsReference))

fun call(function: Definition.FunctionDeclaration) = CFGNode.Call(function)

// logical
infix fun CFGNode.eq(other: CFGNode) = CFGNode.Equals(this, other)

infix fun CFGNode.neq(other: CFGNode) = CFGNode.NotEquals(this, other)

infix fun CFGNode.lt(other: CFGNode) = CFGNode.Less(this, other)

infix fun CFGNode.leq(other: CFGNode) = CFGNode.LessEqual(this, other)

infix fun CFGNode.gt(other: CFGNode) = CFGNode.Greater(this, other)

infix fun CFGNode.geq(other: CFGNode) = CFGNode.GreaterEqual(this, other)

fun not(node: CFGNode) = CFGNode.LogicalNot(node)

fun wrapAllocation(allocation: VariableAllocation, holdsReference: Boolean): CFGNode.LValue =
    when (allocation) {
        is VariableAllocation.InRegister -> registerUse(allocation.register, holdsReference)
        is VariableAllocation.OnStack -> memoryAccess(registerUse(rbp, false) sub integer(allocation.offset), holdsReference)
        is VariableAllocation.ViaPointer -> TODO()
    }
