package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.generation.Layout
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Type

/**
 * Wrapper for generateCall that additionally fills staticLink to parent function.
 */
fun generateCallFrom(
    callerFunction: FunctionHandler,
    function: Definition.FunctionDeclaration,
    functionHandler: FunctionHandler?,
    arguments: List<CFGNode>,
    result: Layout?,
): List<CFGNode> =
    when (function) {
        is Definition.ForeignFunctionDeclaration -> {
            if (function.type!!.argumentsType.size != arguments.size) {
                throw IllegalArgumentException("Wrong argument count")
            }
            if (result != null && !layoutMatchesType(result, function.type.returnType)) {
                throw IllegalArgumentException("Wrong result layout")
            }
            generateCall(function, function.type.argumentsType, arguments, result, callerFunction.getStackSpace())
        }

        is Definition.FunctionDefinition -> {
            if (function.arguments.sumOf { it.type.size() } != arguments.size) {
                throw IllegalArgumentException("Wrong argument count")
            }
            if (result != null && !layoutMatchesType(result, function.returnType)) {
                throw IllegalArgumentException("Wrong result layout")
            }
            val staticLinkVar = functionHandler!!.generateStaticLinkVariable(callerFunction)
            generateCall(
                function,
                functionHandler.getFunctionDeclaration().arguments.map { it.type },
                arguments + listOf(staticLinkVar),
                result,
                callerFunction.getStackSpace(),
            )
        }
    }

private fun layoutMatchesType(layout: Layout, type: Type): Boolean = layout.matchesType(type)

fun generateCall(
    function: Definition.FunctionDeclaration,
    argTypes: List<Type>,
    arguments: List<CFGNode>,
    result: Layout?,
    callerFunctionStackSize: CFGNode.Constant,
): List<CFGNode> {
    // One additional value for static link.
    val isReference = argTypes.map { it is BaseType.Referential } + listOf(false)

    val registerArguments = arguments.zip(isReference).zip(REGISTER_ARGUMENT_ORDER)
    val stackArguments = arguments.zip(isReference).drop(registerArguments.size).map { Pair(it.first, Register.VirtualRegister(it.second)) }
    val resultSize = function.returnType.size()

    val stackResultsSize = (resultSize - REGISTER_RETURN_ORDER.size).let { if (it > 0) it else 0 }

    val nodes: MutableList<CFGNode> = mutableListOf()

    // At the moment of calling the `function`, the RSP is divisible by 16,
    // The return value make it congruent to 8, and then oldRSP push makes it divisible by 16 again. (look at prologue generation)
    //
    // Then `callerFunctionStackSize.value` bytes on the stack are allocated by the `function`.
    // Finally, here we are going to increase the stack size to store all the stack arguments
    // Therefore we have to shift the stack by (callerFunctionStackSize.value + 8 * stackArguments.size + 8 * stackResults.size) % 16 manually

    val stackShift = 8 * stackArguments.size + 8 * stackResultsSize

    val alignmentShift = CFGNode.ConstantLazy { (callerFunctionStackSize.value + stackShift) % 16 }

    val rsp = CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP), false)
    nodes.add(CFGNode.SubtractionAssignment(rsp, alignmentShift))

    if (stackResultsSize > 0) nodes.add(CFGNode.SubtractionAssignment(rsp, CFGNode.ConstantKnown(8 * stackResultsSize)))

    // in what order should we evaluate arguments? gcc uses reversed order
    for ((argument, register) in registerArguments) {
        nodes.add(CFGNode.Assignment(CFGNode.RegisterUse(Register.FixedRegister(register), argument.second), argument.first))
    }
    for ((argument, register) in stackArguments) {
        nodes.add(CFGNode.Assignment(CFGNode.RegisterUse(register, register.holdsReference), argument))
    }

    // is this indirection necessary?
    for ((_, register) in stackArguments.reversed()) {
        nodes.add(CFGNode.Push(CFGNode.RegisterUse(register, register.holdsReference)))
    }

    nodes.add(CFGNode.Call(function))

    if (result == null) {
        nodes.add(
            CFGNode.AdditionAssignment(
                rsp,
                CFGNode.ConstantLazy {
                    alignmentShift.value + 8 * stackArguments.size +
                        8 * stackResultsSize
                },
            ),
        )
    } else {
        if (stackResultsSize == 0) {
            nodes.add(CFGNode.AdditionAssignment(rsp, CFGNode.ConstantLazy { alignmentShift.value + 8 * stackArguments.size }))
        } else {
            nodes.add(CFGNode.AdditionAssignment(rsp, CFGNode.ConstantKnown(8 * stackArguments.size)))
        }

        val results = result.flatten()
        val registerResults = results.zip(REGISTER_RETURN_ORDER)
        val stackResults = results.drop(registerResults.size)

        for ((accessInfo, register) in registerResults) {
            nodes.add(
                CFGNode.Assignment(
                    accessInfo.access as CFGNode.LValue,
                    CFGNode.RegisterUse(Register.FixedRegister(register), accessInfo.holdsReference),
                ),
            )
        }

        if (stackResults.isNotEmpty()) {
            for (accessInfo in stackResults.reversed()) {
                val tmpReg = Register.VirtualRegister(accessInfo.holdsReference)
                nodes.add(CFGNode.Pop(registerUse(tmpReg, tmpReg.holdsReference)))
                nodes.add(CFGNode.Assignment(accessInfo.access as CFGNode.LValue, registerUse(tmpReg, tmpReg.holdsReference)))
            }
            nodes.add(CFGNode.AdditionAssignment(rsp, CFGNode.ConstantLazy { alignmentShift.value }))
        }
    }

    return nodes
}

interface CallGenerator {
    fun generateCallFrom(
        callerFunction: FunctionHandler,
        function: Definition.FunctionDeclaration,
        functionHandler: FunctionHandler?,
        arguments: List<CFGNode>,
        result: Layout?,
    ): List<CFGNode>
}

class SimpleCallGenerator : CallGenerator {
    override fun generateCallFrom(
        callerFunction: FunctionHandler,
        function: Definition.FunctionDeclaration,
        functionHandler: FunctionHandler?,
        arguments: List<CFGNode>,
        result: Layout?,
    ): List<CFGNode> = cacophony.controlflow.functions.generateCallFrom(callerFunction, function, functionHandler, arguments, result)
}
