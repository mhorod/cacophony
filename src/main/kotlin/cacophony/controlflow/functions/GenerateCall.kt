package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.generation.FunctionLayout
import cacophony.controlflow.generation.Layout
import cacophony.controlflow.generation.generateSubLayout
import cacophony.diagnostics.DiagnosticMessage
import cacophony.diagnostics.Diagnostics
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Type
import cacophony.semantic.types.TypeTranslator
import cacophony.utils.Location

/**
 * Wrapper for generateCall that additionally fills staticLink to parent function.
 */
fun generateCallFrom(
    callerFunction: CallableHandler,
    functionLayout: FunctionLayout,
    arguments: List<Layout>,
    result: Layout?,
): List<CFGNode> {
    TODO("Justyna")
    // detect whether this is a foreign function
//    if (functionLayout.link == SimpleLayout(integer(0))) {
//        generateCall(function, arguments, result, callerFunction.getStackSpace())
//    }
}
// =
//    when (function) {
//        is Definition.ForeignFunctionDeclaration -> {
//            if (function.type!!.argumentsType.size != arguments.size) {
//                throw IllegalArgumentException("Wrong argument count")
//            }
//            if (result != null && !layoutMatchesType(result, function.type.returnType)) {
//                throw IllegalArgumentException("Wrong result layout")
//            }
//            generateCall(function, arguments, result, callerFunction.getStackSpace())
//        }
//
//        is Definition.FunctionDefinition -> {
//            if (function.arguments.size != arguments.size) {
//                throw IllegalArgumentException("Wrong argument count")
//            }
//            if (result != null && !layoutMatchesType(result, function.returnType)) {
//                throw IllegalArgumentException("Wrong result layout")
//            }
//            val staticLinkVar = functionHandler!!.generateStaticLinkVariable(callerFunction)
//            generateCall(function, arguments + listOf(SimpleLayout(staticLinkVar)), result, callerFunction.getStackSpace())
//        }
//    }

private fun layoutMatchesType(layout: Layout, type: Type): Boolean = layout.matchesType(type)

fun generateCall(
    function: Definition.FunctionDeclaration,
    argumentLayouts: List<Layout>,
    result: Layout?,
    callerFunctionStackSize: CFGNode.Constant,
): List<CFGNode> {
    val translator =
        TypeTranslator(
            object : Diagnostics {
                override fun report(message: DiagnosticMessage, range: Pair<Location, Location>): Unit =
                    throw IllegalArgumentException("Invalid type $message")

                override fun fatal(): Throwable = throw IllegalArgumentException("Invalid type")

                override fun getErrors(): List<String> = listOf()
            },
        )
    val argumentTypes =
        when (function) {
            is Definition.ForeignFunctionDeclaration -> function.type!!.argumentsType
            is Definition.FunctionDefinition -> function.arguments.map { it.type }
        }

    var arguments =
        argumentLayouts.zip(argumentTypes).flatMap { (layout, type) ->
            generateSubLayout(layout, translator.translateType(type)!!).flatten()
        }

    // Handle static link
    if (argumentTypes.size + 1 == argumentLayouts.size) {
        arguments = arguments + argumentLayouts.last().flatten()
    }

    val registerArguments = arguments.zip(REGISTER_ARGUMENT_ORDER)
    val stackArguments = arguments.drop(registerArguments.size).map { Pair(it, Register.VirtualRegister(it.holdsReference)) }

    val resultSize = function.returnType.size()

    val stackResultsSize = (resultSize - REGISTER_RETURN_ORDER.size).let { if (it > 0) it else 0 }

    val nodes: MutableList<CFGNode> = mutableListOf()

    // At the moment of calling the `function`, the RSP is divisible by 16,
    // The return address makes it congruent to 8,
    // and then oldRBP, 8 bytes of padding and stack frame layout are pushed
    // to make it divisible by 16 again. (look at prologue generation)
    //
    // Then `callerFunctionStackSize.value` bytes on the stack are allocated by the `function`.
    // Finally, here we are going to increase the stack size to store all the stack arguments
    // Therefore we have to shift the stack by (callerFunctionStackSize.value + 8 * stackArguments.size + 8 * stackResults.size) % 16 manually

    val stackShift = 8 * stackArguments.size + 8 * stackResultsSize

    val alignmentShift = CFGNode.ConstantLazy { (callerFunctionStackSize.value + stackShift) % 16 }

    val rsp = CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RSP), false)
    nodes.add(rsp subeq alignmentShift)

    if (stackResultsSize > 0) nodes.add(rsp subeq integer(8 * stackResultsSize))

    for ((accessInfo, register) in stackArguments) {
        nodes.add(writeRegister(register, accessInfo.access, register.holdsReference))
    }

    // in what order should we evaluate arguments? gcc uses reversed order
    for ((accessInfo, register) in registerArguments) {
        nodes.add(writeRegister(Register.FixedRegister(register), accessInfo.access, accessInfo.holdsReference))
    }

    // is this indirection necessary?
    for ((_, register) in stackArguments.reversed()) {
        nodes.add(pushRegister(register, register.holdsReference))
    }

    nodes.add(call(function))

    if (result == null) {
        nodes.add(
            rsp addeq
                CFGNode.ConstantLazy {
                    alignmentShift.value + 8 * stackArguments.size +
                        8 * stackResultsSize
                },
        )
    } else {
        if (stackResultsSize == 0) {
            nodes.add(rsp addeq CFGNode.ConstantLazy { alignmentShift.value + 8 * stackArguments.size })
        } else {
            nodes.add(rsp addeq CFGNode.ConstantKnown(8 * stackArguments.size))
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
            for (accessInfo in stackResults) {
                val tmpReg = Register.VirtualRegister(accessInfo.holdsReference)
                nodes.add(popRegister(tmpReg, tmpReg.holdsReference))
                nodes.add(CFGNode.Assignment(accessInfo.access as CFGNode.LValue, registerUse(tmpReg, tmpReg.holdsReference)))
            }
            nodes.add(rsp addeq CFGNode.ConstantLazy { alignmentShift.value })
        }
    }

    return nodes
}

interface CallGenerator {
    fun generateCallFrom(callerHandler: CallableHandler, callee: FunctionLayout, arguments: List<Layout>, result: Layout?): List<CFGNode>
}

class SimpleCallGenerator : CallGenerator {
    override fun generateCallFrom(
        callerHandler: CallableHandler,
        callee: FunctionLayout,
        arguments: List<Layout>,
        result: Layout?,
    ): List<CFGNode> = cacophony.controlflow.functions.generateCallFrom(callerHandler, callee, arguments, result)
}
