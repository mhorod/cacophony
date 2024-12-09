package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.semantic.syntaxtree.Definition

fun generateCall(
    function: Definition.FunctionDefinition,
    arguments: List<CFGNode>,
    result: Register?,
    respectStackAlignment: Boolean = false,
): List<CFGNode> {
    if (function.arguments.size + 1 != arguments.size) {
        throw IllegalArgumentException("Wrong argument count")
    }
    val registerArguments = arguments.zip(REGISTER_ARGUMENT_ORDER)
    val stackArguments = arguments.drop(registerArguments.size).map { Pair(it, Register.VirtualRegister()) }

    val nodes: MutableList<CFGNode> = mutableListOf()
    nodes.add(CFGNode.Comment("call starts"))

    if (respectStackAlignment) {
        // we push two copies of RSP to the stack and either leave them both there,
        // or remove one of them via RSP assignment
        val oldRSP = Register.VirtualRegister()
        nodes.add(CFGNode.Assignment(CFGNode.RegisterUse(oldRSP), CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP))))

        nodes.add(CFGNode.Comment("pre pushes"))
        nodes.add(CFGNode.Push(CFGNode.RegisterUse(oldRSP)))
        nodes.add(CFGNode.Push(CFGNode.RegisterUse(oldRSP)))
        nodes.add(CFGNode.Comment("pre add"))

        // in an ideal world we would do something like "and rsp, ~15" or similar; for now this will do
        // at the very least split the computation of (RSP + stackArguments.size % 2 * 8) % 16
        // into two cases depending on the parity of stackArguments.size
        nodes.add(
            CFGNode.Assignment(
                CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP)),
                CFGNode.Addition(
                    CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP)),
                    CFGNode.Modulo(
                        CFGNode.Addition(
                            CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP)),
                            CFGNode.ConstantKnown(stackArguments.size % 2 * REGISTER_SIZE),
                        ),
                        CFGNode.ConstantKnown(16),
                    ),
                ),
            ),
        )
    }
    nodes.add(CFGNode.Comment("stack aligned"))

    // in what order should we evaluate arguments? gcc uses reversed order
    for ((argument, register) in registerArguments) {
        nodes.add(CFGNode.Comment("reg arg #"))
        nodes.add(CFGNode.Assignment(CFGNode.RegisterUse(Register.FixedRegister(register)), argument))
        nodes.add(CFGNode.Comment("reg arg $"))
    }
    nodes.add(CFGNode.Comment("register arguments filled"))
    for ((argument, register) in stackArguments) {
        nodes.add(CFGNode.Assignment(CFGNode.RegisterUse(register), argument))
    }
    nodes.add(CFGNode.Comment("stack arguments computed"))

    // is this indirection necessary?
    for ((_, register) in stackArguments.reversed()) {
        nodes.add(CFGNode.Push(CFGNode.RegisterUse(register)))
    }
    nodes.add(CFGNode.Comment("stack arguments filled"))

    nodes.add(CFGNode.Call(function))

    if (stackArguments.isNotEmpty()) {
        nodes.add(
            CFGNode.Assignment(
                CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP)),
                CFGNode.Addition(
                    CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP)),
                    CFGNode.ConstantKnown(REGISTER_SIZE * stackArguments.size),
                ),
            ),
        )
    }

    if (respectStackAlignment) {
        // we could remove the operations from previous `if (stackArguments.isNotEmpty())` block
        // via MemoryAccess, but for now the semantics are a bit unclear + it would introduce
        // a few ifs, which we do not need at this point
        nodes.add(CFGNode.Pop(CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP))))
    }

    if (result != null) {
        nodes.add(CFGNode.Assignment(CFGNode.RegisterUse(result), CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RAX))))
    }
    nodes.add(CFGNode.Comment("call ends"))

    return nodes
}
