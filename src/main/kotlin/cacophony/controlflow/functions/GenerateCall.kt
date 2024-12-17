package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.generation.Layout
import cacophony.controlflow.generation.SimpleLayout
import cacophony.semantic.syntaxtree.Definition

/**
 * Wrapper for generateCall that additionally fills staticLink to parent function.
 */
fun generateCallFrom(
    callerFunction: FunctionHandler,
    function: Definition.FunctionDeclaration,
    functionHandler: FunctionHandler?,
    arguments: List<Layout>,
    result: Register?,
): List<CFGNode> =
    when (function) {
        is Definition.ForeignFunctionDeclaration -> {
            if (function.type!!.argumentsType.size != arguments.size) {
                throw IllegalArgumentException("Wrong argument count")
            }
            generateCall(function, arguments, result, callerFunction.getStackSpace())
        }

        is Definition.FunctionDefinition -> {
            if (function.arguments.size != arguments.size) {
                throw IllegalArgumentException("Wrong argument count")
            }
            val staticLinkVar = functionHandler!!.generateStaticLinkVariable(callerFunction)
            generateCall(function, arguments + listOf(SimpleLayout(staticLinkVar)), result, callerFunction.getStackSpace())
        }
    }

fun generateCall(
    function: Definition.FunctionDeclaration,
    arguments: List<Layout>,
    result: Register?,
    callerFunctionStackSize: CFGNode.Constant,
): List<CFGNode> {
    // TODO: intentional shadowing, remove after the struct flattening is done
    val arguments =
        arguments.map {
            require(it is SimpleLayout)
            it.access
        }

    val registerArguments = arguments.zip(REGISTER_ARGUMENT_ORDER)
    val stackArguments = arguments.drop(registerArguments.size).map { Pair(it, Register.VirtualRegister()) }

    val nodes: MutableList<CFGNode> = mutableListOf()

    // At the moment of calling the `function`, the RSP is divisible by 16,
    // The return value make it congruent to 8, and then oldRSP push makes it divisible by 16 again. (look at prologue generation)
    //
    // Then `callerFunctionStackSize.value` bytes on the stack are allocated by the `function`.
    // Finally, here we are going to increase the stack size to store all the stack arguments
    // Therefore we have to shift the stack by (callerFunctionStackSize.value + 8 * stackArguments.size) % 16 manually

    val alignmentShift = CFGNode.ConstantLazy { (callerFunctionStackSize.value + 8 * stackArguments.size) % 16 }

    val rsp = CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP))
    nodes.add(CFGNode.SubtractionAssignment(rsp, alignmentShift))

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
    nodes.add(CFGNode.AdditionAssignment(rsp, CFGNode.ConstantLazy { alignmentShift.value + 8 * stackArguments.size }))

    if (result != null)
        nodes.add(CFGNode.Assignment(CFGNode.RegisterUse(result), CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RAX))))

    return nodes
}

interface CallGenerator {
    fun generateCallFrom(
        callerFunction: FunctionHandler,
        function: Definition.FunctionDeclaration,
        functionHandler: FunctionHandler?,
        arguments: List<Layout>,
        result: Register?,
    ): List<CFGNode>
}

class SimpleCallGenerator : CallGenerator {
    override fun generateCallFrom(
        callerFunction: FunctionHandler,
        function: Definition.FunctionDeclaration,
        functionHandler: FunctionHandler?,
        arguments: List<Layout>,
        result: Register?,
    ): List<CFGNode> =
        cacophony.controlflow.functions.generateCallFrom(
            callerFunction,
            function,
            functionHandler,
            arguments,
            result,
        )
}
