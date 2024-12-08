package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.semantic.syntaxtree.Definition

/**
 * Wrapper for generateCall that additionally fills staticLink to parent function.
 */
fun generateCallFrom(
    callerFunction: FunctionHandler,
    function: Definition.FunctionDeclaration,
    functionHandler: FunctionHandler?,
    arguments: List<CFGNode>,
    result: Register?,
    respectStackAlignment: Boolean,
): List<CFGNode> =
    when (function) {
        is Definition.ForeignFunctionDeclaration -> {
            if (function.type!!.argumentsType.size != arguments.size) {
                throw IllegalArgumentException("Wrong argument count")
            }
            generateCall(function, arguments, result, true)
        }
        is Definition.FunctionDefinition -> {
            if (function.arguments.size != arguments.size) {
                throw IllegalArgumentException("Wrong argument count")
            }
            val staticLinkVar = functionHandler!!.generateStaticLinkVariable(callerFunction)
            generateCall(function, arguments + mutableListOf(staticLinkVar), result, respectStackAlignment)
        }
    }

fun generateCall(
    function: Definition.FunctionDeclaration,
    arguments: List<CFGNode>,
    result: Register?,
    respectStackAlignment: Boolean = false,
): List<CFGNode> {
    val registerArguments = arguments.zip(REGISTER_ARGUMENT_ORDER)
    val stackArguments = arguments.drop(registerArguments.size).map { Pair(it, Register.VirtualRegister()) }

    val nodes: MutableList<CFGNode> = mutableListOf()

    if (respectStackAlignment) {
        // we push two copies of RSP to the stack and either leave them both there,
        // or remove one of them via RSP assignment
        val oldRSP = Register.VirtualRegister()
        nodes.add(CFGNode.Assignment(CFGNode.RegisterUse(oldRSP), CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP))))

        nodes.add(CFGNode.Push(CFGNode.RegisterUse(oldRSP)))
        nodes.add(CFGNode.Push(CFGNode.RegisterUse(oldRSP)))

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

    // in what order should we evaluate arguments? gcc uses reversed order
    for ((argument, register) in registerArguments) {
        nodes.add(CFGNode.Assignment(CFGNode.RegisterUse(Register.FixedRegister(register)), argument))
    }
    for ((argument, register) in stackArguments) {
        nodes.add(CFGNode.Assignment(CFGNode.RegisterUse(register), argument))
    }

    // is this indirection necessary?
    for ((_, register) in stackArguments.reversed()) {
        nodes.add(CFGNode.Push(CFGNode.RegisterUse(register)))
    }

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

    return nodes
}
